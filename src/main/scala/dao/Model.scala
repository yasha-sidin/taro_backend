package ru.otus
package dao

import java.time.Instant

abstract class Model(
    val createdAt: Instant,
    val updatedAt: Instant,
  )
