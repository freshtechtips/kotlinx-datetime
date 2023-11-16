/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

/**
 * Common functions for all format builders.
 */
public sealed interface DateTimeFormatBuilder {
    /**
     * A literal string.
     *
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input verbatim.
     */
    public fun chars(value: String)

    /**
     * Functions specific to the date-time format builders containing the local-date fields.
     */
    public sealed interface WithDate : DateTimeFormatBuilder {
        /**
         * A year number.
         *
         * By default, for years [-9999..9999], it's formatted as a decimal number, zero-padded to four digits, though
         * this padding can be disabled or changed to space padding by passing [padding].
         * For years outside this range, it's formatted as a decimal number with a leading sign, so the year 12345
         * is formatted as "+12345".
         */
        public fun year(padding: Padding = Padding.ZERO)

        /**
         * The last two digits of the ISO year.
         *
         * [baseYear] is the base year for the two-digit year.
         * For example, if [baseYear] is 1960, then this format correctly works with years [1960..2059].
         *
         * On formatting, when given a year in the valid range, it returns the last two digits of the year,
         * so 1993 becomes "93". When given a year outside the valid range, it returns the full year number
         * with a leading sign, so 1850 becomes "+1850", and -200 becomes "-200".
         *
         * On parsing, it accepts either a two-digit year or a full year number with a leading sign.
         * When given a two-digit year, it returns a year in the valid range, so "93" becomes 1993,
         * and when given a full year number with a leading sign, it parses the full year number,
         * so "+1850" becomes 1850.
         */
        public fun yearTwoDigits(baseYear: Int)

        /**
         * A month-of-year number, from 1 to 12.
         */
        public fun monthNumber(padding: Padding = Padding.ZERO)

        /**
         * A month name (for example, "January").
         *
         * Example:
         * ```
         * monthName(MonthNames.ENGLISH_FULL)
         * ```
         */
        public fun monthName(names: MonthNames)

        /**
         * A day-of-month number, from 1 to 31.
         *
         * By default, it's padded with zeros to two digits. This can be changed by passing [padding].
         */
        public fun dayOfMonth(padding: Padding = Padding.ZERO)

        /**
         * A day-of-week name (for example, "Thursday").
         *
         * Example:
         * ```
         * dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
         * ```
         */
        public fun dayOfWeek(names: DayOfWeekNames)

        /**
         * An existing [DateTimeFormat] for the date part.
         *
         * Example:
         * ```
         * date(LocalDate.Formats.ISO)
         * ```
         */
        public fun date(format: DateTimeFormat<LocalDate>)
    }

    /**
     * Functions specific to the date-time format builders containing the local-time fields.
     */
    public sealed interface WithTime : DateTimeFormatBuilder {
        /**
         * The hour of the day, from 0 to 23.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         */
        public fun hour(padding: Padding = Padding.ZERO)

        /**
         * The number of hours in the 12-hour clock:
         *
         * * Midnight is 12,
         * * Hours 1-11 are 1-11,
         * * Noon is 12,
         * * Hours 13-23 are 1-11.
         *
         * To disambiguate between the first and the second halves of the day, [amPmMarker] should be used.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * @see [amPmMarker]
         */
        public fun amPmHour(padding: Padding = Padding.ZERO)

        /**
         * The AM/PM marker, using the specified strings.
         *
         * [am] is used for the AM marker (0-11 hours), [pm] is used for the PM marker (12-23 hours).
         *
         * @see [amPmHour]
         */
        public fun amPmMarker(am: String, pm: String)

        /**
         * The number of minutes.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         */
        public fun minute(padding: Padding = Padding.ZERO)

        /**
         * The number of seconds.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun second(padding: Padding = Padding.ZERO)

        /**
         * The fractional part of the second without the leading dot.
         *
         * When formatting, the decimal fraction will round the number to fit in the specified [maxLength] and will add
         * trailing zeroes to the specified [minLength].
         *
         * When parsing, the parser will require that the fraction is at least [minLength] and at most [maxLength]
         * digits long.
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         *
         * @throws IllegalArgumentException if [minLength] is greater than [maxLength] or if either is not in the range 1..9.
         */
        public fun secondFraction(minLength: Int = 1, maxLength: Int = 9)

        /**
         * The fractional part of the second without the leading dot.
         *
         * When formatting, the decimal fraction will add trailing zeroes or round as necessary to always output
         * exactly the number of digits specified in [fixedLength].
         *
         * When parsing, exactly [fixedLength] digits will be consumed.
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         *
         * @throws IllegalArgumentException if [fixedLength] is not in the range 1..9.
         */
        public fun secondFraction(fixedLength: Int) {
            secondFraction(fixedLength, fixedLength)
        }

        /**
         * An existing [DateTimeFormat] for the time part.
         *
         * Example:
         * ```
         * time(LocalTime.Formats.ISO)
         * ```
         */
        public fun time(format: DateTimeFormat<LocalTime>)
    }

    /**
     * Functions specific to the date-time format builders containing the local-date and local-time fields.
     */
    public sealed interface WithDateTime : WithDate, WithTime {
        /**
         * An existing [DateTimeFormat] for the date-time part.
         *
         * Example:
         * ```
         * dateTime(LocalDateTime.Formats.ISO)
         * ```
         */
        public fun dateTime(format: DateTimeFormat<LocalDateTime>)
    }

    /**
     * Functions specific to the date-time format builders containing the UTC-offset fields.
     */
    public sealed interface WithUtcOffset : DateTimeFormatBuilder {
        /**
         * The total number of hours in the UTC offset, with a sign.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun offsetHours(padding: Padding = Padding.ZERO)

        /**
         * The minute-of-hour of the UTC offset.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun offsetMinutesOfHour(padding: Padding = Padding.ZERO)

        /**
         * The second-of-minute of the UTC offset.
         *
         * By default, it's zero-padded to two digits, but this can be changed with [padding].
         *
         * This field has the default value of 0. If you want to omit it, use [optional].
         */
        public fun offsetSecondsOfMinute(padding: Padding = Padding.ZERO)

        /**
         * An existing [DateTimeFormat] for the UTC offset part.
         *
         * Example:
         * ```
         * offset(UtcOffset.Formats.FOUR_DIGITS)
         * ```
         */
        public fun offset(format: DateTimeFormat<UtcOffset>)
    }

    /**
     * Builder for formats for values that have all the date-time components:
     * date, time, UTC offset, and the timezone ID.
     */
    public sealed interface WithDateTimeComponents : WithDateTime, WithUtcOffset {
        /**
         * The IANA time zone identifier, for example, "Europe/Berlin".
         *
         * When formatting, the timezone identifier is supplied as is, without any validation.
         * On parsing, [TimeZone.availableZoneIds] is used to validate the identifier.
         */
        public fun timeZoneId()

        /**
         * An existing [DateTimeFormat].
         *
         * Example:
         * ```
         * dateTimeComponents(DateTimeComponents.Formats.RFC_1123)
         * ```
         */
        public fun dateTimeComponents(format: DateTimeFormat<DateTimeComponents>)
    }
}

/**
 * The fractional part of the second without the leading dot.
 *
 * When formatting, the decimal fraction will round the number to fit in the specified [maxLength] and will add
 * trailing zeroes to the specified [minLength].
 *
 * Additionally, [grouping] is a list, where the i'th element specifies how many trailing zeros to add during formatting
 * when
 *
 * When parsing, the parser will require that the fraction is at least [minLength] and at most [maxLength]
 * digits long.
 *
 * This field has the default value of 0. If you want to omit it, use [optional].
 *
 * @throws IllegalArgumentException if [minLength] is greater than [maxLength] or if either is not in the range 1..9.
 */
internal fun DateTimeFormatBuilder.WithTime.secondFractionInternal(minLength: Int, maxLength: Int, grouping: List<Int>) {
    @Suppress("NO_ELSE_IN_WHEN")
    when (this) {
        is AbstractWithTimeBuilder -> addFormatStructureForTime(
            BasicFormatStructure(FractionalSecondDirective(minLength, maxLength, grouping))
        )
    }
}

/**
 * A format along with other ways to parse the same portion of the value.
 *
 * When parsing, first, [primaryFormat] is used; if parsing the whole string fails using that, the formats
 * from [alternativeFormats] are tried in order.
 *
 * When formatting, the [primaryFormat] is used to format the value, and [alternativeFormats] are ignored.
 *
 * Example:
 * ```
 * alternativeParsing(
 *   { dayOfMonth(); char('-'); monthNumber() },
 *   { monthNumber(); char(' '); dayOfMonth() },
 * ) { monthNumber(); char('/'); dayOfMonth() }
 * ```
 *
 * This will always format a date as `MM/DD`, but will also accept `DD-MM` and `MM DD`.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: DateTimeFormatBuilder> T.alternativeParsing(
    vararg alternativeFormats: T.() -> Unit,
    primaryFormat: T.() -> Unit
): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> ->
        appendAlternativeParsingImpl(*alternativeFormats as Array<out AbstractDateTimeFormatBuilder<*, *>.() -> Unit>,
            mainFormat = primaryFormat as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit))
    else -> throw IllegalStateException("impossible")
}

/**
 * An optional section.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [optional] calls where all the fields have default values are permitted.
 *
 * Example:
 * ```
 * offsetHours(); char(':'); offsetMinutesOfHour()
 * optional { char(':'); offsetSecondsOfMinute() }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero, so the
 * UTC offset `+18:30:00` gets formatted as `"+18:30"`, but `+18:30:01` becomes `"+18:30:01"`.
 *
 * When parsing, either [format] or, if that fails, the literal [ifZero] are parsed. If the [ifZero] string is parsed,
 * the values in [format] get assigned their default values.
 *
 * [ifZero] defines the string that is used if values are the default ones.
 *
 * @throws IllegalArgumentException if not all fields used in [format] have a default value.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: DateTimeFormatBuilder> T.optional(ifZero: String = "", format: T.() -> Unit): Unit = when (this) {
    is AbstractDateTimeFormatBuilder<*, *> -> appendOptionalImpl(onZero = ifZero, format as (AbstractDateTimeFormatBuilder<*, *>.() -> Unit))
    else -> throw IllegalStateException("impossible")
}

/**
 * A literal character.
 *
 * This is a shorthand for `chars(value.toString())`.
 */
public fun DateTimeFormatBuilder.char(value: Char): Unit = chars(value.toString())

internal interface AbstractDateTimeFormatBuilder<Target, ActualSelf> :
    DateTimeFormatBuilder where ActualSelf : AbstractDateTimeFormatBuilder<Target, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf

    fun appendAlternativeParsingImpl(
        vararg otherFormats: ActualSelf.() -> Unit,
        mainFormat: ActualSelf.() -> Unit
    ) {
        val others = otherFormats.map { block ->
            createEmpty().also { block(it) }.actualBuilder.build()
        }
        val main = createEmpty().also { mainFormat(it) }.actualBuilder.build()
        actualBuilder.add(AlternativesParsingFormatStructure(main, others))
    }

    fun appendOptionalImpl(
        onZero: String,
        format: ActualSelf.() -> Unit
    ) {
        actualBuilder.add(OptionalFormatStructure(onZero, createEmpty().also { format(it) }.actualBuilder.build()))
    }

    override fun chars(value: String) = actualBuilder.add(ConstantFormatStructure(value))

    fun withSharedSignImpl(outputPlus: Boolean, block: ActualSelf.() -> Unit) {
        actualBuilder.add(
            SignedFormatStructure(
                createEmpty().also { block(it) }.actualBuilder.build(),
                outputPlus
            )
        )
    }

    fun build(): StringFormat<Target> = StringFormat(actualBuilder.build())
}

internal inline fun<T> StringFormat<T>.builderString(constants: List<Pair<String, StringFormat<*>>>): String =
    directives.builderString(constants)

private fun<T> FormatStructure<T>.builderString(constants: List<Pair<String, StringFormat<*>>>): String = when (this) {
    is BasicFormatStructure -> directive.builderRepresentation
    is ConstantFormatStructure -> if (string.length == 1) {
        "${DateTimeFormatBuilder::char.name}(${string[0].toKotlinCode()})"
    } else {
        "${DateTimeFormatBuilder::chars.name}(${string.toKotlinCode()})"
    }
    is SignedFormatStructure -> {
        if (format is BasicFormatStructure && format.directive is UtcOffsetWholeHoursDirective) {
            format.directive.builderRepresentation
        } else {
            buildString {
                if (withPlusSign) appendLine("withSharedSign(outputPlus = true) {")
                else appendLine("withSharedSign {")
                appendLine(format.builderString(constants).prependIndent(CODE_INDENT))
                append("}")
            }
        }
    }
    is OptionalFormatStructure -> buildString {
        if (onZero == "") {
            appendLine("${DateTimeFormatBuilder::optional.name} {")
        } else {
            appendLine("${DateTimeFormatBuilder::optional.name}(${onZero.toKotlinCode()}) {")
        }
        val subformat = format.builderString(constants)
        if (subformat.isNotEmpty()) {
            appendLine(subformat.prependIndent(CODE_INDENT))
        }
        append("}")
    }
    is AlternativesParsingFormatStructure -> buildString {
        append("${DateTimeFormatBuilder::alternativeParsing.name}(")
        for (alternative in formats) {
            appendLine("{")
            val subformat = alternative.builderString(constants)
            if (subformat.isNotEmpty()) {
                appendLine(subformat.prependIndent(CODE_INDENT))
            }
            append("}, ")
        }
        if (this[length - 2] == ',') {
            repeat(2) {
                deleteAt(length - 1)
            }
        }
        appendLine(") {")
        appendLine(mainFormat.builderString(constants).prependIndent(CODE_INDENT))
        append("}")
    }
    is ConcatenatedFormatStructure -> buildString {
        if (formats.isNotEmpty()) {
            var index = 0
            loop@while (index < formats.size) {
                searchConstant@for (constant in constants) {
                    val constantDirectives = constant.second.directives.formats
                    if (formats.size - index >= constantDirectives.size) {
                        for (i in constantDirectives.indices) {
                            if (formats[index + i] != constantDirectives[i]) {
                                continue@searchConstant
                            }
                        }
                        append(constant.first)
                        index += constantDirectives.size
                        continue@loop
                    }
                }
                if (index == formats.size - 1) {
                    append(formats.last().builderString(constants))
                } else {
                    appendLine(formats[index].builderString(constants))
                }
                ++index
            }
        }
    }
}

private const val CODE_INDENT = "    "
