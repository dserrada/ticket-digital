package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VatBreakdownTest {

    @Test
    @DisplayName("Debería parsear correctamente un desglose con 2 tramos")
    void testParseValidBreakdownTwoRates() throws ParseException {
        List<String> lines = List.of(
                "     IVA           BASE IMPONIBLE (€)     CUOTA (€)",
                "      2%                   6,81             0,14",
                "     10%                  64,33             6,43",
                "    TOTAL                 71,14             6,57"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        VatBreakdown breakdown = VatBreakdown.parse(status);

        assertNotNull(breakdown);
        assertEquals(2, breakdown.rates().size());
        assertEquals(new BigDecimal("2"), breakdown.rates().get(0).rate());
        assertEquals(new BigDecimal("6.81"), breakdown.rates().get(0).taxableBase());
        assertEquals(new BigDecimal("0.14"), breakdown.rates().get(0).vatAmount());
        assertEquals(new BigDecimal("10"), breakdown.rates().get(1).rate());
        assertEquals(new BigDecimal("64.33"), breakdown.rates().get(1).taxableBase());
        assertEquals(new BigDecimal("6.43"), breakdown.rates().get(1).vatAmount());
        assertEquals(new BigDecimal("71.14"), breakdown.totalTaxableBase());
        assertEquals(new BigDecimal("6.57"), breakdown.totalVatAmount());
    }

    @Test
    @DisplayName("Debería parsear correctamente un desglose con 3 tramos")
    void testParseValidBreakdownThreeRates() throws ParseException {
        List<String> lines = List.of(
                "     IVA            BASE IMPONIBLE (€)     CUOTA (€)",
                "      4%                   34,83              1,39",
                "     10%                   53,25              5,33",
                "     21%                   22,52              4,73",
                "    TOTAL                 110,60             11,45"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        VatBreakdown breakdown = VatBreakdown.parse(status);

        assertNotNull(breakdown);
        assertEquals(3, breakdown.rates().size());
        assertEquals(new BigDecimal("4"), breakdown.rates().get(0).rate());
        assertEquals(new BigDecimal("21"), breakdown.rates().get(2).rate());
        assertEquals(new BigDecimal("110.60"), breakdown.totalTaxableBase());
        assertEquals(new BigDecimal("11.45"), breakdown.totalVatAmount());
    }

    @Test
    @DisplayName("Debería lanzar ParseException si la suma de tramos no coincide con la fila TOTAL")
    void testParseThrowsWhenSumDoesNotMatchTotal() {
        List<String> lines = List.of(
                "     IVA           BASE IMPONIBLE (€)     CUOTA (€)",
                "      2%                   6,81             0,14",
                "     10%                  64,33             6,43",
                "    TOTAL                 99,99             6,57"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> VatBreakdown.parse(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si falta la cabecera")
    void testParseThrowsForMissingHeader() {
        List<String> lines = List.of(
                "      2%                   6,81             0,14",
                "    TOTAL                  6,81             0,14"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> VatBreakdown.parse(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si se llega a EOF antes de la fila TOTAL")
    void testParseThrowsForEOFBeforeTotalRow() {
        List<String> lines = List.of(
                "     IVA           BASE IMPONIBLE (€)     CUOTA (€)",
                "      2%                   6,81             0,14"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> VatBreakdown.parse(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si aparece una fila no reconocida")
    void testParseThrowsForUnrecognizedRow() {
        List<String> lines = List.of(
                "     IVA           BASE IMPONIBLE (€)     CUOTA (€)",
                "      2%                   6,81             0,14",
                "     esto no es una fila válida",
                "    TOTAL                  6,81             0,14"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> VatBreakdown.parse(status));
    }
}
