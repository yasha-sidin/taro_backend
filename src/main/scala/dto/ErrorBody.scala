package ru.otus
package dto

import zio.json.{SnakeCase, jsonDerive, jsonMemberNames}

@jsonDerive
@jsonMemberNames(SnakeCase)
case class ErrorBody(code: Int, message: String)
