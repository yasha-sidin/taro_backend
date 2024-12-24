package ru.otus
package repository

import dao.JwtToken
import `type`.ZIOTypeAlias.DIO

import io.getquill.{ EntityQuery, Quoted }
import error.DBFailure
import zio.{ ULayer, ZLayer }
import zio.macros.accessible

import java.util.UUID

@accessible
object JwtTokenRepository {
  private type JwtTokenRepository = Service

  trait Service extends Repository.Service {
    def getUserAdminTokens(userId: UUID): DIO[List[JwtToken]]
    def getDataById(id: UUID): DIO[Option[JwtToken]]
    def insertData(data: JwtToken): DIO[JwtToken]
    def changeFlagDeletedById(id: UUID): DIO[Unit]
    def updateData(data: JwtToken): DIO[JwtToken]
    def getAllData: DIO[List[JwtToken]]

  }

  class ServiceImpl extends Service {
    import dc._
    protected val schema: Quoted[EntityQuery[JwtToken]] =
      querySchema[JwtToken]("""jwt_token""").filter(_.isDeleted == lift(false))

    override def getUserAdminTokens(userId: UUID): DIO[List[JwtToken]] =
      dc.run {
        quote {
          schema.filter(row => row.userAdminId == lift(userId))
        }
      }.mapError(er => DBFailure(er))

    override def getDataById(id: UUID): DIO[Option[JwtToken]] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er), _.headOption)

    override def insertData(data: JwtToken): DIO[JwtToken] =
      dc.run {
        quote {
          schema.insertValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def changeFlagDeletedById(id: UUID): DIO[Unit] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(id)).update(_.isDeleted -> lift(true))
        }
      }.mapBoth(er => DBFailure(er), _ => {})

    override def updateData(data: JwtToken): DIO[JwtToken] =
      dc.run {
        quote {
          schema.filter(el => el.id == lift(data.id)).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    override def getAllData: DIO[List[JwtToken]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er), _.toList)
  }

  val live: ULayer[JwtTokenRepository] = ZLayer.succeed(new ServiceImpl)
}
