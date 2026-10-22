package com.buy2go.api.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy, h:mm:ss a");

    public static String formatInstant(Instant instant) {
        if (instant == null) return "";
        return instant.atZone(ZoneId.of("Asia/Kolkata")).format(DATE_FORMATTER);
    }

    public static Instant getStartOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
    }

    public static Instant getEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().minusMillis(1);
    }
}
