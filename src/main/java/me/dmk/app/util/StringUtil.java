package me.dmk.app.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Created by DMK on 30.03.2023
 */

@UtilityClass
public class StringUtil {

    public boolean isNotLong(String string) {
        try {
             Long.parseLong(string);
        } catch (NumberFormatException numberFormatException) {
            return true;
        }
        return false;
    }

    public static Optional<Instant> toInstant(String string) {
        if (string.isEmpty() || string.length() == 1)
            return Optional.empty();

        long difference;
        try {
            difference = Long.parseLong(string.substring(0, string.length() - 1));
        } catch (Exception exception) {
            return Optional.empty();
        }

        char charAt = string.toUpperCase().charAt(string.length() - 1);

        return switch (charAt) {
            case 'S' -> Optional.of(Instant.now().plus(difference, ChronoUnit.SECONDS));
            case 'M' -> Optional.of(Instant.now().plus(difference, ChronoUnit.MINUTES));
            case 'H' -> Optional.of(Instant.now().plus(difference, ChronoUnit.HOURS));
            case 'D' -> Optional.of(Instant.now().plus(difference, ChronoUnit.DAYS));
            default -> Optional.empty();
        };
    }

    public static String formatTime(long i, String single, String second, String many) {
        long iDivided = i % 10L;

        return (i == 1 ? single : (i > 1 && iDivided < 5 && iDivided != 1 && iDivided != 0) ? second : many);
    }

    public static String formatDuration(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return "<1s";
        }

        long millis = duration.toMillis();
        long seconds = duration.toSecondsPart();
        long minutes = duration.toMinutesPart();
        long hours = duration.toHoursPart();
        long days = duration.toDays();

        StringBuilder stringBuilder = new StringBuilder();

        if (days > 0) {
            stringBuilder.append(days)
                    .append(" ")
                    .append(formatTime(days, "dzień", "dni", "dni"))
                    .append(", ");
        }

        if (hours > 0) {
            stringBuilder.append(hours)
                    .append(" ")
                    .append(formatTime(hours, "godzinę", "godziny", "godzin"))
                    .append(", ");
        }

        if (minutes > 0) {
            stringBuilder.append(minutes)
                    .append(" ")
                    .append(formatTime(minutes, "minutę", "minuty", "minut"))
                    .append(", ");
        }

        if (seconds > 0) {
            stringBuilder.append(seconds)
                    .append(" ")
                    .append(formatTime(seconds, "sekundę", "sekundy", "sekund"));
        }

        if (stringBuilder.isEmpty() && millis > 0) {
            stringBuilder.append(millis)
                    .append(" ")
                    .append("ms");
        }

        return stringBuilder.toString();
    }
}
