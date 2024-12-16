package ru.otus

import zio.{ Config, ConfigProvider, ZIO, ZLayer }
import zio.config.magnolia._

package object configuration {
  case class AppConfig(
      auth: AuthConfig,
      postgres: PostgresConfig,
      liquibase: LiquibaseConfig,
      notification: NotificationConfig,
      server: ServerConfig
    )

  type Configuration = AppConfig

  case class AuthConfig(
      secretKey: String,
      accessTokenLifetimeMinutes: Long,
      refreshTokenLifetimeDays: Long,
      deleteExpiredTokensJobHours: Long,
      serverApiKey: String,
      superAdmin: String,
      adminPassword: String,
    )
  case class NotificationConfig(telegramBotApiKey: String)
  case class LiquibaseConfig(changeLog: String)
  case class PostgresConfig(
      url: String,
      databaseName: String,
      user: String,
      password: String,
    )
  case class ServerConfig(
      host: String,
      port: Int,
    )

  private val configDescriptor: Config[AppConfig] = deriveConfig[AppConfig]

  object Configuration {
    val live: ZLayer[ConfigProvider, Config.Error, Configuration] = ZLayer {
      ZIO.serviceWithZIO[ConfigProvider](provider => provider.load(configDescriptor))
    }
  }
}
