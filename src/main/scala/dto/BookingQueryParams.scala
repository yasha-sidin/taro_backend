package ru.otus
package dto

import `type`.BookingStatus

import zio.json.{SnakeCase, jsonDerive, jsonMemberNames}

@jsonDerive
@jsonMemberNames(SnakeCase)
case class BookingQueryParams(
    status: Option[BookingStatus],
    chatId: Option[Long],
  )
