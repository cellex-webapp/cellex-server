package com.example.cellex.utils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utility to generate URL-friendly slugs from Vietnamese (and other) strings.
 */
public final class SlugUtil {

    private SlugUtil() {}

    public static String toSlug(String input) {
        if (input == null) return "";
        // Normalize Unicode and remove diacritics
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Remove diacritic marks
        String withoutDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Replace non alphanumeric characters with hyphen
        String replaced = withoutDiacritics.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        // Collapse multiple hyphens
        return replaced.replaceAll("-+", "-");
    }
}
