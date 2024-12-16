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
    createdAt: Instant,
    updatedAt: Instant,
  ) { self =>
  def isAvailable: Boolean             = self.status == AppointmentDateStatus.Available
  def isExpired(now: Instant): Boolean = self.bookingDeadline.isBefore(now)
}