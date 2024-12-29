package ru.otus
package `type`

import enumeratum.{EnumEntry, _}
import io.getquill.MappedEncoding
import zio.json.{JsonDecoder, JsonEncoder}

sealed trait AppointmentDateStatus extends EnumEntry

object AppointmentDateStatus extends Enum[AppointmentDateStatus] {
  case object Available extends AppointmentDateStatus
  case object Booked    extends AppointmentDateStatus
  case object Expired   extends AppointmentDateStatus

  override def values: IndexedSeq[AppointmentDateStatus] = findValues

  implicit val encodeDateStatus: MappedEncoding[AppointmentDateStatus, String] =
    MappedEncoding[AppointmentDateStatus, String](_.entryName)

  implicit val decodeDateStatus: MappedEncoding[String, AppointmentDateStatus] =
    MappedEncoding[String, AppointmentDateStatus](AppointmentDateStatus.withName)

  implicit val encoder: JsonEncoder[AppointmentDateStatus] = JsonEncoder[String].contramap(_.entryName)
  implicit val decoder: JsonDecoder[AppointmentDateStatus] = JsonDecoder[String].mapOrFail { str =>
    AppointmentDateStatus.withNameOption(str).toRight(s"Invalid AppointmentDateStatus: $str")
  }
}
