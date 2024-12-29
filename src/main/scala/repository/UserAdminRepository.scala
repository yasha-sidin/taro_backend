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

  trait Service {
    def getDataById(id: UUID): DIO[Option[UserAdmin]]
    def insertData(data: UserAdmin): DIO[UserAdmin]
    def deletedById(id: UUID): DIO[Unit]
    def updateData(data: UserAdmin): DIO[UserAdmin]
    def getAllData: DIO[List[UserAdmin]]
  }

  class ServiceImpl extends Service {
    private val dc: db.Ctx.type = db.Ctx
    import dc._
    private val schema: Quoted[EntityQuery[UserAdmin]] =
      querySchema[UserAdmin]("""telegram_user""")

    override def getDataById(id: UUID): DIO[Option[UserAdmin]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.headOption)

    override def insertData(data: UserAdmin): DIO[UserAdmin] =
      dc.run {
        quote {
          schema.insertValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def deletedById(id: UUID): DIO[Unit] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => {})

    override def updateData(data: UserAdmin): DIO[UserAdmin] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def getAllData: DIO[List[UserAdmin]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)
  }

  val live: ULayer[UserAdminRepository] = ZLayer.succeed(new ServiceImpl)
}
