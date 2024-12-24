package ru.otus
package dao

import `type`.AppointmentDateStatus

import java.time.Instant
import java.util.UUID

case class AppointmentDate(
    id: UUID,
    dateFrom: Instant,
    dateTo: Instant,
    status: AppointmentDateStatus,
    bookingDeadline: Instant,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val isDeleted: Boolean = false,
  ) extends Model(isDeleted, createdAt, updatedAt) { self =>
  def isAvailable: Boolean             = self.status == AppointmentDateStatus.Available
  def isExpired(now: Instant): Boolean = self.bookingDeadline.isBefore(now)
}
