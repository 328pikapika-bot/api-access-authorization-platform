package com.nstl.openplatform.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.util.StringUtils;

public final class DateTimeRangeUtil {

    private DateTimeRangeUtil() {
    }

    public static LocalDateTime normalizeStartTime(Object value) {
        LocalDateTime dateTime = parseDateTime(value);
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? dateTime.toLocalDate().atStartOfDay()
                : dateTime;
    }

    public static LocalDateTime normalizeEndTime(Object value) {
        LocalDateTime dateTime = parseDateTime(value);
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? dateTime.toLocalDate().atTime(23, 59, 59)
                : dateTime;
    }

    public static LocalDateTime parseDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay();
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (!(value instanceof String)) {
            return null;
        }

        String normalized = ((String) value).trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        String isoLike = normalized.replace(" ", "T");
        try {
            return OffsetDateTime.parse(isoLike).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(isoLike);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(normalized).atStartOfDay();
        } catch (Exception ignored) {
        }
        return null;
    }
}
