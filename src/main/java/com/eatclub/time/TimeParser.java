package com.eatclub.time;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class TimeParser {

    private static final DateTimeFormatter TIME_24_HOUR =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_12_HOUR =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("h:mma")
                    .toFormatter(Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);

    private TimeParser() {
    }

    public static LocalTime parseTime(String input) {
        String time = input.strip();

        try {
            return LocalTime.parse(time, TIME_24_HOUR);
        } catch (DateTimeParseException ignored) {
            return LocalTime.parse(time, TIME_12_HOUR);
        }
    }
}
