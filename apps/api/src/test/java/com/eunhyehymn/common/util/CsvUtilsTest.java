package com.eunhyehymn.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvUtilsTest {
    @Test
    void toSafeCsvCellPrefixesQuoteForFormulaLikeValue() {
        String escaped = CsvUtils.toSafeCsvCell("=1+1");
        assertThat(escaped).isEqualTo("\"'=1+1\"");
    }

    @Test
    void toSafeCsvCellHandlesLeadingWhitespaceFormula() {
        String escaped = CsvUtils.toSafeCsvCell("  -SUM(1,1)");
        assertThat(escaped).isEqualTo("\"'  -SUM(1,1)\"");
    }

    @Test
    void toSafeCsvCellEscapesQuotes() {
        String escaped = CsvUtils.toSafeCsvCell("a\"b");
        assertThat(escaped).isEqualTo("\"a\"\"b\"");
    }
}
