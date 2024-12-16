package ru.otus
package repository

import io.getquill.{ EntityQuery, Quoted }
import dao.Notification

import `type`.ZIOTypeAlias.DIO
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object NotificationRepository extends Repository.Accessible[Notification, UUID] {
  private type NotificationRepository = Service

  trait Service extends Repository.Service[Notification, UUID] {
    def getTelegramUserNotifications(userId: UUID): DIO[List[Notification]]
    def getNotifications(sent: Boolean): DIO[List[Notification]]
  }

  class ServiceImpl extends Service {
    import dc._
    override protected val getId: Notification => UUID     = data => data.id
    override val schema: Quoted[EntityQuery[Notification]] =
      querySchema[Notification]("""notification""")

    override def getTelegramUserNotifications(userId: UUID): DIO[List[Notification]] =
      getFilteredData(data => data.telegramUserId, userId, (a: UUID, b: UUID) => a == b)

    override def getNotifications(sent: Boolean): DIO[List[Notification]] =
      getFilteredData(data => data.sent, sent, (a: Boolean, b: Boolean) => a == b)
  }

  val live: ULayer[NotificationRepository] = ZLayer.succeed(new ServiceImpl)
}
