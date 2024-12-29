package ru.otus

import configuration.Configuration
import db.{LiquibaseService, zioDS}
import service.{AppointmentDateService, BookService, BookingService, ConstantService, DateManageService, PreStartService, ScheduleService, TelegramUserService}
import repository.{AppointmentDateRepository, BookingRepository, ConstantRepository, TelegramUserRepository}
import api.{AppointmentDateApi, BookingApi, ConstantApi}

import ru.otus.`type`.BookingStatus.Active
import ru.otus.dao.Booking
import zio.{Scope, ZIO, ZLayer}
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.{HandlerAspect, Middleware, Server}

import java.time.Instant
import java.util.UUID

object App {
  private val configProvider                          = ZLayer.succeed(TypesafeConfigProvider.fromResourcePath())
  private val server                                  = ZLayer.fromZIO {
    for {
      config <- ZIO.service[Configuration]
      res    <- ZIO.succeed(Server.Config.default.port(config.server.port))
    } yield res
  }
  private val logMiddleware: HandlerAspect[Any, Unit] = Middleware.debug

  private val buildEnv =
    configProvider >+>
      Configuration.live >+>
      server >+>
      Server.live >+>
      Scope.default >+>
      zioDS >+>
      LiquibaseService.liquibaseLayer >+>
      ConstantRepository.live >+>
      ConstantService.live >+>
      BookingRepository.live >+>
      BookingService.live >+>
      BookService.live >+>
      TelegramUserRepository.live >+>
      TelegramUserService.live >+>
      AppointmentDateRepository.live >+>
      AppointmentDateService.live >+>
      ScheduleService.live >+>
      DateManageService.live >+>
      AppointmentDateApi.live >+>
      BookingApi.live >+>
      ConstantApi.live >+>
      PreStartService.live ++
      LiquibaseService.live

  private val build = for {
    _                     <- PreStartService.preStart
    appointmentDateRoutes <- AppointmentDateApi.routes
    bookingRoutes         <- BookingApi.routes
    constantRoutes        <- ConstantApi.routes
    _                     <- Server.serve((appointmentDateRoutes ++ bookingRoutes ++ constantRoutes) @@ logMiddleware)
  } yield ()

  val app: ZIO[Any, Throwable, Unit] = build.provideSomeLayer(buildEnv)
}
