package ru.otus
package service

import repository.TelegramUserRepository
import error.ExpectedFailure
import dao.TelegramUser

import dto.TelegramDetails
import zio.{Random, ULayer, ZIO, ZLayer}
import zio.macros.accessible

import java.time.Instant

@accessible
object TelegramUserService {
  private type TelegramUserService    = Service
  private type TelegramUserServiceEnv = db.DataSource with TelegramUserRepository.Service

  trait Service {
    def getOrCreateOrUpdateByTelegramDetails(details: TelegramDetails): ZIO[TelegramUserServiceEnv, ExpectedFailure, TelegramUser]
    def createByTelegramDetails(details: TelegramDetails): ZIO[TelegramUserServiceEnv, ExpectedFailure, TelegramUser]
    def updateTelegramUser(telegramUser: TelegramUser): ZIO[TelegramUserServiceEnv, ExpectedFailure, TelegramUser]
  }

  class ServiceImpl extends Service {
    override def getOrCreateOrUpdateByTelegramDetails(details: TelegramDetails): ZIO[TelegramUserServiceEnv, ExpectedFailure, TelegramUser] =
      for {
        userOpt <- TelegramUserRepository.getUserByChatId(details.chatId)
        user    <- userOpt match {
                     case Some(value) =>
                       this.updateTelegramUser(
                         value.copy(
                           firstName = details.firstName,
                           lastName = details.lastName,
                           username = details.username,
                           languageCode = details.languageCode,
                         )
                       )
                     case None        => this.createByTelegramDetails(details)
                   }
      } yield user

    override def createByTelegramDetails(details: TelegramDetails): ZIO[TelegramUserServiceEnv, ExpectedFailure, TelegramUser] =
      for {
        now  <- ZIO.succeed(Instant.now())
        uuid <- Random.nextUUID
        user <- TelegramUserRepository.insertData(
                  TelegramUser(
                    uuid,
                    details.chatId,
                    details.firstName,
                    details.lastName,
                    details.username,
                    details.languageCode,
                    now,
                    now,
                  )
                )
      } yield user

    override def updateTelegramUser(telegramUser: TelegramUser): ZIO[TelegramUserServiceEnv, ExpectedFailure, TelegramUser] =
      for {
        now  <- ZIO.succeed(Instant.now())
        user <- TelegramUserRepository.updateData(telegramUser.copy(updatedAt = now))
      } yield user
  }

  val live: ULayer[TelegramUserService] = ZLayer.succeed(new ServiceImpl)
}
