package ru.otus
package `type`

import enumeratum.{ EnumEntry, _ }
import io.getquill.MappedEncoding
import zio.json.{ JsonDecoder, JsonEncoder }

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

  case object AboutMe extends AppConstant {
    override def defaultValue: String = "Обо мне"
    override def serialize: Any => String = data => data.toString
    override def deserialize: String => String = str => str
  }

  case object MyContacts extends AppConstant {
    override def defaultValue: String = "@yashaphrh333"
    override def serialize: Any => String = data => data.toString
    override def deserialize: String => String = str => str
  }

  override def values: IndexedSeq[AppConstant] = findValues

  implicit val encodeAppConstant: MappedEncoding[AppConstant, String] =
    MappedEncoding[AppConstant, String](_.entryName)

  implicit val decodeAppConstant: MappedEncoding[String, AppConstant] =
    MappedEncoding[String, AppConstant](AppConstant.withName)

  implicit val encoder: JsonEncoder[AppConstant] = JsonEncoder[String].contramap(_.entryName)
  implicit val decoder: JsonDecoder[AppConstant] = JsonDecoder[String].mapOrFail { str =>
    AppConstant.withNameOption(str).toRight(s"Invalid AppointmentDateStatus: $str")
  }
}
