package ru.otus

import configuration.Configuration
import db.{ LiquibaseService, zioDS }
import error.ExpectedFailure
import service.{ ConstantService, PreStartService }

import ru.otus.repository.ConstantRepository
import zio.{ Scope, ZIO, ZLayer }
import zio.config.typesafe.TypesafeConfigProvider

object App {
  private val configProvider = ZLayer.succeed(TypesafeConfigProvider.fromResourcePath())

  private val buildEnv =
    configProvider >+>
      Configuration.live >+>
      Scope.default >+>
      zioDS >+>
      LiquibaseService.liquibaseLayer >+>
      ConstantRepository.live >+>
      ConstantService.live >+>
      PreStartService.live ++
      LiquibaseService.live

  private val build = for {
    _ <- PreStartService.preStart
  } yield ()

  val app: ZIO[Any, ExpectedFailure, Unit] = build.provideSomeLayer(buildEnv)
}
