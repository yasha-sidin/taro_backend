package ru.otus
package repository

import dao.AppointmentDate
import `type`.AppointmentDateStatus
import `type`.ZIOTypeAlias.DIO
import error.DBFailure

import io.getquill.{ EntityQuery, Quoted }
import zio.{ ULayer, ZIO, ZLayer }
import zio.macros.accessible

import java.time.Instant
import java.util.UUID

@accessible
object AppointmentDateRepository {
  private type AvailableDateRepository = Service

  trait Service {
    def getDatesByStatus(status: AppointmentDateStatus): DIO[List[AppointmentDate]]
    def checkPeriodExists(dateFrom: Instant, dateTo: Instant): DIO[Boolean]
    def getDataById(id: UUID): DIO[Option[AppointmentDate]]
    def insertData(data: AppointmentDate): DIO[AppointmentDate]
    def deletedById(id: UUID): DIO[Unit]
    def updateData(data: AppointmentDate): DIO[AppointmentDate]
    def getAllData: DIO[List[AppointmentDate]]
    def updateExpiredDates(): DIO[Long]
  }

  private class ServiceImpl extends Service {
    private val dc: db.Ctx.type = db.Ctx
    import dc._
    private val schema: Quoted[EntityQuery[AppointmentDate]] =
      querySchema[AppointmentDate]("""appointment_date""")

    override def getDatesByStatus(status: AppointmentDateStatus): DIO[List[AppointmentDate]] =
      dc.run {
        quote {
          schema.filter(row => row.status == lift(status))
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def checkPeriodExists(dateFrom: Instant, dateTo: Instant): DIO[Boolean] =
      dc.run {
        quote {
          schema.filter(row => sql"(${row.dateFrom} < ${lift(dateTo)} AND ${row.dateTo} > ${lift(dateFrom)})".as[Boolean]).nonEmpty
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def getDataById(id: UUID): DIO[Option[AppointmentDate]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.headOption)

    override def insertData(data: AppointmentDate): DIO[AppointmentDate] =
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

    override def updateData(data: AppointmentDate): DIO[AppointmentDate] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def getAllData: DIO[List[AppointmentDate]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)

    override def updateExpiredDates(): DIO[Long] =
      for {
        now          <- ZIO.succeed(Instant.now())
        updatedCount <- dc.run(
                          quote {
                            schema
                              .filter(d => d.status == lift(AppointmentDateStatus.Available: AppointmentDateStatus))
                              .filter(el => sql"${el.bookingDeadline} < ${lift(now)}".as[Boolean])
                              .update(_.status -> lift(AppointmentDateStatus.Expired: AppointmentDateStatus), _.updatedAt -> lift(now))
                          }
                        ).mapError(er => DBFailure(er, er.getMessage))
      } yield updatedCount
  }

  val live: ULayer[AvailableDateRepository] = ZLayer.succeed(new ServiceImpl)
}
