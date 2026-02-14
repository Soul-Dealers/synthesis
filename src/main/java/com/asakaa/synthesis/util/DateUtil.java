package com.asakaa.synthesis.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class DateUtil {

    private static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter PARSE_DATE_FORMATTER_1 = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter PARSE_DATE_FORMATTER_2 = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Format LocalDateTime for display
     * @param dt DateTime to format
     * @return Formatted string (dd MMM yyyy HH:mm)
     */
    public String formatForDisplay(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        return dt.format(DISPLAY_DATETIME_FORMATTER);
    }

    /**
     * Format LocalDate for display
     * @param date Date to format
     * @return Formatted string (dd MMM yyyy)
     */
    public String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DISPLAY_DATE_FORMATTER);
    }

    /**
     * Parse date string in multiple formats
     * @param dateString Date string to parse (dd/MM/yyyy or yyyy-MM-dd)
     * @return Parsed LocalDate
     */
    public LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            // Try dd/MM/yyyy format first
            return LocalDate.parse(dateString, PARSE_DATE_FORMATTER_1);
        } catch (DateTimeParseException e1) {
            try {
                // Try yyyy-MM-dd format
                return LocalDate.parse(dateString, PARSE_DATE_FORMATTER_2);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format. Expected dd/MM/yyyy or yyyy-MM-dd");
            }
        }
    }

    /**
     * Check if patient is an adult (age >= 18)
     * @param dateOfBirth Patient's date of birth
     * @return True if adult
     */
    public boolean isAdult(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= 18;
    }

    /**
     * Check if patient is pediatric (age < 12)
     * @param dateOfBirth Patient's date of birth
     * @return True if pediatric
     */
    public boolean isPediatric(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age < 12;
    }
}
