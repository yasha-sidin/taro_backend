package ru.otus
package repository

import dao.{ AppointmentDate, Booking, TelegramUser }
import `type`.BookingStatus
import `type`.ZIOTypeAlias.DIO

import io.getquill.{ EntityQuery, Quoted }
import error.DBFailure

import zio.{ ULayer, ZIO, ZLayer }
import zio.macros.accessible

import java.time.Instant
import java.util.UUID

@accessible
object BookingRepository {
  private type BookingRepository = Service

  trait Service {
    def getBookingsByStatus(status: BookingStatus): DIO[List[Booking]]
    def getTelegramUserBookings(userId: UUID): DIO[List[Booking]]
    def getDateBookings(dateId: UUID): DIO[List[Booking]]
    def getDataById(id: UUID): DIO[Option[Booking]]
    def insertData(data: Booking): DIO[Booking]
    def deletedById(id: UUID): DIO[Unit]
    def updateData(data: Booking): DIO[Booking]
    def getAllData: DIO[List[Booking]]
    def getBookingsToCancel: DIO[List[Booking]]
    def getBookingsWithDateByStatus(status: BookingStatus): DIO[List[(Booking, AppointmentDate)]]
    def getBookingsWithDateByChatId(chatId: Long): DIO[List[(Booking, AppointmentDate)]]
    def getFilteredBookingWithDate(status: Option[BookingStatus], chatId: Option[Long]): DIO[List[(Booking, AppointmentDate)]]
  }

  class ServiceImpl extends Service {
    private val dc: db.Ctx.type                                       = db.Ctx
    import dc._
    protected val schema: Quoted[EntityQuery[Booking]]                =
      querySchema[Booking]("""booking""")
    private val schemaDate: Quoted[EntityQuery[AppointmentDate]]      =
      querySchema[AppointmentDate]("""appointment_date""")
    private val schemaTelegramUser: Quoted[EntityQuery[TelegramUser]] =
      querySchema[TelegramUser]("""telegram_user""")

    override def getBookingsByStatus(status: BookingStatus): DIO[List[Booking]] =
      dc.run {
        quote {
          schema.filter(row => row.status == lift(status))
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def getTelegramUserBookings(userId: UUID): DIO[List[Booking]] =
      dc.run {
        quote {
          schema.filter(row => row.userId == lift(userId))
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def getDateBookings(dateId: UUID): DIO[List[Booking]] =
      dc.run {
        quote {
          schema.filter(row => row.dateId == lift(dateId))
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def getDataById(id: UUID): DIO[Option[Booking]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.headOption)

    override def insertData(data: Booking): DIO[Booking] =
      dc.run {
        quote {
          schema.insertValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def deletedById(id: UUID): DIO[Unit] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).delete
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => {})

    override def updateData(data: Booking): DIO[Booking] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def getAllData: DIO[List[Booking]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)

    override def getBookingsToCancel: DIO[List[Booking]] =
      (for {
        now <- ZIO.succeed(Instant.now())
        res <- dc.run {
                 quote {
                   schema
                     .filter(row => row.status == lift(BookingStatus.Active: BookingStatus))
                     .filter(row => sql"${row.timeToConfirm} < ${lift(now)}".as[Boolean])
                 }
               }
      } yield res).mapBoth(er => DBFailure(er, er.getMessage), _.toList)

    override def getBookingsWithDateByStatus(status: BookingStatus): DIO[List[(Booking, AppointmentDate)]] =
      dc.run {
        schema
          .filter(_.status == lift(status: BookingStatus))
          .join(schemaDate)
          .on((bookingItem, date) => bookingItem.dateId == date.id)
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)

    override def getBookingsWithDateByChatId(chatId: Long): DIO[List[(Booking, AppointmentDate)]] =
      dc.run {
        schema
          .join(schemaTelegramUser)
          .on((bookingItem, tgUser) => bookingItem.userId == tgUser.id)
          .filter(item => item._2.chatId == lift(chatId))
          .join(schemaDate)
          .on { case ((booking, _), date) => booking.dateId == date.id }
          .map { case ((booking, _), date) => (booking, date) }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)

    override def getFilteredBookingWithDate(status: Option[BookingStatus], chatId: Option[Long]): DIO[List[(Booking, AppointmentDate)]] =
      dc.run {
        schema
          .filter(item => lift(status).filterIfDefined((_: BookingStatus) == item.status))
          .join(schemaTelegramUser)
          .on((bookingItem, tgUser) => bookingItem.userId == tgUser.id)
          .filter { case (_, tgUser) =>
            lift(chatId).filterIfDefined(_ == tgUser.chatId)
          }
          .join(schemaDate)
          .on { case ((booking, _), date) => booking.dateId == date.id }
          .map { case ((booking, _), date) => (booking, date) }
      }.mapBoth(er => DBFailure(er, er.toString), _.toList)
  }

  val live: ULayer[BookingRepository] = ZLayer.succeed(new ServiceImpl)
}
