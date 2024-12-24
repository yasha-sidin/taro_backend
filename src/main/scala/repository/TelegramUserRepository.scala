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

  trait Service extends Repository.Service {
    def getDataById(id: UUID): DIO[Option[TelegramUser]]
    def insertData(data: TelegramUser): DIO[TelegramUser]
    def changeFlagDeletedById(id: UUID): DIO[Unit]
    def updateData(data: TelegramUser): DIO[TelegramUser]
    def getAllData: DIO[List[TelegramUser]]
  }

  class ServiceImpl extends Service {
    import dc._
    protected val schema: Quoted[EntityQuery[TelegramUser]] =
      querySchema[TelegramUser]("""telegram_user""").filter(_.isDeleted == lift(false))

    override def getDataById(id: UUID): DIO[Option[TelegramUser]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er), _.headOption)

    override def insertData(data: TelegramUser): DIO[TelegramUser] =
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

    override def updateData(data: TelegramUser): DIO[TelegramUser] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def getAllData: DIO[List[TelegramUser]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er), _.toList)
  }

  val live: ULayer[TelegramUserRepository] = ZLayer.succeed(new ServiceImpl)
}
