package ru.otus
package `type`

import enumeratum.{ EnumEntry, _ }
import io.getquill.MappedEncoding

sealed trait AppConstant extends EnumEntry {
  def defaultValue: Any
  def serialize: Any => String
  def deserialize: String => Any
}

object AppConstant extends Enum[AppConstant] {
  case object MaxTimeToConfirm extends AppConstant {
    override def defaultValue: Long          = 18000L
    override def serialize: Any => String    = data => data.toString
    override def deserialize: String => Long = str => str.toLong
  }

  override def values: IndexedSeq[AppConstant] = findValues

  implicit val encodeAppConstant: MappedEncoding[AppConstant, String] =
    MappedEncoding[AppConstant, String](_.entryName)

  implicit val decodeAppConstant: MappedEncoding[String, AppConstant] =
    MappedEncoding[String, AppConstant](AppConstant.withName)
}
