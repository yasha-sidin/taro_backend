package ru.otus

package object error {
  sealed abstract class ExpectedFailure extends Exception

  final case class DBFailure(throwable: Throwable)                  extends ExpectedFailure
  final case class NotFoundFailure(message: String)                 extends ExpectedFailure
  final case class ValidationFailure(message: String)               extends ExpectedFailure
  final case class AppointmentIsPastFailure(message: String)        extends ExpectedFailure
  final case class InvalidDateRangeFailure(message: String)         extends ExpectedFailure
  final case class InvalidDeadlineFailure(message: String)          extends ExpectedFailure
  final case class NotAvailableFailure(message: String)             extends ExpectedFailure
  final case class DateIsBookedAndConfirmedFailure(message: String) extends ExpectedFailure
  final case class PeriodIsAlreadyTakenFailure(message: String)     extends ExpectedFailure
  final case class ConfirmationNotAvailableFailure(message: String) extends ExpectedFailure
  final case class MigrationFailure(message: String)                extends ExpectedFailure
  final case class ConfigFailure(message: String)                   extends ExpectedFailure
}
