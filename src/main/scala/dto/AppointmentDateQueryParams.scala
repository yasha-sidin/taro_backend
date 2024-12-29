package ru.otus
package dto

import `type`.AppointmentDateStatus
import zio.json.{ SnakeCase, jsonDerive, jsonMemberNames }

@jsonDerive
@jsonMemberNames(SnakeCase)
case class AppointmentDateQueryParams(
    status: Option[AppointmentDateStatus],
  )
