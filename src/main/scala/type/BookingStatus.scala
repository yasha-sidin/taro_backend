package ru.otus
package `type`

import enumeratum.{EnumEntry, _}
import io.getquill.MappedEncoding
import zio.json.{JsonDecoder, JsonEncoder}

sealed trait BookingStatus extends EnumEntry

object BookingStatus extends Enum[BookingStatus] {
  case object Active    extends BookingStatus
  case object Completed extends BookingStatus
  case object Cancelled extends BookingStatus
  case object Confirmed extends BookingStatus

  override def values: IndexedSeq[BookingStatus] = findValues

  implicit val encodeBookingStatus: MappedEncoding[BookingStatus, String] =
    MappedEncoding[BookingStatus, String](_.entryName)

  implicit val decodeBookingStatus: MappedEncoding[String, BookingStatus] =
    MappedEncoding[String, BookingStatus](BookingStatus.withName)

  implicit val encoder: JsonEncoder[BookingStatus] = JsonEncoder[String].contramap(_.entryName)
  implicit val decoder: JsonDecoder[BookingStatus] = JsonDecoder[String].mapOrFail { str =>
    BookingStatus.withNameOption(str).toRight(s"Invalid AppointmentDateStatus: $str")
  }
}
