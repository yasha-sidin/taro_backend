package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class TelegramUser(
    id: UUID,
    chatId: Long,
    firstName: String,
    lastName: Option[String],
    username: Option[String],
    languageCode: Option[String],
    createdAt: Instant,
    updatedAt: Instant,
  )
