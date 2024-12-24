package ru.otus
package `type`

import enumeratum.{ EnumEntry, _ }
import io.getquill.MappedEncoding

sealed trait TokenType extends EnumEntry

object TokenType extends Enum[TokenType] {
  case object Refresh extends TokenType
  case object Access  extends TokenType

  override def values: IndexedSeq[TokenType] = findValues

  implicit val encodeTokenType: MappedEncoding[TokenType, String] =
    MappedEncoding[TokenType, String](_.entryName)

  implicit val decodeTokenType: MappedEncoding[String, TokenType] =
    MappedEncoding[String, TokenType](TokenType.withName)
}
