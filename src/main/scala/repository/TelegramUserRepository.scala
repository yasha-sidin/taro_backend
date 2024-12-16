package ru.otus
package repository

import dao.TelegramUser

import io.getquill.{ EntityQuery, Quoted }
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object TelegramUserRepository extends Repository.Accessible[TelegramUser, UUID] {
  private type TelegramUserRepository = Service

  trait Service extends Repository.Service[TelegramUser, UUID] {}

  class ServiceImpl extends Service {
    import dc._
    override protected val getId: TelegramUser => UUID     = data => data.id
    override val schema: Quoted[EntityQuery[TelegramUser]] =
      querySchema[TelegramUser]("""telegram_user""")
  }

  val live: ULayer[TelegramUserRepository] = ZLayer.succeed(new ServiceImpl)
}
