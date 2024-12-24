package ru.otus
package dao

import java.time.Instant

abstract class Model(
    val isDeleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
  )
