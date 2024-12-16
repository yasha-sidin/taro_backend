package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class Notification(
    id: UUID,
    body: String,
    createdAt: Instant,
    sendAfter: Instant,
    telegramUserId: UUID,
    sent: Boolean = false,
  )
