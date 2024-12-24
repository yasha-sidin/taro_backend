package ru.otus
package repository

import io.getquill.{EntityQuery, Quoted}
import `type`.ZIOTypeAlias.DIO
import error.DBFailure
import `type`.AppConstant

import dao.Constant
import zio.{ULayer, ZLayer}
import zio.macros.accessible

@accessible
object ConstantRepository {

  trait Service {
    def getConstantByKey(key: AppConstant): DIO[Option[Constant]]
    def insertConstant(constant: Constant): DIO[Constant]
    def updateConstant(constant: Constant): DIO[Constant]
    def deleteConstantByKey(key: AppConstant): DIO[Boolean]
  }

  class ServiceImpl extends Service {
    protected val dc: db.Ctx.type = db.Ctx
    import dc._

    val schema: Quoted[EntityQuery[Constant]] = quote {
      querySchema[Constant]("constant")
    }

    override def getConstantByKey(key: AppConstant): DIO[Option[Constant]] =
      dc.run {
        quote {
          schema.filter(_.key == lift(key))
        }
      }.mapBoth(e => DBFailure(e), _.headOption)

    override def insertConstant(constant: Constant): DIO[Constant] =
      dc.run {
        quote {
          schema.insertValue(lift(constant))
        }
      }.mapBoth(e => DBFailure(e), _ => constant)

    override def updateConstant(constant: Constant): DIO[Constant] =
      dc.run {
        quote {
          schema
            .filter(_.key == lift(constant.key))
            .updateValue(lift(constant))
        }
      }.mapBoth(e => DBFailure(e), _ => constant)

    override def deleteConstantByKey(key: AppConstant): DIO[Boolean] =
      dc.run {
        quote {
          schema.filter(_.key == lift(key)).delete
        }
      }.mapBoth(e => DBFailure(e), _ > 0)
  }

  val live: ULayer[Service] = ZLayer.succeed(new ServiceImpl)
}
