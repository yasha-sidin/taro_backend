package ru.otus
package service

import repository.{AppointmentDateRepository, BookingRepository}
import dao.AppointmentDate
import error.{AppointmentIsPastFailure, DateIsBookedAndConfirmedFailure, ExpectedFailure, InvalidDateRangeFailure, InvalidDeadlineFailure, PeriodIsAlreadyTakenFailure}
import dto.AppointmentDateAdd

import `type`.AppointmentDateStatus
import service.AppointmentDateService.AppointmentDateCreate
import zio.macros.accessible
import zio.{ULayer, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

@accessible
object DateManageService {
  private type DateManageService    = Service
  private type DateManageServiceEnv =
    db.DataSource with BookingRepository.Service with BookingService.Service with AppointmentDateRepository.Service with AppointmentDateService.Service

  trait Service {
    def addAppointment(date: AppointmentDateAdd): ZIO[DateManageServiceEnv, ExpectedFailure, AppointmentDate]
    def removeDate(id: UUID): ZIO[DateManageServiceEnv, ExpectedFailure, Unit]
  }

  class ServiceImpl extends Service {
    override def addAppointment(date: AppointmentDateAdd): ZIO[DateManageServiceEnv, ExpectedFailure, AppointmentDate] =
      for {
        now     <- ZIO.succeed(Instant.now())
        check   <- AppointmentDateService.checkPeriodExists(date.dateFrom, date.dateTo)
        _       <- ZIO
                     .fail(PeriodIsAlreadyTakenFailure("Period is already taken"))
                     .when(check)
        _       <- ZIO
                     .fail(InvalidDateRangeFailure("DateFrom must be before dateTo"))
                     .when(!date.dateFrom.isBefore(date.dateTo))
        _       <- ZIO
                     .fail(InvalidDeadlineFailure("BookingDeadline must be before dateFrom"))
                     .when(!date.bookingDeadline.isBefore(date.dateFrom))
        _       <- ZIO
                     .fail(AppointmentIsPastFailure("Current time must not be after bookingDeadline"))
                     .when(now.isAfter(date.bookingDeadline))
        created <- AppointmentDateService.createAppointmentDate(AppointmentDateCreate(AppointmentDateStatus.Available, date.dateFrom, date.dateTo, date.bookingDeadline))
      } yield created

    override def removeDate(id: UUID): ZIO[DateManageServiceEnv, ExpectedFailure, Unit] =
      for {
        bookings <- BookingService.getDateBookings(id)
        _        <- if (!bookings.exists(_.isConfirmed)) {
                      AppointmentDateService.removeAppointmentDate(id)
                    }
                    else {
                      ZIO.fail(DateIsBookedAndConfirmedFailure("Date is booked and confirmed. You can't delete it."))
                    }
      } yield ()
  }

  val live: ULayer[DateManageService] = ZLayer.succeed(new ServiceImpl)
}
