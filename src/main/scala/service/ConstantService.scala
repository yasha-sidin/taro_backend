package ru.otus
package service

import repository.{ConstantRepository, Repository}
import dao.Constant
import error.{ExpectedFailure, NotFoundFailure}
import `type`.AppConstant

import zio.{ULayer, ZIO, ZLayer}
import zio.macros.accessible

@accessible
object ConstantService {
  private type ConstantService    = Service
  private type ConstantServiceEnv = Repository.Env with ConstantRepository.Service

  trait Service {
    def initConstants(): ZIO[ConstantServiceEnv, ExpectedFailure, List[Constant]]
    def getConstantValueByKey[T](key: AppConstant): ZIO[ConstantServiceEnv, ExpectedFailure, T]
    def updateConstant(constant: Constant): ZIO[ConstantServiceEnv, ExpectedFailure, Constant]
  }

  class ServiceImpl extends Service {
    override def initConstants(): ZIO[ConstantServiceEnv, ExpectedFailure, List[Constant]] =
      for {
        constants <- ZIO.succeed(AppConstant.values)
        res <- ZIO.foreach(constants)(value => {
          ConstantRepository.insertConstant(Constant(value, value.defaultValue)(value.serialize))
        })
      } yield res.toList

    override def updateConstant(constant: Constant): ZIO[ConstantServiceEnv, ExpectedFailure, Constant] =
      ConstantRepository.updateConstant(constant)

    override def getConstantValueByKey[T](key: AppConstant): ZIO[ConstantServiceEnv, ExpectedFailure, T] =
      for {
        constant <- ConstantRepository.getConstantByKey(key).some.orElseFail(NotFoundFailure("Constant not found."))
        value <- ZIO.succeed(Constant.toValue[T](constant)(key.deserialize.asInstanceOf[String => T]))
      } yield value
  }

  val live: ULayer[ConstantService] = ZLayer.succeed(new ServiceImpl)
}
