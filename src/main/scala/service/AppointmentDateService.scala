package ru.otus
package service

import `type`.AppointmentDateStatus
import error.{ ExpectedFailure, NotFoundFailure }
import repository.AppointmentDateRepository
import dao.AppointmentDate

import dto.AppointmentDateQueryParams
import zio.{ Random, ULayer, ZIO, ZLayer }
import zio.macros.accessible

import java.time.Instant
import java.util.UUID

@accessible
object AppointmentDateService {

  private type AppointmentDateService = Service

  private type AppointmentDateServiceEnv = db.DataSource with AppointmentDateRepository.Service

  case class AppointmentDateCreate(
      status: AppointmentDateStatus,
      dateFrom: Instant,
      dateTo: Instant,
      bookingDeadline: Instant,
    )

  trait Service {
    def createAppointmentDate(appointmentDateCreate: AppointmentDateCreate): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate]
    def updateAppointmentDate(appointmentDate: AppointmentDate): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate]
    def getAppointmentDateById(id: UUID): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate]
    def removeAppointmentDate(id: UUID): ZIO[AppointmentDateServiceEnv, ExpectedFailure, Unit]
    def checkPeriodExists(dateFrom: Instant, dateTo: Instant): ZIO[AppointmentDateServiceEnv, ExpectedFailure, Boolean]
    def getAvailableDates: ZIO[AppointmentDateServiceEnv, ExpectedFailure, List[AppointmentDate]]
    def updateExpiredDates(): ZIO[AppointmentDateServiceEnv, ExpectedFailure, Unit]
    def getFilteredDates(searchParams: AppointmentDateQueryParams): ZIO[AppointmentDateServiceEnv, ExpectedFailure, List[AppointmentDate]]
  }

  class ServiceImpl extends Service {
    override def createAppointmentDate(appointmentDateCreate: AppointmentDateCreate): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate] =
      for {
        uuid <- Random.nextUUID
        now  <- ZIO.succeed(Instant.now())
        date <- AppointmentDateRepository.insertData(
                  AppointmentDate(
                    uuid,
                    appointmentDateCreate.dateFrom,
                    appointmentDateCreate.dateTo,
                    appointmentDateCreate.status,
                    appointmentDateCreate.bookingDeadline,
                    now,
                    now,
                  )
                )
        _ <- ZIO.logInfo(date.toString)
      } yield date

    override def updateAppointmentDate(appointmentDate: AppointmentDate): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate] =
      for {
        now         <- ZIO.succeed(Instant.now())
        updatedDate <- ZIO.succeed(appointmentDate.copy(updatedAt = now))
        updated     <- AppointmentDateRepository.updateData(updatedDate)
      } yield updated

    override def getAppointmentDateById(id: UUID): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate] =
      AppointmentDateRepository.getDataById(id).some.orElseFail(NotFoundFailure(s"Date with id $id has not been found."))

    override def removeAppointmentDate(id: UUID): ZIO[AppointmentDateServiceEnv, ExpectedFailure, Unit] =
      AppointmentDateRepository.deletedById(id)

    override def checkPeriodExists(dateFrom: Instant, dateTo: Instant): ZIO[AppointmentDateServiceEnv, ExpectedFailure, Boolean] =
      AppointmentDateRepository.checkPeriodExists(dateFrom, dateTo)

    override def getAvailableDates: ZIO[AppointmentDateServiceEnv, ExpectedFailure, List[AppointmentDate]] =
      for {
        dates         <- AppointmentDateRepository.getDatesByStatus(AppointmentDateStatus.Available)
        now           <- ZIO.succeed(Instant.now())
        filteredDates <- ZIO.filterPar(dates)(date => ZIO.succeed(!date.isExpired(now)))
      } yield filteredDates

    override def updateExpiredDates(): ZIO[AppointmentDateServiceEnv, ExpectedFailure, Unit] =
      for {
        updatedCount <- AppointmentDateRepository.updateExpiredDates()
        _            <- ZIO.logInfo(s"Expired dates count: ${updatedCount}")
      } yield ()

    override def getFilteredDates(searchParams: AppointmentDateQueryParams): ZIO[AppointmentDateServiceEnv, ExpectedFailure, List[AppointmentDate]] =
      for {
        res <- searchParams.status match {
                 case Some(status) =>
                   status match {
                     case AppointmentDateStatus.Available => this.getAvailableDates
                     case status                          => AppointmentDateRepository.getDatesByStatus(status)
                   }
                 case None         => AppointmentDateRepository.getAllData
               }
      } yield res
  }

  val live: ULayer[AppointmentDateService] = ZLayer.succeed(new ServiceImpl)
}
