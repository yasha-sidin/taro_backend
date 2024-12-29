package ru.otus
package dao

import `type`.AppConstant

import zio.json.{SnakeCase, jsonDerive, jsonMemberNames}

@jsonDerive
@jsonMemberNames(SnakeCase)
case class Constant(key: AppConstant, value: String)

object Constant {
  def apply[T](key: AppConstant, value: T)(serialize: T => String): Constant =
    Constant(key, serialize(value))

  def toValue[T](constant: Constant)(deserialize: String => T): T =
    deserialize(constant.value)
}
