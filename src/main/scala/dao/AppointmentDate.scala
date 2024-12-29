package ru.otus
package dao

import `type`.AppointmentDateStatus

import zio.json.{SnakeCase, jsonDerive, jsonMemberNames}

import java.time.Instant
import java.util.UUID

@jsonDerive
@jsonMemberNames(SnakeCase)
case class AppointmentDate(
    id: UUID,
    dateFrom: Instant,
    dateTo: Instant,
    status: AppointmentDateStatus,
    bookingDeadline: Instant,
    override val createdAt: Instant,
    override val updatedAt: Instant,
  ) extends Model(createdAt, updatedAt) { self =>
  def isAvailable: Boolean             = self.status == AppointmentDateStatus.Available
  def isExpired(now: Instant): Boolean = self.bookingDeadline.isBefore(now)
  def canBook(now: Instant): Boolean =
    self.isAvailable && !self.isExpired(now)
}
