package ru.otus
package repository

import dao.Notification
import `type`.ZIOTypeAlias.DIO

import io.getquill.{ EntityQuery, Quoted }
import error.DBFailure
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object NotificationRepository {
  private type NotificationRepository = Service

  trait Service {
    def getTelegramUserNotifications(userId: UUID): DIO[List[Notification]]
    def getNotifications(sent: Boolean): DIO[List[Notification]]
    def getDataById(id: UUID): DIO[Option[Notification]]
    def insertData(data: Notification): DIO[Notification]
    def deletedById(id: UUID): DIO[Unit]
    def updateData(data: Notification): DIO[Notification]
    def getAllData: DIO[List[Notification]]
  }

  class ServiceImpl extends Service {
    private val dc: db.Ctx.type = db.Ctx
    import dc._
    protected val schema: Quoted[EntityQuery[Notification]] =
      querySchema[Notification]("""notification""")

    override def getTelegramUserNotifications(userId: UUID): DIO[List[Notification]] =
      dc.run {
        quote {
          schema.filter(row => row.telegramUserId == lift(userId))
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def getNotifications(sent: Boolean): DIO[List[Notification]] =
      dc.run {
        quote {
          schema.filter(row => row.sent == lift(sent))
        }
      }.mapError(er => DBFailure(er, er.getMessage))

    override def getDataById(id: UUID): DIO[Option[Notification]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.headOption)

    override def insertData(data: Notification): DIO[Notification] =
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

    override def updateData(data: Notification): DIO[Notification] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def getAllData: DIO[List[Notification]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)
  }

  val live: ULayer[NotificationRepository] = ZLayer.succeed(new ServiceImpl)
}
