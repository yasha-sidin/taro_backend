package ru.otus
package repository

import io.getquill.{ EntityQuery, Quoted }
import dao.AppointmentDate
import `type`.AppointmentDateStatus
import `type`.ZIOTypeAlias.DIO

import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible[AppointmentDate]
object AppointmentDateRepository extends Repository.Accessible[AppointmentDate, UUID] {
  private type AvailableDateRepository = Service

  trait Service extends Repository.Service[AppointmentDate, UUID] {
    def getDatesByStatus(status: AppointmentDateStatus): DIO[List[AppointmentDate]]
  }

  private class ServiceImpl extends Service {
    import dc._
    override protected val getId: AppointmentDate => UUID     = data => data.id
    override val schema: Quoted[EntityQuery[AppointmentDate]] =
      querySchema[AppointmentDate]("""appointment_date""")

    override def getDatesByStatus(status: AppointmentDateStatus): DIO[List[AppointmentDate]] =
      getFilteredData(data => data.status, status, (a: AppointmentDateStatus, b: AppointmentDateStatus) => a == b)
  }

  val live: ULayer[AvailableDateRepository] = ZLayer.succeed(new ServiceImpl)
}
