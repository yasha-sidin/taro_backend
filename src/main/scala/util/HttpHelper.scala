package ru.otus
package util

import error.{ DBFailure, ExpectedFailure, MigrationFailure, ServerRunningError }
import dto.ErrorBody

import zio.ZIO
import zio.json._
import zio.http.{ QueryParams, Request, Response, Status }

object HttpHelper {
  val handleExpectedFailure: ExpectedFailure => Response = {
    case DBFailure(_, msg)       => Response.json(ErrorBody(code = 500, message = msg).toJson).status(Status.InternalServerError)
    case MigrationFailure(msg)   => Response.json(ErrorBody(code = 500, message = msg).toJson).status(Status.InternalServerError)
    case ServerRunningError(msg) => Response.json(ErrorBody(code = 500, message = msg).toJson).status(Status.InternalServerError)
    case error                   => Response.json(ErrorBody(code = 400, message = error.message).toJson).status(Status.BadRequest)
  }

  def getBodyFromRequest[T](request: Request)(implicit encoder: JsonEncoder[T], decoder: JsonDecoder[T]): ZIO[Any, Response, T] =
    for {
      body        <- request
                       .body
                       .asString
                       .mapError(er => Response.json(ErrorBody(code = 400, message = s"Charset decode error: ${er.getMessage}").toJson).status(Status.BadRequest))
      decodedBody <- ZIO
                       .fromEither(body.fromJson[T])
                       .mapError(er => Response.json(ErrorBody(code = 400, message = s"Invalid JSON: $er").toJson).status(Status.BadRequest))
    } yield decodedBody

  def parseQueryParams[A: JsonDecoder](queryParams: QueryParams): ZIO[Any, Response, A] =
    for {
      jsonString <- ZIO.succeed(
                      queryParams
                        .map
                        .map {
                          case (key, values) => s""""$key": "${values.headOption.getOrElse("")}""""
                        }
                        .mkString("{", ",", "}")
                    )
      res        <- ZIO
                      .fromEither(jsonString.fromJson[A])
                      .mapError(er => Response.json(ErrorBody(code = 400, message = s"Wrong query params: $er").toJson).status(Status.BadRequest))
    } yield res

}
