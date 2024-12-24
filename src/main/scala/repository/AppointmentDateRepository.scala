package ru.otus
package repository

import dao.AppointmentDate
import `type`.AppointmentDateStatus
import `type`.ZIOTypeAlias.DIO
import error.DBFailure

import io.getquill.{ EntityQuery, Quoted }
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.time.Instant
import java.util.UUID

@accessible
object AppointmentDateRepository {
  private type AvailableDateRepository = Service

  trait Service extends Repository.Service {
    def getDatesByStatus(status: AppointmentDateStatus): DIO[List[AppointmentDate]]
    def checkPeriodExists(dateFrom: Instant, dateTo: Instant): DIO[Boolean]
    def getDataById(id: UUID): DIO[Option[AppointmentDate]]
    def insertData(data: AppointmentDate): DIO[AppointmentDate]
    def changeFlagDeletedById(id: UUID): DIO[Unit]
    def updateData(data: AppointmentDate): DIO[AppointmentDate]
    def getAllData: DIO[List[AppointmentDate]]
  }

  private class ServiceImpl extends Service {
    import dc._
    protected val schema: Quoted[EntityQuery[AppointmentDate]] =
      querySchema[AppointmentDate]("""appointment_date""").filter(_.isDeleted == lift(false))

    override def getDatesByStatus(status: AppointmentDateStatus): DIO[List[AppointmentDate]] =
      dc.run {
        quote {
          schema.filter(row => row.status == lift(status))
        }
      }.mapError(er => DBFailure(er))

    override def checkPeriodExists(dateFrom: Instant, dateTo: Instant): DIO[Boolean] =
      dc.run {
        quote {
          schema.filter(row => sql"(${row.dateFrom} < ${lift(dateTo)} AND ${row.dateTo} > ${lift(dateFrom)})".as[Boolean]).nonEmpty
        }
      }.mapError(er => DBFailure(er))

    override def getDataById(id: UUID): DIO[Option[AppointmentDate]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er), _.headOption)

    override def insertData(data: AppointmentDate): DIO[AppointmentDate] =
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

    override def updateData(data: AppointmentDate): DIO[AppointmentDate] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def getAllData: DIO[List[AppointmentDate]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er), _.toList)
  }

  val live: ULayer[AvailableDateRepository] = ZLayer.succeed(new ServiceImpl)
}
