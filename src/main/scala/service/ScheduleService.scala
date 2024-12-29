package ru.otus
package service

import repository.{AppointmentDateRepository, BookingRepository, ConstantRepository, TelegramUserRepository}
import error.ExpectedFailure

import zio.{Schedule, ULayer, ZIO, ZLayer, durationInt}
import zio.macros.accessible
import zio.stream.ZStream

import scala.language.postfixOps

@accessible
object ScheduleService {
  private type ScheduleService    = Service
  private type ScheduleServiceEnv =
    db.DataSource
      with AppointmentDateRepository.Service
      with AppointmentDateService.Service
      with BookingRepository.Service
      with BookingService.Service
      with BookService.Service
      with ConstantRepository.Service
      with ConstantService.Service
      with TelegramUserRepository.Service
      with TelegramUserService.Service

  trait Service {
    def updateExpiredDates(): ZIO[ScheduleServiceEnv, ExpectedFailure, Unit]
    def cancelNotConfirmedBookings(): ZIO[ScheduleServiceEnv, ExpectedFailure, Unit]
    def markBookingsAsCompleted(): ZIO[ScheduleServiceEnv, ExpectedFailure, Unit]
  }

  class ServiceImpl extends Service {
    override def updateExpiredDates(): ZIO[ScheduleServiceEnv, ExpectedFailure, Unit] =
      ZStream
        .repeatZIO(
          ZIO.logInfo("Start expired dates checking") *>
          AppointmentDateService
            .updateExpiredDates()
            .tapError(er => ZIO.logError(er.message))
            .catchAllDefect(er => ZIO.logError(er.getMessage))
        )
        .schedule(Schedule.fixed(5 minutes))
        .runDrain

    override def cancelNotConfirmedBookings(): ZIO[ScheduleServiceEnv, ExpectedFailure, Unit] =
      ZStream
        .repeatZIO(
          ZIO.logInfo("Start checking bookings to cancel") *>
            BookService
              .cancelNotConfirmedBookings()
              .tapError(er => ZIO.logError(er.message))
              .catchAllDefect(er => ZIO.logError(er.getMessage))
        )
        .schedule(Schedule.fixed(3 minutes))
        .runDrain

    override def markBookingsAsCompleted(): ZIO[ScheduleServiceEnv, ExpectedFailure, Unit] =
      ZStream
        .repeatZIO(
          ZIO.logInfo("Start checking bookings to complete") *>
            BookService
              .markBookingsAsCompleted()
              .tapError(er => ZIO.logError(er.message))
              .catchAllDefect(er => ZIO.logError(er.getMessage))
        )
        .schedule(Schedule.fixed(6 minutes))
        .runDrain
  }

  val live: ULayer[ScheduleService] = ZLayer.succeed(new ServiceImpl)
}
