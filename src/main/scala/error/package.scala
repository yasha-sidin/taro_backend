package ru.otus

package object error {
  sealed abstract class ExpectedFailure                 extends Exception
  final case class DBFailure(throwable: Throwable)      extends ExpectedFailure
  final case class NotFoundFailure(message: String)     extends ExpectedFailure
  final case class ValidationFailure(message: String)   extends ExpectedFailure
  final case class NotAvailableFailure(message: String) extends ExpectedFailure
}
