package ru.otus
package repository

import dao.UserAdmin

import io.getquill.{ EntityQuery, Quoted }
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object UserAdminRepository extends Repository.Accessible[UserAdmin, UUID] {
  private type UserAdminRepository = Service

  trait Service extends Repository.Service[UserAdmin, UUID] {}

  class ServiceImpl extends Service {
    import dc._
    override protected val getId: UserAdmin => UUID     = data => data.id
    override val schema: Quoted[EntityQuery[UserAdmin]] =
      querySchema[UserAdmin]("""user_admin""")
  }

  val live: ULayer[UserAdminRepository] = ZLayer.succeed(new ServiceImpl)
}
