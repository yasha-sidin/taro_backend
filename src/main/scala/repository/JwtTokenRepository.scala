package ru.otus
package repository

import io.getquill.{ EntityQuery, Quoted }
import dao.JwtToken

import `type`.ZIOTypeAlias.DIO
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object JwtTokenRepository extends Repository.Accessible[JwtToken, UUID] {
  private type JwtTokenRepository = Service

  trait Service extends Repository.Service[JwtToken, UUID] {
    def getUserAdminTokens(userId: UUID): DIO[List[JwtToken]]
  }

  class ServiceImpl extends Service {
    import dc._
    override protected val getId: JwtToken => UUID     = data => data.id
    override val schema: Quoted[EntityQuery[JwtToken]] =
      querySchema[JwtToken]("""jwt_token""")

    override def getUserAdminTokens(userId: UUID): DIO[List[JwtToken]] =
      getFilteredData(data => data.userAdminId, userId, (a: UUID, b: UUID) => a == b)
  }

  val live: ULayer[JwtTokenRepository] = ZLayer.succeed(new ServiceImpl)
}
