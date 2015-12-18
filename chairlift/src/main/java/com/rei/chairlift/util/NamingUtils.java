package com.rei.chairlift.util;

import org.apache.commons.lang3.text.WordUtils;

public class NamingUtils {
    public static String toCamelCase(String input) {
        return WordUtils.capitalizeFully(input);
    }
}
