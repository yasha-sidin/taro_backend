package ru.otus
package api

import repository.{AppointmentDateRepository, BookingRepository}
import service.{AppointmentDateService, BookingService, DateManageService}
import dto.{AppointmentDateAdd, AppointmentDateQueryParams}
import util.HttpHelper

import configuration.Configuration
import zio.http._
import zio.http.{uuid => uuidField}
import zio.json._
import zio._
import zio.macros.accessible

import java.util.UUID

@accessible
object AppointmentDateApi {
  private type AppointmentDateApi    = Service
  private type AppointmentDateApiEnv =
    db.DataSource with BookingRepository.Service with BookingService.Service with AppointmentDateRepository.Service with AppointmentDateService.Service with DateManageService.Service

  trait Service {
    def routes: Routes[AppointmentDateApiEnv, Response]
  }

  class ServiceImpl(val prefixPath: String) extends Service {
    private val baseUrl = "appointment_date"

    override def routes: Routes[AppointmentDateApiEnv, Response] =
      Routes(
        Method.POST / prefixPath / baseUrl / ""        -> handler { (request: Request) =>
          for {
            decodedBody <- HttpHelper.getBodyFromRequest[AppointmentDateAdd](request)
            result      <- DateManageService
                             .addAppointment(decodedBody)
                             .tapError(er => ZIO.logError(er.message))
                             .mapError(HttpHelper.handleExpectedFailure)
          } yield Response.json(result.toJson)
        },
        Method.DELETE / prefixPath / baseUrl / uuidField("id") / "" -> handler { (id: UUID, _: Request) =>
          DateManageService
            .removeDate(id)
            .tapError(er => ZIO.logError(er.message))
            .mapBoth(HttpHelper.handleExpectedFailure, _ => Response.ok)
        },
        Method.GET / prefixPath / baseUrl / ""                      -> handler { (request: Request) =>
          for {
            searchParams <- HttpHelper.parseQueryParams[AppointmentDateQueryParams](request.url.queryParams)
            res          <- AppointmentDateService
                              .getFilteredDates(searchParams)
                              .tapError(er => ZIO.logError(er.message))
                              .mapError(HttpHelper.handleExpectedFailure)
          } yield Response.json(res.toJson)
        },
      )
  }

  val live: URLayer[Configuration, ServiceImpl] = ZLayer.fromZIO {
    for {
      config <- ZIO.service[Configuration]
      service <- ZIO.succeed(new ServiceImpl(config.server.prefixPath))
    } yield service
  }
}
