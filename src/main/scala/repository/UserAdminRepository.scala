package ru.otus
package repository

import dao.UserAdmin

import io.getquill.{ EntityQuery, Quoted }
import `type`.ZIOTypeAlias.DIO
import error.DBFailure
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object UserAdminRepository {
  private type UserAdminRepository = Service

  trait Service extends Repository.Service {
    def getDataById(id: UUID): DIO[Option[UserAdmin]]
    def insertData(data: UserAdmin): DIO[UserAdmin]
    def changeFlagDeletedById(id: UUID): DIO[Unit]
    def updateData(data: UserAdmin): DIO[UserAdmin]
    def getAllData: DIO[List[UserAdmin]]
  }

  class ServiceImpl extends Service {
    import dc._
    protected val schema: Quoted[EntityQuery[UserAdmin]] =
      querySchema[UserAdmin]("""telegram_user""").filter(_.isDeleted == lift(false))

    override def getDataById(id: UUID): DIO[Option[UserAdmin]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er), _.headOption)

    override def insertData(data: UserAdmin): DIO[UserAdmin] =
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

    override def updateData(data: UserAdmin): DIO[UserAdmin] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def getAllData: DIO[List[UserAdmin]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er), _.toList)
  }

  val live: ULayer[UserAdminRepository] = ZLayer.succeed(new ServiceImpl)
}
