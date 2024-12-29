package ru.otus
package api

import repository.{ AppointmentDateRepository, BookingRepository, ConstantRepository, TelegramUserRepository }
import service.{ AppointmentDateService, BookService, BookingService, ConstantService, DateManageService, TelegramUserService }
import util.HttpHelper
import dto.{ BookDateBody, BookingDateBody, BookingQueryParams }

import configuration.Configuration
import zio.http._
import zio.http.{ uuid => uuidField }
import zio.json._
import zio._
import zio.macros.accessible

import java.util.UUID

@accessible
object BookingApi {
  private type BookingApi    = Service
  private type BookingApiEnv =
    db.DataSource
      with BookingRepository.Service
      with BookingService.Service
      with AppointmentDateRepository.Service
      with AppointmentDateService.Service
      with DateManageService.Service
      with BookService.Service
      with ConstantRepository.Service
      with ConstantService.Service
      with TelegramUserRepository.Service
      with TelegramUserService.Service

  trait Service {
    def routes: Routes[BookingApiEnv, Response]
  }

  class ServiceImpl(val prefixPath: String) extends Service {
    private val baseUrl = "booking"

    override def routes: Routes[BookingApiEnv, Response] =
      Routes(
        Method.POST / prefixPath / baseUrl / ""                     -> handler { (request: Request) =>
          for {
            decodedBody <- HttpHelper.getBodyFromRequest[BookDateBody](request)
            result      <- BookService
                             .bookDate(decodedBody.dateId, decodedBody.telegramDetails)
                             .tapError(er => ZIO.logError(er.message))
                             .mapError(HttpHelper.handleExpectedFailure)
            respBody    <- ZIO.succeed(BookingDateBody.fromTuple(result))
          } yield Response.json(respBody.toJson)
        },
        Method.DELETE / prefixPath / baseUrl / uuidField("id") / "" -> handler { (id: UUID, _: Request) =>
          BookService
            .cancelBooking(id)
            .tapError(er => ZIO.logError(er.message))
            .mapBoth(HttpHelper.handleExpectedFailure, res => Response.json(BookingDateBody.fromTuple(res).toJson))
        },
        Method.GET / prefixPath / baseUrl / ""                      -> handler { (request: Request) =>
          for {
            searchParams <- HttpHelper.parseQueryParams[BookingQueryParams](request.url.queryParams)
            res          <- BookingService
                              .getFilteredBookingsWithDate(searchParams.status, searchParams.chatId)
                              .tapError(er => ZIO.logError(er.message))
                              .mapError(HttpHelper.handleExpectedFailure)
            respBody     <- ZIO.succeed(res.map(el => BookingDateBody.fromTuple((el._2, el._1))))
          } yield Response.json(respBody.toJson)
        },
      )
  }

  val live: URLayer[Configuration, BookingApi] = ZLayer.fromZIO {
    for {
      config  <- ZIO.service[Configuration]
      service <- ZIO.succeed(new ServiceImpl(config.server.prefixPath))
    } yield service
  }
}
