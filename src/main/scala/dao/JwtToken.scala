package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class JwtToken(
    id: UUID,
    userAdminId: Long,
    token: String,
    expiresAt: Instant,
    createdAt: Instant,
    lastUsedAt: Instant,
  )
