/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.Copyable
import kotlin.native.concurrent.*

/**
 * A description of how month names are formatted.
 */
public class MonthNames(
    /**
     * A list of month names, in order from January to December.
     */
    public val names: List<String>
) {
    init {
        require(names.size == 12) { "Month names must contain exactly 12 elements" }
    }

    /**
     * Create a [MonthNames], accepting the month names in order from January to December.
     */
    public constructor(
        january: String, february: String, march: String, april: String, may: String, june: String,
        july: String, august: String, september: String, october: String, november: String, december: String
    ) :
        this(listOf(january, february, march, april, may, june, july, august, september, october, november, december))

    public companion object {
        /**
         * English month names, 'January' to 'December'.
         */
        public val ENGLISH_FULL: MonthNames = MonthNames(
            listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
        )

        /**
         * Shortened English month names, 'Jan' to 'Dec'.
         */
        public val ENGLISH_ABBREVIATED: MonthNames = MonthNames(
            listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
        )
    }
}

internal fun MonthNames.toKotlinCode(): String = when (this.names) {
    MonthNames.ENGLISH_FULL.names -> "MonthNames.${DayOfWeekNames.Companion::ENGLISH_FULL.name}"
    MonthNames.ENGLISH_ABBREVIATED.names -> "MonthNames.${DayOfWeekNames.Companion::ENGLISH_ABBREVIATED.name}"
    else -> names.joinToString(", ", "MonthNames(", ")", transform = String::toKotlinCode)
}

/**
 * A description of how day of week names are formatted.
 */
public class DayOfWeekNames(
    /**
     * A list of day of week names, in order from Monday to Sunday.
     */
    public val names: List<String>
) {
    init {
        require(names.size == 7) { "Day of week names must contain exactly 7 elements" }
    }

    /**
     * A constructor that takes a list of day of week names, in order from Monday to Sunday.
     */
    public constructor(
        monday: String,
        tuesday: String,
        wednesday: String,
        thursday: String,
        friday: String,
        saturday: String,
        sunday: String
    ) :
        this(listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday))

    public companion object {
        /**
         * English day of week names, 'Monday' to 'Sunday'.
         */
        public val ENGLISH_FULL: DayOfWeekNames = DayOfWeekNames(
            listOf(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
            )
        )

        /**
         * Shortened English day of week names, 'Mon' to 'Sun'.
         */
        public val ENGLISH_ABBREVIATED: DayOfWeekNames = DayOfWeekNames(
            listOf(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )
        )
    }
}

internal fun DayOfWeekNames.toKotlinCode(): String = when (this.names) {
    DayOfWeekNames.ENGLISH_FULL.names -> "DayOfWeekNames.${DayOfWeekNames.Companion::ENGLISH_FULL.name}"
    DayOfWeekNames.ENGLISH_ABBREVIATED.names -> "DayOfWeekNames.${DayOfWeekNames.Companion::ENGLISH_ABBREVIATED.name}"
    else -> names.joinToString(", ", "DayOfWeekNames(", ")", transform = String::toKotlinCode)
}

internal fun <T> getParsedField(field: T?, name: String): T {
    if (field == null) {
        throw DateTimeFormatException("Can not create a $name from the given input: the field $name is missing")
    }
    return field
}

internal interface DateFieldContainer {
    var year: Int?
    var monthNumber: Int?
    var dayOfMonth: Int?
    var isoDayOfWeek: Int?
}

internal object DateFields {
    val year = SignedFieldSpec(DateFieldContainer::year, maxAbsoluteValue = null)
    val month = UnsignedFieldSpec(DateFieldContainer::monthNumber, minValue = 1, maxValue = 12)
    val dayOfMonth = UnsignedFieldSpec(DateFieldContainer::dayOfMonth, minValue = 1, maxValue = 31)
    val isoDayOfWeek = UnsignedFieldSpec(DateFieldContainer::isoDayOfWeek, minValue = 1, maxValue = 7)
}

/**
 * A [kotlinx.datetime.LocalDate], but potentially incomplete and inconsistent.
 */
internal class IncompleteLocalDate(
    override var year: Int? = null,
    override var monthNumber: Int? = null,
    override var dayOfMonth: Int? = null,
    override var isoDayOfWeek: Int? = null
) : DateFieldContainer, Copyable<IncompleteLocalDate> {
    fun toLocalDate(): LocalDate {
        val date = LocalDate(
            getParsedField(year, "year"),
            getParsedField(monthNumber, "monthNumber"),
            getParsedField(dayOfMonth, "dayOfMonth")
        )
        isoDayOfWeek?.let {
            if (it != date.dayOfWeek.isoDayNumber) {
                throw DateTimeFormatException(
                    "Can not create a LocalDate from the given input: " +
                        "the day of week is ${DayOfWeek(it)} but the date is $date, which is a ${date.dayOfWeek}"
                )
            }
        }
        return date
    }

    fun populateFrom(date: LocalDate) {
        year = date.year
        monthNumber = date.monthNumber
        dayOfMonth = date.dayOfMonth
        isoDayOfWeek = date.dayOfWeek.isoDayNumber
    }

    override fun copy(): IncompleteLocalDate = IncompleteLocalDate(year, monthNumber, dayOfMonth, isoDayOfWeek)

    override fun equals(other: Any?): Boolean =
        other is IncompleteLocalDate && year == other.year && monthNumber == other.monthNumber &&
            dayOfMonth == other.dayOfMonth && isoDayOfWeek == other.isoDayOfWeek

    override fun hashCode(): Int =
        year.hashCode() * 31 + monthNumber.hashCode() * 31 + dayOfMonth.hashCode() * 31 + isoDayOfWeek.hashCode() * 31

    override fun toString(): String =
        "${year ?: "??"}-${monthNumber ?: "??"}-${dayOfMonth ?: "??"} (day of week is ${isoDayOfWeek ?: "??"})"
}

internal class YearDirective(private val padding: Padding) :
    SignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.year,
        minDigits = padding.minDigits(4),
        maxDigits = null,
        spacePadding = padding.spaces(4),
        outputPlusOnExceededWidth = 4,
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::year.name}()"
        else -> "${DateTimeFormatBuilder.WithDate::year.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is YearDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class ReducedYearDirective(val base: Int) :
    ReducedIntFieldDirective<DateFieldContainer>(
        DateFields.year,
        digits = 2,
        base = base,
    ) {
    override val builderRepresentation: String get() = "${DateTimeFormatBuilder.WithDate::yearTwoDigits.name}($base)"

    override fun equals(other: Any?): Boolean = other is ReducedYearDirective && base == other.base
    override fun hashCode(): Int = base.hashCode()
}

internal class MonthDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.month,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::monthNumber.name}()"
        else -> "${DateTimeFormatBuilder.WithDate::monthNumber.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is MonthDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class MonthNameDirective(private val names: MonthNames) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.month, names.names) {
    override val builderRepresentation: String get() =
        "${DateTimeFormatBuilder.WithDate::monthName.name}(${names.toKotlinCode()})"

    override fun equals(other: Any?): Boolean = other is MonthNameDirective && names.names == other.names.names
    override fun hashCode(): Int = names.names.hashCode()
}

internal class DayDirective(private val padding: Padding) :
    UnsignedIntFieldFormatDirective<DateFieldContainer>(
        DateFields.dayOfMonth,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2),
    ) {
    override val builderRepresentation: String get() = when (padding) {
        Padding.ZERO -> "${DateTimeFormatBuilder.WithDate::dayOfMonth.name}()"
        else -> "${DateTimeFormatBuilder.WithDate::dayOfMonth.name}(${padding.toKotlinCode()})"
    }

    override fun equals(other: Any?): Boolean = other is DayDirective && padding == other.padding
    override fun hashCode(): Int = padding.hashCode()
}

internal class DayOfWeekDirective(private val names: DayOfWeekNames) :
    NamedUnsignedIntFieldFormatDirective<DateFieldContainer>(DateFields.isoDayOfWeek, names.names) {

    override val builderRepresentation: String get() =
        "${DateTimeFormatBuilder.WithDate::dayOfWeek.name}(${names.toKotlinCode()})"

    override fun equals(other: Any?): Boolean = other is DayOfWeekDirective && names.names == other.names.names
    override fun hashCode(): Int = names.names.hashCode()
}

internal class LocalDateFormat(override val actualFormat: StringFormat<DateFieldContainer>) :
    AbstractDateTimeFormat<LocalDate, IncompleteLocalDate>() {
    override fun intermediateFromValue(value: LocalDate): IncompleteLocalDate =
        IncompleteLocalDate().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteLocalDate): LocalDate = intermediate.toLocalDate()

    override val emptyIntermediate get() = emptyIncompleteLocalDate

    companion object {
        fun build(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate> {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return LocalDateFormat(builder.build())
        }
    }

    internal class Builder(override val actualBuilder: AppendableFormatStructure<DateFieldContainer>) :
        AbstractDateTimeFormatBuilder<DateFieldContainer, Builder>, DateTimeFormatBuilder.WithDate {
        override fun year(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(YearDirective(padding)))

        override fun yearTwoDigits(baseYear: Int) =
            actualBuilder.add(BasicFormatStructure(ReducedYearDirective(baseYear)))

        override fun monthNumber(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(MonthDirective(padding)))

        override fun monthName(names: MonthNames) =
            actualBuilder.add(BasicFormatStructure(MonthNameDirective(names)))

        override fun dayOfMonth(padding: Padding) = actualBuilder.add(BasicFormatStructure(DayDirective(padding)))
        override fun dayOfWeek(names: DayOfWeekNames) =
            actualBuilder.add(BasicFormatStructure(DayOfWeekDirective(names)))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun date(format: DateTimeFormat<LocalDate>) = when (format) {
            is LocalDateFormat -> actualBuilder.add(format.actualFormat.directives)
        }

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
    }
}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_DATE by lazy {
    LocalDateFormat.build { year(); char('-'); monthNumber(); char('-'); dayOfMonth() }
}
@SharedImmutable
internal val ISO_DATE_BASIC by lazy {
    LocalDateFormat.build { year(); monthNumber(); dayOfMonth() }
}

@SharedImmutable
private val emptyIncompleteLocalDate = IncompleteLocalDate()