package ru.otus
package repository

import io.getquill.{ EntityQuery, Quoted }
import `type`.ZIOTypeAlias.DIO
import error.DBFailure
import db.DataSource

import zio.ZIO

object Repository {
  type Env = DataSource

  trait Accessible[T, Id] {
    def getDataById(id: Id): ZIO[DataSource with Service[T, Id], DBFailure, Option[T]] =
      ZIO.serviceWithZIO[Service[T, Id]](_.getDataById(id))

    def insertData(data: T): ZIO[DataSource with Service[T, Id], DBFailure, T] =
      ZIO.serviceWithZIO[Service[T, Id]](_.insertData(data))

    def updateData(data: T): ZIO[DataSource with Service[T, Id], DBFailure, T] =
      ZIO.serviceWithZIO[Service[T, Id]](_.updateData(data))

    def getAllData: ZIO[DataSource with Service[T, Id], DBFailure, List[T]] =
      ZIO.serviceWithZIO[Service[T, Id]](_.getAllData)

    def getFilteredData[B](
                                   func: T => B,
                                   param: B,
                                   compare: (B, B) => Boolean,
                                 ): ZIO[DataSource with Service[T, Id], DBFailure, List[T]] =
      ZIO.serviceWithZIO[Service[T, Id]](_.getFilteredData(func, param, compare))
  }

  trait Service[T, Id] {
    protected val dc: db.Ctx.type = db.Ctx
    import dc._
    protected val getId: T => Id

    val schema: Quoted[EntityQuery[T]]

    def getDataById(id: Id): DIO[Option[T]] =
      dc.run {
        quote {
          schema.filter(el => getId(el) == lift(id)).take(1)
        }
      }.mapBoth(er => DBFailure(er), _.headOption)

    def insertData(data: T): DIO[T] =
      dc.run {
        quote {
          schema.insertValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    def updateData(data: T): DIO[T] =
      dc.run {
        quote {
          schema.filter(el => getId(el) == lift(getId(data))).updateValue(lift(data))
        }
      }.mapBoth(er => DBFailure(er), _ => data)

    def getAllData: DIO[List[T]] =
      dc.run {
        quote {
          schema
        }
      }.mapBoth(er => DBFailure(er), _.toList)

    def getFilteredData[B](
        func: T => B,
        param: B,
        compare: (B, B) => Boolean,
      ): DIO[List[T]] =
      dc.run {
        quote {
          schema.filter(el => compare(func(el), lift(param)))
        }
      }.mapBoth(er => DBFailure(er), _.toList)
  }
}
