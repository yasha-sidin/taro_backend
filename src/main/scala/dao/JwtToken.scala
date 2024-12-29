package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class JwtToken(
    id: UUID,
    userAdminId: UUID,
    token: String,
    expiresAt: Instant,
    lastUsedAt: Instant,
    override val createdAt: Instant,
    override val updatedAt: Instant,
  ) extends Model(createdAt, updatedAt)
