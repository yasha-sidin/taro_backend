package ru.otus
package dao

import java.time.Instant
import java.util.UUID

case class UserAdmin(
    id: UUID,
    username: String,
    password: String,
    deleted: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
  )
