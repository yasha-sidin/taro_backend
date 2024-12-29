package ru.otus
package dto

import zio.json.{ SnakeCase, jsonDerive, jsonMemberNames }

@jsonDerive
@jsonMemberNames(SnakeCase)
case class TelegramDetails(
    chatId: Long,
    firstName: String,
    lastName: Option[String],
    username: Option[String],
    languageCode: Option[String],
  )
