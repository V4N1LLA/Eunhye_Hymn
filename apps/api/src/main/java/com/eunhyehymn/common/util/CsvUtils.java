package com.eunhyehymn.common.util;

public final class CsvUtils {
    private CsvUtils() {
    }

    public static String toSafeCsvCell(Object value) {
        if (value == null) {
            return "";
        }

        String raw = value.toString();
        if (looksLikeSpreadsheetFormula(raw)) {
            raw = "'" + raw;
        }
        raw = raw.replace("\"", "\"\"");
        return "\"" + raw + "\"";
    }

    private static boolean looksLikeSpreadsheetFormula(String value) {
        if (value.isEmpty()) {
            return false;
        }

        int firstNonWhitespaceIndex = 0;
        while (firstNonWhitespaceIndex < value.length()
            && Character.isWhitespace(value.charAt(firstNonWhitespaceIndex))) {
            firstNonWhitespaceIndex += 1;
        }
        if (firstNonWhitespaceIndex >= value.length()) {
            return false;
        }

        char first = value.charAt(firstNonWhitespaceIndex);
        return first == '=' || first == '+' || first == '-' || first == '@';
    }
}
