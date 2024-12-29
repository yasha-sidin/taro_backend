package ru.otus
package repository

import dao.TelegramUser

import io.getquill.{ EntityQuery, Quoted }
import `type`.ZIOTypeAlias.DIO
import error.DBFailure
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object TelegramUserRepository {
  private type TelegramUserRepository = Service

  trait Service {
    def getDataById(id: UUID): DIO[Option[TelegramUser]]
    def insertData(data: TelegramUser): DIO[TelegramUser]
    def deletedById(id: UUID): DIO[Unit]
    def updateData(data: TelegramUser): DIO[TelegramUser]
    def getAllData: DIO[List[TelegramUser]]
    def getUserByChatId(chatId: Long): DIO[Option[TelegramUser]]
  }

  class ServiceImpl extends Service {
    private val dc: db.Ctx.type = db.Ctx
    import dc._
    private val schema: Quoted[EntityQuery[TelegramUser]] =
      querySchema[TelegramUser]("""telegram_user""")

    override def getDataById(id: UUID): DIO[Option[TelegramUser]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.headOption)

    override def insertData(data: TelegramUser): DIO[TelegramUser] =
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

    override def updateData(data: TelegramUser): DIO[TelegramUser] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _ => data)

    override def getAllData: DIO[List[TelegramUser]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.toList)

    override def getUserByChatId(chatId: Long): DIO[Option[TelegramUser]] =
      dc.run {
        quote {
          schema.filter(el => el.chatId == lift(chatId)).take(1)
        }
      }.mapBoth(er => DBFailure(er, er.getMessage), _.headOption)
  }

  val live: ULayer[TelegramUserRepository] = ZLayer.succeed(new ServiceImpl)
}
