package ru.otus
package `type`

import db.DataSource

import error.DBFailure
import zio.ZIO

object ZIOTypeAlias {
  type DIO[T] = ZIO[DataSource, DBFailure, T]
}
