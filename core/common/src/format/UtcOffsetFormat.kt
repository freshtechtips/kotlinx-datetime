/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlin.math.*
import kotlin.native.concurrent.*

internal interface UtcOffsetFieldContainer {
    var isNegative: Boolean?
    var totalHoursAbs: Int?
    var minutesOfHour: Int?
    var secondsOfMinute: Int?
}

internal class UtcOffsetFormat(val actualFormat: StringFormat<UtcOffsetFieldContainer>) :
    AbstractDateTimeFormat<UtcOffset, IncompleteUtcOffset>(actualFormat) {
    companion object {
        fun build(block: DateTimeFormatBuilder.WithUtcOffset.() -> Unit): UtcOffsetFormat {
            val builder = Builder(AppendableFormatStructure())
            builder.block()
            return UtcOffsetFormat(builder.build())
        }
    }

    private class Builder(override val actualBuilder: AppendableFormatStructure<UtcOffsetFieldContainer>) :
        AbstractDateTimeFormatBuilder<UtcOffsetFieldContainer, Builder>, DateTimeFormatBuilder.WithUtcOffset {

        override fun createEmpty(): Builder = Builder(AppendableFormatStructure())
        override fun appendOffsetTotalHours(padding: Padding) =
            actualBuilder.add(SignedFormatStructure(
                BasicFormatStructure(UtcOffsetWholeHoursDirective(padding)),
                withPlusSign = true
            ))

        override fun appendOffsetMinutesOfHour(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetMinuteOfHourDirective(padding)))

        override fun appendOffsetSecondsOfMinute(padding: Padding) =
            actualBuilder.add(BasicFormatStructure(UtcOffsetSecondOfMinuteDirective(padding)))

        @Suppress("NO_ELSE_IN_WHEN")
        override fun appendOffset(format: DateTimeFormat<UtcOffset>) = when (format) {
            is UtcOffsetFormat -> actualBuilder.add(format.actualFormat.directives)
        }
    }

    override fun intermediateFromValue(value: UtcOffset): IncompleteUtcOffset =
        IncompleteUtcOffset().apply { populateFrom(value) }

    override fun valueFromIntermediate(intermediate: IncompleteUtcOffset): UtcOffset = intermediate.toUtcOffset()

    override fun newIntermediate(): IncompleteUtcOffset = IncompleteUtcOffset()

    override fun toString(): String = actualFormat.builderString()

}

internal enum class WhenToOutput {
    NEVER,
    IF_NONZERO,
    ALWAYS,
}

internal fun DateTimeFormatBuilder.WithUtcOffset.appendIsoOffset(
    zOnZero: Boolean,
    useSeparator: Boolean,
    outputMinute: WhenToOutput,
    outputSecond: WhenToOutput
) {
    require(outputMinute >= outputSecond) { "Seconds cannot be included without minutes" }
    fun DateTimeFormatBuilder.WithUtcOffset.appendIsoOffsetWithoutZOnZero() {
        appendOffsetTotalHours()
        when (outputMinute) {
            WhenToOutput.NEVER -> {}
            WhenToOutput.IF_NONZERO -> {
                optional {
                    if (useSeparator) {
                        char(':')
                    }
                    appendOffsetMinutesOfHour()
                    when (outputSecond) {
                        WhenToOutput.NEVER -> {}
                        WhenToOutput.IF_NONZERO -> {
                            optional {
                                if (useSeparator) {
                                    char(':')
                                }
                                appendOffsetSecondsOfMinute()
                            }
                        }

                        WhenToOutput.ALWAYS -> {
                            if (useSeparator) {
                                char(':')
                            }
                            appendOffsetSecondsOfMinute()
                        }
                    }
                }
            }

            WhenToOutput.ALWAYS -> {
                if (useSeparator) {
                    char(':')
                }
                appendOffsetMinutesOfHour()
                when (outputSecond) {
                    WhenToOutput.NEVER -> {}
                    WhenToOutput.IF_NONZERO -> {
                        optional {
                            if (useSeparator) {
                                char(':')
                            }
                            appendOffsetSecondsOfMinute()
                        }
                    }

                    WhenToOutput.ALWAYS -> {
                        if (useSeparator) {
                            char(':')
                        }
                        appendOffsetSecondsOfMinute()
                    }
                }
            }
        }
    }
    if (zOnZero) {
        optional("Z") {
            alternativeParsing({
                char('z')
            }) {
                appendIsoOffsetWithoutZOnZero()
            }
        }
    } else {
        appendIsoOffsetWithoutZOnZero()
    }
}

internal object OffsetFields {
    private val sign = object : FieldSign<UtcOffsetFieldContainer> {
        override val isNegative = UtcOffsetFieldContainer::isNegative
        override fun isZero(obj: UtcOffsetFieldContainer): Boolean =
            (obj.totalHoursAbs ?: 0) == 0 && (obj.minutesOfHour ?: 0) == 0 && (obj.secondsOfMinute ?: 0) == 0
    }
    val totalHoursAbs = UnsignedFieldSpec(
        UtcOffsetFieldContainer::totalHoursAbs,
        defaultValue = 0,
        minValue = 0,
        maxValue = 18,
        sign = sign,
    )
    val minutesOfHour = UnsignedFieldSpec(
        UtcOffsetFieldContainer::minutesOfHour,
        defaultValue = 0,
        minValue = 0,
        maxValue = 59,
        sign = sign,
    )
    val secondsOfMinute = UnsignedFieldSpec(
        UtcOffsetFieldContainer::secondsOfMinute,
        defaultValue = 0,
        minValue = 0,
        maxValue = 59,
        sign = sign,
    )
}

internal class IncompleteUtcOffset(
    override var isNegative: Boolean? = null,
    override var totalHoursAbs: Int? = null,
    override var minutesOfHour: Int? = null,
    override var secondsOfMinute: Int? = null,
) : UtcOffsetFieldContainer, Copyable<IncompleteUtcOffset> {

    fun toUtcOffset(): UtcOffset {
        val sign = if (isNegative == true) -1 else 1
        return UtcOffset(
            totalHoursAbs?.let { it * sign }, minutesOfHour?.let { it * sign }, secondsOfMinute?.let { it * sign }
        )
    }

    fun populateFrom(offset: UtcOffset) {
        isNegative = offset.totalSeconds < 0
        val totalSecondsAbs = offset.totalSeconds.absoluteValue
        totalHoursAbs = totalSecondsAbs / 3600
        minutesOfHour = (totalSecondsAbs / 60) % 60
        secondsOfMinute = totalSecondsAbs % 60
    }

    override fun equals(other: Any?): Boolean =
        other is IncompleteUtcOffset && isNegative == other.isNegative && totalHoursAbs == other.totalHoursAbs &&
            minutesOfHour == other.minutesOfHour && secondsOfMinute == other.secondsOfMinute

    override fun hashCode(): Int =
        isNegative.hashCode() + totalHoursAbs.hashCode() + minutesOfHour.hashCode() + secondsOfMinute.hashCode()

    override fun copy(): IncompleteUtcOffset =
        IncompleteUtcOffset(isNegative, totalHoursAbs, minutesOfHour, secondsOfMinute)

    override fun toString(): String =
        "${isNegative?.let { if (it) "-" else "+" } ?: " "}${totalHoursAbs ?: "??"}:${minutesOfHour ?: "??"}:${secondsOfMinute ?: "??"}"
}

internal class UtcOffsetWholeHoursDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(
        OffsetFields.totalHoursAbs,
        minDigits = padding.minDigits(2),
        spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String =
        "${DateTimeFormatBuilder.WithUtcOffset::appendOffsetTotalHours.name}($padding)"
}

internal class UtcOffsetMinuteOfHourDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(
        OffsetFields.minutesOfHour,
        minDigits = padding.minDigits(2), spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String = when (padding) {
        Padding.NONE -> "${DateTimeFormatBuilder.WithUtcOffset::appendOffsetMinutesOfHour.name}()"
        else -> "${DateTimeFormatBuilder.WithUtcOffset::appendOffsetMinutesOfHour.name}($padding)"
    }
}

internal class UtcOffsetSecondOfMinuteDirective(padding: Padding) :
    UnsignedIntFieldFormatDirective<UtcOffsetFieldContainer>(
        OffsetFields.secondsOfMinute,
        minDigits = padding.minDigits(2), spacePadding = padding.spaces(2)
    ) {

    override val builderRepresentation: String = when (padding) {
        Padding.NONE -> "${DateTimeFormatBuilder.WithUtcOffset::appendOffsetSecondsOfMinute.name}()"
        else -> "${DateTimeFormatBuilder.WithUtcOffset::appendOffsetSecondsOfMinute.name}($padding)"
    }
}

// these are constants so that the formats are not recreated every time they are used
@SharedImmutable
internal val ISO_OFFSET by lazy {
    UtcOffsetFormat.build {
        alternativeParsing({ chars("z") }) {
            optional("Z") {
                appendOffsetTotalHours()
                char(':')
                appendOffsetMinutesOfHour()
                optional {
                    char(':')
                    appendOffsetSecondsOfMinute()
                }
            }
        }
    }
}
@SharedImmutable
internal val ISO_OFFSET_BASIC by lazy {
    UtcOffsetFormat.build {
        alternativeParsing({ chars("z") }) {
            optional("Z") {
                appendOffsetTotalHours()
                optional {
                    appendOffsetMinutesOfHour()
                    optional {
                        appendOffsetSecondsOfMinute()
                    }
                }
            }
        }
    }
}
@SharedImmutable
internal val FOUR_DIGIT_OFFSET by lazy {
    UtcOffsetFormat.build {
        appendOffsetTotalHours()
        appendOffsetMinutesOfHour()
    }
}
