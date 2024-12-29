package ru.otus

package object error {
  sealed abstract class ExpectedFailure(val message: String) extends Exception

  final case class DBFailure(throwable: Throwable, override val message: String) extends ExpectedFailure(message)
  final case class NotFoundFailure(override val message: String)                 extends ExpectedFailure(message)
  final case class ValidationFailure(override val message: String)               extends ExpectedFailure(message)
  final case class AppointmentIsPastFailure(override val message: String)        extends ExpectedFailure(message)
  final case class InvalidDateRangeFailure(override val message: String)         extends ExpectedFailure(message)
  final case class InvalidDeadlineFailure(override val message: String)          extends ExpectedFailure(message)
  final case class NotAvailableFailure(override val message: String)             extends ExpectedFailure(message)
  final case class DateIsBookedAndConfirmedFailure(override val message: String) extends ExpectedFailure(message)
  final case class PeriodIsAlreadyTakenFailure(override val message: String)     extends ExpectedFailure(message)
  final case class ConfirmationNotAvailableFailure(override val message: String) extends ExpectedFailure(message)
  final case class MigrationFailure(override val message: String)                extends ExpectedFailure(message)
  final case class ConfigFailure(override val message: String)                   extends ExpectedFailure(message)
  final case class ServerRunningError(override val message: String)              extends ExpectedFailure(message)
}
