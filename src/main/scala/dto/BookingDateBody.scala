package ru.otus
package dto

import `type`.BookingStatus
import dao.{ AppointmentDate, Booking }

import zio.json.{ SnakeCase, jsonDerive, jsonMemberNames }

import java.time.Instant
import java.util.UUID

@jsonDerive
@jsonMemberNames(SnakeCase)
case class BookingDateBody(
    id: UUID,
    userId: UUID,
    bookNumber: Long,
    status: BookingStatus,
    canReturn: Boolean,
    timeToConfirm: Instant,
    createdAt: Instant,
    updatedAt: Instant,
    date: AppointmentDate,
  )

object BookingDateBody {
  def fromTuple(tuple: (AppointmentDate, Booking)): BookingDateBody =
    BookingDateBody(
      tuple._2.id,
      tuple._2.userId,
      tuple._2.bookNumber,
      tuple._2.status,
      tuple._2.canReturn,
      tuple._2.timeToConfirm,
      tuple._2.createdAt,
      tuple._2.updatedAt,
      tuple._1,
    )
}
