package ru.otus
package repository

import dao.{ AppointmentDate, Booking }
import `type`.BookingStatus
import `type`.ZIOTypeAlias.DIO

import io.getquill.{ EntityQuery, Quoted }
import error.DBFailure
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object BookingRepository {
  private type BookingRepository = Service

  trait Service extends Repository.Service {
    def getBookingsByStatus(status: BookingStatus): DIO[List[Booking]]
    def getTelegramUserBookings(userId: UUID): DIO[List[Booking]]
    def getDateBookings(dateId: UUID): DIO[List[Booking]]
    def getDataById(id: UUID): DIO[Option[Booking]]
    def insertData(data: Booking): DIO[Booking]
    def changeFlagDeletedById(id: UUID): DIO[Unit]
    def updateData(data: Booking): DIO[Booking]
    def getAllData: DIO[List[Booking]]
  }

  class ServiceImpl extends Service {
    import dc._
    protected val schema: Quoted[EntityQuery[Booking]] =
      querySchema[Booking]("""booking""").filter(_.isDeleted == lift(false))

    override def getBookingsByStatus(status: BookingStatus): DIO[List[Booking]] =
      dc.run {
        quote {
          schema.filter(row => row.status == lift(status))
        }
      }.mapError(er => DBFailure(er))

    override def getTelegramUserBookings(userId: UUID): DIO[List[Booking]] =
      dc.run {
        quote {
          schema.filter(row => row.userId == lift(userId))
        }
      }.mapError(er => DBFailure(er))

    override def getDateBookings(dateId: UUID): DIO[List[Booking]] =
      dc.run {
        quote {
          schema.filter(row => row.dateId == lift(dateId))
        }
      }.mapError(er => DBFailure(er))

    override def getDataById(id: UUID): DIO[Option[Booking]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er), _.headOption)

    override def insertData(data: Booking): DIO[Booking] =
      dc.run {
        quote {
          schema.insertValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def changeFlagDeletedById(id: UUID): DIO[Unit] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).update(_.isDeleted -> lift(true))
        }
      }.mapBoth(er => DBFailure(er), _ => {})

    override def updateData(data: Booking): DIO[Booking] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def getAllData: DIO[List[Booking]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er), _.toList)
  }

  val live: ULayer[BookingRepository] = ZLayer.succeed(new ServiceImpl)
}
