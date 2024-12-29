package ru.otus
package service

import repository.ConstantRepository
import dao.Constant
import error.{ExpectedFailure, NotFoundFailure}
import `type`.AppConstant

import zio.{ULayer, URIO, ZIO, ZLayer}
import zio.macros.accessible

@accessible
object ConstantService {
  private type ConstantService    = Service
  private type ConstantServiceEnv = db.DataSource with ConstantRepository.Service

  trait Service {
    def initConstants(): URIO[ConstantServiceEnv, Unit]
    def getConstantValueByKey[T](key: AppConstant): ZIO[ConstantServiceEnv, ExpectedFailure, T]
    def getConstantByKey(key: AppConstant): ZIO[ConstantServiceEnv, ExpectedFailure, Constant]
    def getConstants: ZIO[ConstantServiceEnv, ExpectedFailure, List[Constant]]
    def updateConstant(constant: Constant): ZIO[ConstantServiceEnv, ExpectedFailure, Constant]
  }

  class ServiceImpl extends Service {
    override def initConstants(): URIO[ConstantServiceEnv, Unit] =
      for {
        constants <- ZIO.succeed(AppConstant.values)
        _ <- ZIO.foreachDiscard(constants)(value => {
          ConstantRepository.insertConstant(Constant(value, value.defaultValue)(value.serialize)).catchAll(er => ZIO.logError(er.message))
        })
      } yield ()

    override def updateConstant(constant: Constant): ZIO[ConstantServiceEnv, ExpectedFailure, Constant] =
      ConstantRepository.updateConstant(constant)

    override def getConstantValueByKey[T](key: AppConstant): ZIO[ConstantServiceEnv, ExpectedFailure, T] =
      for {
        constant <- ConstantRepository.getConstantByKey(key).some.orElseFail(NotFoundFailure("Constant not found."))
        value <- ZIO.succeed(Constant.toValue[T](constant)(key.deserialize.asInstanceOf[String => T]))
      } yield value

    override def getConstants: ZIO[ConstantServiceEnv, ExpectedFailure, List[Constant]] =
      ConstantRepository.getConstants

    override def getConstantByKey(key: AppConstant): ZIO[ConstantServiceEnv, ExpectedFailure, Constant] =
      ConstantRepository.getConstantByKey(key).some.orElseFail(NotFoundFailure(s"Key $key not found"))
  }

  val live: ULayer[ConstantService] = ZLayer.succeed(new ServiceImpl)
}
