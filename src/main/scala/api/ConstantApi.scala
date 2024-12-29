package ru.otus
package api

import repository.ConstantRepository
import service.ConstantService
import util.HttpHelper
import configuration.Configuration

import ru.otus.`type`.AppConstant
import ru.otus.error.ValidationFailure
import zio.http._
import zio.json._
import zio._
import zio.macros.accessible

@accessible
object ConstantApi {
  private type ConstantApi    = Service
  private type ConstantApiEnv =
    db.DataSource with ConstantRepository.Service with ConstantService.Service

  trait Service {
    def routes: Routes[ConstantApiEnv, Response]
  }

  class ServiceImpl(val prefixPath: String) extends Service {
    private val baseUrl = "constant"

    override def routes: Routes[ConstantApiEnv, Response] =
      Routes(
        Method.GET / prefixPath / baseUrl / ""            -> handler { (_: Request) =>
          for {
            res <- ConstantService
                     .getConstants
                     .tapError(er => ZIO.logError(er.message))
                     .mapError(HttpHelper.handleExpectedFailure)
          } yield Response.json(res.toJson)
        },
        Method.GET / prefixPath / baseUrl / string("key") / "" -> handler { (key: String, _: Request) =>
          for {
            constant <- ZIO
                          .succeed(AppConstant.withNameOption(key))
                          .some
                          .orElseFail(ValidationFailure("Not valid AppConstant name"))
                          .mapError(HttpHelper.handleExpectedFailure)
            res      <- ConstantService
                          .getConstantByKey(constant)
                          .tapError(er => ZIO.logError(er.message))
                          .mapError(HttpHelper.handleExpectedFailure)
          } yield Response.json(res.toJson)
        },
      )
  }

  val live: URLayer[Configuration, ConstantApi] = ZLayer.fromZIO {
    for {
      config  <- ZIO.service[Configuration]
      service <- ZIO.succeed(new ServiceImpl(config.server.prefixPath))
    } yield service
  }
}
