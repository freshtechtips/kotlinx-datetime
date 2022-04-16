/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalTimeJvmKt")

package kotlinx.datetime

import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.LocalTime as jtLocalTime

@Serializable(with = LocalTimeIso8601Serializer::class)
public actual class LocalTime internal constructor(internal val value: jtLocalTime) :
    Comparable<LocalTime> {

    public actual constructor(hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(
                try {
                    jtLocalTime.of(hour, minute, second, nanosecond)
                } catch (e: DateTimeException) {
                    throw IllegalArgumentException(e)
                }
            )

    public actual val hour: Int get() = value.hour
    public actual val minute: Int get() = value.minute
    public actual val second: Int get() = value.second
    public actual val nanosecond: Int get() = value.nano

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(isoString: String): LocalTime = try {
            jtLocalTime.parse(isoString).let(::LocalTime)
        } catch (e: DateTimeParseException) {
            throw DateTimeFormatException(e)
        }

        internal actual val MIN: LocalTime = LocalTime(jtLocalTime.MIN)
        internal actual val MAX: LocalTime = LocalTime(jtLocalTime.MAX)
    }
}