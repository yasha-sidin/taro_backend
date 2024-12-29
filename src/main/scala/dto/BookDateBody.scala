package ru.otus
package dto

import zio.json.{SnakeCase, jsonDerive, jsonMemberNames}

import java.util.UUID

@jsonDerive
@jsonMemberNames(SnakeCase)
case class BookDateBody(
    dateId: UUID,
    telegramDetails: TelegramDetails,
  )
