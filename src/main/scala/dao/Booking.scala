package ru.otus
package dao

import `type`.BookingStatus

import java.time.Instant
import java.util.UUID

case class Booking(
    id: UUID,
    userId: UUID,
    dateId: UUID,
    status: BookingStatus,
    canReturn: Boolean,
    timeToConfirm: Instant,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val isDeleted: Boolean = false,
  ) extends Model(isDeleted, createdAt, updatedAt) { self =>
  def isActive: Boolean    = self.status == BookingStatus.Active
  def isCancelled: Boolean = self.status == BookingStatus.Cancelled
  def isCompleted: Boolean = self.status == BookingStatus.Completed
  def isConfirmed: Boolean = self.status == BookingStatus.Confirmed
  def canConfirm(now: Instant): Boolean = self.timeToConfirm.isBefore(now)
}
