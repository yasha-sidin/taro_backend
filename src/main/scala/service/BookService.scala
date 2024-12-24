package ru.otus
package service

import repository.{AppointmentDateRepository, BookingRepository, ConstantRepository, Repository}
import error.{ConfirmationNotAvailableFailure, DBFailure, ExpectedFailure, NotAvailableFailure, NotFoundFailure}
import dao.{AppointmentDate, Booking, Constant}
import `type`.{AppConstant, AppointmentDateStatus, BookingStatus}
import service.BookingService.BookingCreate

import zio.{ULayer, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

object BookService {
  private type BookService    = Service
  private type BookServiceEnv =
    Repository.Env
      with BookingService.Service
      with BookingRepository.Service
      with AppointmentDateRepository.Service
      with AppointmentDateService.Service
      with ConstantRepository.Service
      with ConstantService.Service

  trait Service {
    def bookDate(dateId: UUID, telegramUserId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def cancelBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def confirmBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def completeBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
  }

  class ServiceImpl extends Service {
    protected val dc: db.Ctx.type = db.Ctx

    private def canBook(date: AppointmentDate, now: Instant): Boolean =
      date.isAvailable && !date.isExpired(now)

    override def bookDate(dateId: UUID, telegramUserId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        now                  <- ZIO.succeed(Instant.now())
        date                 <- AppointmentDateService.getAppointmentDateById(dateId)
        constantValue        <- ConstantService.getConstantValueByKey[Long](AppConstant.MaxTimeToConfirm)
        timeSecondsToConfirm <- if (now.plusSeconds(constantValue).isAfter(date.bookingDeadline)) {
                                  ZIO.succeed(date.dateFrom)
                                }
                                else {
                                  ZIO.succeed(now.plusSeconds(constantValue))
                                }
        tuple                <- if (canBook(date, now)) {
                                  dc.transaction(
                                    for {
                                      updatedDate <- AppointmentDateService.updateAppointmentDate(date.copy(status = AppointmentDateStatus.Booked))
                                      booking     <- BookingService.createBooking(
                                                       BookingCreate(
                                                         BookingStatus.Active,
                                                         dateId,
                                                         telegramUserId,
                                                         canReturn = true,
                                                         timeToConfirm = timeSecondsToConfirm,
                                                       )
                                                     )
                                    } yield (updatedDate, booking)
                                  ).mapError(er => DBFailure(er))
                                }
                                else {
                                  ZIO.logWarning(s"Can't book date with id $dateId for user with id $telegramUserId") *>
                                    ZIO.fail(NotAvailableFailure("Date is not available now."))
                                }
      } yield tuple

    override def cancelBooking(bookingId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        now     <- ZIO.succeed(Instant.now())
        booking <- BookingService.getBookingById(bookingId)
        date    <- AppointmentDateService.getAppointmentDateById(booking.dateId)
        tuple   <- if (!date.isExpired(now)) {
                     dc.transaction(
                       for {
                         updatedDate    <- AppointmentDateService.updateAppointmentDate(date.copy(status = AppointmentDateStatus.Available))
                         updatedBooking <- BookingService.updateBooking(booking.copy(status = BookingStatus.Cancelled))
                       } yield (updatedDate, updatedBooking)
                     ).mapError(er => DBFailure(er))
                   }
                   else {
                     ZIO.logWarning(s"$date is expired") *>
                       ZIO.fail(NotAvailableFailure("Date is not available now."))
                   }
      } yield tuple

    override def confirmBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        now <- ZIO.succeed(Instant.now())
        booking <- BookingService.getBookingById(bookId)
        res <- if (booking.isActive && booking.canConfirm(now)) {
          for {
            updated <- BookingService.updateBooking(booking.copy(status = BookingStatus.Confirmed))
            date <- AppointmentDateService.getAppointmentDateById(booking.dateId)
          } yield (date, updated)
        } else {
          ZIO.fail(ConfirmationNotAvailableFailure("Booking is not active or time to confirmation is expired"))
        }
      } yield res

    override def completeBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        booking <- BookingService.getBookingById(bookId)
        updated <- BookingService.updateBooking(booking.copy(status = BookingStatus.Completed))
        date <- AppointmentDateService.getAppointmentDateById(booking.dateId)
      } yield (date, updated)
  }

  val live: ULayer[BookService] = ZLayer.succeed(new ServiceImpl)
}
