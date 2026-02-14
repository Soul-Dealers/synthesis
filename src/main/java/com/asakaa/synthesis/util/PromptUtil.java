package com.asakaa.synthesis.util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptUtil {

    private static final int MAX_PROMPT_LENGTH = 2000;

    /**
     * Sanitize input for use in AI prompts
     * Removes characters that could interfere with JSON or cause prompt injection
     * @param input Input string to sanitize
     * @return Sanitized string
     */
    public String sanitizeForPrompt(String input) {
        if (input == null) {
            return "";
        }

        // Remove control characters and normalize whitespace
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\\r\\n|\\r|\\n", " ")
                .trim();

        // Limit length
        if (sanitized.length() > MAX_PROMPT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_PROMPT_LENGTH);
        }

        return sanitized;
    }

    /**
     * Format a list as a numbered string for prompts
     * @param items List of items
     * @return Formatted numbered string
     */
    public String formatListForPrompt(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            formatted.append(String.format("%d. %s\n", i + 1, items.get(i)));
        }

        return formatted.toString().trim();
    }

    /**
     * Truncate text to maximum length
     * @param text Text to truncate
     * @param maxLength Maximum length
     * @return Truncated text with "..." if truncated
     */
    public String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }
}
