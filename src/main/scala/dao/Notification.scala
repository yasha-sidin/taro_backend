package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class Notification(
    id: UUID,
    body: String,
    sendAfter: Instant,
    telegramUserId: UUID,
    sent: Boolean = false,
    override val createdAt: Instant,
    override val updatedAt: Instant,
  ) extends Model(createdAt, updatedAt)
