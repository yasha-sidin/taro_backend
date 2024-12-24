package ru.otus
package service

import db.LiquibaseService
import repository.{ConstantRepository, Repository}

import error.{ExpectedFailure, MigrationFailure}
import zio.{ULayer, ZIO, ZLayer}
import zio.macros.accessible

@accessible
object PreStartService {
  private type PreStartService    = Service
  private type PreStartServiceEnv =
    Repository.Env with ConstantRepository.Service with ConstantService.Service with LiquibaseService.Service with LiquibaseService.Liqui

  trait Service {
    def preStart: ZIO[PreStartServiceEnv, ExpectedFailure, Unit]
  }

  class ServiceImpl extends Service {
    override def preStart: ZIO[PreStartServiceEnv, ExpectedFailure, Unit] =
      for {
        _ <- LiquibaseService.performMigration.mapError(er => MigrationFailure(er.getMessage))
        _ <- ConstantService.initConstants()
      } yield ()
  }

  val live: ULayer[PreStartService] = ZLayer.succeed(new ServiceImpl)
}
