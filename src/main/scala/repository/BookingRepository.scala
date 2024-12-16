package ru.otus
package repository

import dao.Booking

import io.getquill.{ EntityQuery, Quoted }
import `type`.BookingStatus
import `type`.ZIOTypeAlias.DIO

import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object BookingRepository extends Repository.Accessible[Booking, UUID] {
  private type BookingRepository = Service

  trait Service extends Repository.Service[Booking, UUID] {
    def getBookingsByStatus(status: BookingStatus): DIO[List[Booking]]
    def getTelegramUserBookings(userId: UUID): DIO[List[Booking]]
    def getDateBookings(dateId: UUID): DIO[List[Booking]]
  }

  class ServiceImpl extends Service {
    import dc._
    override protected val getId: Booking => UUID = data => data.id

    override val schema: Quoted[EntityQuery[Booking]] =
      querySchema[Booking]("""booking""")

    override def getBookingsByStatus(status: BookingStatus): DIO[List[Booking]] =
      getFilteredData(data => data.status, status, (a: BookingStatus, b: BookingStatus) => a == b)

    override def getTelegramUserBookings(userId: UUID): DIO[List[Booking]] =
      getFilteredData(data => data.userId, userId, (a: UUID, b: UUID) => a == b)

    override def getDateBookings(dateId: UUID): DIO[List[Booking]] =
      getFilteredData(data => data.dateId, dateId, (a: UUID, b: UUID) => a == b)
  }

  val live: ULayer[BookingRepository] = ZLayer.succeed(new ServiceImpl)
}
