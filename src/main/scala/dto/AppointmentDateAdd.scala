package ru.otus
package dto

import zio.json.{ SnakeCase, jsonDerive, jsonMemberNames }

import java.time.Instant

@jsonDerive
@jsonMemberNames(SnakeCase)
case class AppointmentDateAdd(
    dateFrom: Instant,
    dateTo: Instant,
    bookingDeadline: Instant,
  )
