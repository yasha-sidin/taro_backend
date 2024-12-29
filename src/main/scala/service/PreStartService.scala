package ru.otus
package service

import db.LiquibaseService
import repository.{AppointmentDateRepository, BookingRepository, ConstantRepository, TelegramUserRepository}
import error.{ExpectedFailure, MigrationFailure}

import zio.{ULayer, ZIO, ZLayer}
import zio.macros.accessible

@accessible
object PreStartService {
  private type PreStartService    = Service
  private type PreStartServiceEnv =
    db.DataSource
      with ConstantRepository.Service
      with ConstantService.Service
      with LiquibaseService.Service
      with LiquibaseService.Liqui
      with AppointmentDateRepository.Service
      with AppointmentDateService.Service
      with ScheduleService.Service
      with BookingRepository.Service
      with BookingService.Service
      with BookService.Service
      with TelegramUserRepository.Service
      with TelegramUserService.Service

  trait Service {
    def preStart: ZIO[PreStartServiceEnv, ExpectedFailure, Unit]
  }

  class ServiceImpl extends Service {
    override def preStart: ZIO[PreStartServiceEnv, ExpectedFailure, Unit] =
      for {
        _ <- LiquibaseService.performMigration.mapError(er => MigrationFailure(er.getMessage))
        _ <- ConstantService.initConstants()
        _ <- ScheduleService.updateExpiredDates().forkDaemon
        _ <- ScheduleService.cancelNotConfirmedBookings().forkDaemon
        _ <- ScheduleService.markBookingsAsCompleted().forkDaemon
      } yield ()
  }

  val live: ULayer[PreStartService] = ZLayer.succeed(new ServiceImpl)
}
