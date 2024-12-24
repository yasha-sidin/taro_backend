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
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val isDeleted: Boolean = false,
  ) extends Model(isDeleted, createdAt, updatedAt)
