package ru.otus
package service

import repository.{AppointmentDateRepository, BookingRepository, ConstantRepository, TelegramUserRepository}
import error.{ConfirmationNotAvailableFailure, DBFailure, ExpectedFailure, NotAvailableFailure}
import dao.{AppointmentDate, Booking}
import `type`.{AppConstant, AppointmentDateStatus, BookingStatus}
import service.BookingService.BookingCreate

import dto.TelegramDetails
import zio.macros.accessible
import zio.{ULayer, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

@accessible
object BookService {
  private type BookService    = Service
  private type BookServiceEnv =
    db.DataSource
      with BookingService.Service
      with BookingRepository.Service
      with AppointmentDateRepository.Service
      with AppointmentDateService.Service
      with ConstantRepository.Service
      with ConstantService.Service
      with TelegramUserRepository.Service
      with TelegramUserService.Service

  trait Service {
    def bookDate(dateId: UUID, telegramDetails: TelegramDetails): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def cancelBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def confirmBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def completeBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)]
    def cancelNotConfirmedBookings(): ZIO[BookServiceEnv, ExpectedFailure, Unit]
    def markBookingsAsCompleted(): ZIO[BookServiceEnv, ExpectedFailure, Unit]
  }

  class ServiceImpl extends Service {
    protected val dc: db.Ctx.type = db.Ctx

    override def bookDate(dateId: UUID, telegramDetails: TelegramDetails): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        now                  <- ZIO.succeed(Instant.now())
        date                 <- AppointmentDateService.getAppointmentDateById(dateId)
        constantValue        <- ConstantService.getConstantValueByKey[Long](AppConstant.MaxTimeToConfirm)
        telegramUser         <- TelegramUserService.getOrCreateOrUpdateByTelegramDetails(telegramDetails)
        timeSecondsToConfirm <- if (now.plusSeconds(constantValue).isAfter(date.bookingDeadline)) {
                                  ZIO.succeed(date.dateFrom)
                                }
                                else {
                                  ZIO.succeed(now.plusSeconds(constantValue))
                                }
        tuple                <- if (date.canBook(now)) {
                                  dc.transaction(
                                    for {
                                      updatedDate <- AppointmentDateService.updateAppointmentDate(date.copy(status = AppointmentDateStatus.Booked))
                                      booking     <- BookingService.createBooking(
                                                       BookingCreate(
                                                         BookingStatus.Active,
                                                         dateId,
                                                         telegramUser.id,
                                                         canReturn = true,
                                                         timeToConfirm = timeSecondsToConfirm,
                                                       )
                                                     )
                                    } yield (updatedDate, booking)
                                  ).mapError(er => DBFailure(er, er.getMessage))
                                }
                                else {
                                  ZIO.logWarning(s"Can't book date with id $dateId for user with id $telegramUser") *>
                                    ZIO.fail(NotAvailableFailure("Date is not available now."))
                                }
      } yield tuple

    override def cancelBooking(bookingId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        now     <- ZIO.succeed(Instant.now())
        booking <- BookingService.getBookingById(bookingId)
        date    <- AppointmentDateService.getAppointmentDateById(booking.dateId)
        tuple   <- if (!date.dateFrom.isBefore(now)) {
                     dc.transaction(
                       for {
                         updatedDate    <- AppointmentDateService.updateAppointmentDate(date.copy(status = AppointmentDateStatus.Available))
                         updatedBooking <- BookingService.updateBooking(booking.copy(status = BookingStatus.Cancelled))
                       } yield (updatedDate, updatedBooking)
                     ).mapError(er => DBFailure(er, er.getMessage))
                   }
                   else {
                     ZIO.logWarning(s"$date is already started") *>
                       ZIO.fail(NotAvailableFailure("Date is is already started."))
                   }
        _       <- ZIO.logInfo(s"Booking $tuple canceled")
      } yield tuple

    override def confirmBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        now     <- ZIO.succeed(Instant.now())
        booking <- BookingService.getBookingById(bookId)
        res     <- if (booking.isActive) {
                     if (booking.canConfirm(now)) {
                       for {
                         updated <- BookingService.updateBooking(booking.copy(status = BookingStatus.Confirmed))
                         date    <- AppointmentDateService.getAppointmentDateById(booking.dateId)
                       } yield (date, updated)
                     }
                     else {
                       for {
                         _    <- this.cancelBooking(booking.id)
                         fail <- ZIO.fail(ConfirmationNotAvailableFailure("Time to confirmation is expired"))
                       } yield fail
                     }
                   }
                   else {
                     ZIO.fail(ConfirmationNotAvailableFailure("Booking is not in Active status"))
                   }
      } yield res

    override def completeBooking(bookId: UUID): ZIO[BookServiceEnv, ExpectedFailure, (AppointmentDate, Booking)] =
      for {
        booking <- BookingService.getBookingById(bookId)
        updated <- BookingService.updateBooking(booking.copy(status = BookingStatus.Completed))
        date    <- AppointmentDateService.getAppointmentDateById(booking.dateId)
      } yield (date, updated)

    override def cancelNotConfirmedBookings(): ZIO[BookServiceEnv, ExpectedFailure, Unit] = for {
      bookings <- BookingRepository.getBookingsToCancel
      _        <- ZIO.logInfo(s"Bookings to cancel count: ${bookings.size}")
      _        <- ZIO.foreachDiscard(bookings)(booking => this.cancelBooking(booking.id))
    } yield ()

    override def markBookingsAsCompleted(): ZIO[BookServiceEnv, ExpectedFailure, Unit] =
      for {
        now               <- ZIO.succeed(Instant.now())
        confirmedBookings <- BookingService.getBookingsWithDateByStatus(BookingStatus.Confirmed)
        filteredBookings  <- ZIO.filterPar(confirmedBookings)(booking => ZIO.succeed(booking._2.dateTo.isBefore(now)))
        _                 <- ZIO.logInfo(s"Bookings to complete count: ${filteredBookings.size}")
        _                 <- ZIO.foreachDiscard(filteredBookings)(booking => this.completeBooking(booking._1.id))
      } yield ()
  }

  val live: ULayer[BookService] = ZLayer.succeed(new ServiceImpl)
}
