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
    createdAt: Instant,
    updatedAt: Instant,
    canReturn: Boolean,
    timeToConfirm: Instant,
  ) { self =>
  def isActive: Boolean    = self.status == BookingStatus.Active
  def isCancelled: Boolean = self.status == BookingStatus.Cancelled
  def isCompleted: Boolean = self.status == BookingStatus.Completed
  def isConfirmed: Boolean = self.status == BookingStatus.Confirmed
}
