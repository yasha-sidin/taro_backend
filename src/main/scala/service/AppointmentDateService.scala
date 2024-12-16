package ru.otus
package service

import `type`.AppointmentDateStatus
import error.{ExpectedFailure, NotFoundFailure}
import repository.{AppointmentDateRepository, Repository}

import ru.otus.dao.AppointmentDate
import zio.{Random, ULayer, ZIO, ZLayer}
import zio.macros.accessible

import java.time.Instant
import java.util.UUID

@accessible
object AppointmentDateService {

  private type AppointmentDateService = Service

  private type AppointmentDateServiceEnv = Repository.Env with AppointmentDateRepository.Service

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
  }

  class ServiceImpl extends Service {
    override def createAppointmentDate(appointmentDateCreate: AppointmentDateCreate): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate] =
      for {
        uuid      <- Random.nextUUID
        createdAt <- ZIO.succeed(Instant.now())
        date      <- AppointmentDateRepository.insertData(
                       AppointmentDate(
                         uuid,
                         appointmentDateCreate.dateFrom,
                         appointmentDateCreate.dateTo,
                         appointmentDateCreate.status,
                         appointmentDateCreate.bookingDeadline,
                         createdAt,
                         createdAt,
                       )
                     )
      } yield date

    override def updateAppointmentDate(appointmentDate: AppointmentDate): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate] =
      for {
        now         <- ZIO.succeed(Instant.now())
        updatedDate <- ZIO.succeed(appointmentDate.copy(updatedAt = now))
        updated     <- AppointmentDateRepository.updateData(updatedDate)
      } yield updated

    override def getAppointmentDateById(id: UUID): ZIO[AppointmentDateServiceEnv, ExpectedFailure, AppointmentDate] =
      AppointmentDateRepository.getDataById(id).some.orElseFail(NotFoundFailure(s"Date with id $id has not been found."))
  }

  val live: ULayer[AppointmentDateService] = ZLayer.succeed(new ServiceImpl)
}
