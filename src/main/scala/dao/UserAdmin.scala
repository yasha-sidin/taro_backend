package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class UserAdmin(
    id: UUID,
    username: String,
    password: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
  ) extends Model(createdAt, updatedAt)
