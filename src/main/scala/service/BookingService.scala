package ru.otus
package service

import `type`.BookingStatus
import dao.Booking
import error.{ ExpectedFailure, NotFoundFailure }
import repository.{ BookingRepository, Repository }

import zio.{ Random, ULayer, ZIO, ZLayer }
import zio.macros.accessible

import java.time.Instant
import java.util.UUID

@accessible
object BookingService {

  private type BookingService = Service

  private type BookingServiceEnv = Repository.Env with BookingRepository.Service

  case class BookingCreate(
      status: BookingStatus,
      dateId: UUID,
      userId: UUID,
      canReturn: Boolean,
      timeToConfirm: Instant,
    )

  trait Service {
    def createBooking(bookingCreate: BookingCreate): ZIO[BookingServiceEnv, ExpectedFailure, Booking]
    def updateBooking(booking: Booking): ZIO[BookingServiceEnv, ExpectedFailure, Booking]
    def getBookingById(id: UUID): ZIO[BookingServiceEnv, ExpectedFailure, Booking]
    def getDateBookings(dateId: UUID): ZIO[BookingServiceEnv, ExpectedFailure, List[Booking]]
  }

  class ServiceImpl extends Service {

    override def createBooking(bookingCreate: BookingCreate): ZIO[BookingServiceEnv, ExpectedFailure, Booking] =
      for {
        uuid      <- Random.nextUUID
        createdAt <- ZIO.succeed(Instant.now())
        booking   <- BookingRepository.insertData(
                       Booking(
                         uuid,
                         bookingCreate.userId,
                         bookingCreate.dateId,
                         bookingCreate.status,
                         bookingCreate.canReturn,
                         bookingCreate.timeToConfirm,
                         createdAt,
                         createdAt,
                       )
                     )
      } yield booking

    override def updateBooking(booking: Booking): ZIO[BookingServiceEnv, ExpectedFailure, Booking] =
      for {
        now            <- ZIO.succeed(Instant.now())
        updatedBooking <- ZIO.succeed(booking.copy(updatedAt = now))
        updated        <- BookingRepository.updateData(updatedBooking)
      } yield updated

    override def getBookingById(id: UUID): ZIO[BookingServiceEnv, ExpectedFailure, Booking] =
      BookingRepository.getDataById(id).some.orElseFail(NotFoundFailure(s"Booking with id $id has not been found."))

    override def getDateBookings(dateId: UUID): ZIO[BookingServiceEnv, ExpectedFailure, List[Booking]] =
      BookingRepository.getDateBookings(dateId)
  }

  val live: ULayer[BookingService] = ZLayer.succeed(new ServiceImpl)
}
