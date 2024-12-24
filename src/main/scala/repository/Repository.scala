package ru.otus
package repository

import `type`.ZIOTypeAlias.DIO
import db.DataSource

import io.getquill.{EntityQuery, Quoted}

object Repository {
  type Env = DataSource

  trait Service {
    protected val dc: db.Ctx.type = db.Ctx
  }
}
