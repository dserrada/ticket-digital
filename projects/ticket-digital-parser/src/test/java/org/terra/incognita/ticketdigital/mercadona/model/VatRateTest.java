package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VatRateTest {

    @Test
    @DisplayName("Debería parsear correctamente un tramo del 10%")
    void testParseValidRate10Percent() throws ParseException {
        ParserStatusInfo status = new ParserStatusInfo("     10%                   8,54             0,85");
        VatRate rate = VatRate.parse(status);

        assertNotNull(rate);
        assertEquals(new BigDecimal("10"), rate.rate());
        assertEquals(new BigDecimal("8.54"), rate.taxableBase());
        assertEquals(new BigDecimal("0.85"), rate.vatAmount());
    }

    @Test
    @DisplayName("Debería parsear correctamente un tramo del 0%")
    void testParseValidRate0Percent() throws ParseException {
        ParserStatusInfo status = new ParserStatusInfo("      0%                   1,10               0,00");
        VatRate rate = VatRate.parse(status);

        assertNotNull(rate);
        assertEquals(new BigDecimal("0"), rate.rate());
        assertEquals(new BigDecimal("1.10"), rate.taxableBase());
        assertEquals(new BigDecimal("0.00"), rate.vatAmount());
    }

    @Test
    @DisplayName("Debería parsear correctamente un tramo con porcentaje decimal (7,5%)")
    void testParseValidRateDecimalPercent() throws ParseException {
        ParserStatusInfo status = new ParserStatusInfo("      7,5%                   5,49             0,41");
        VatRate rate = VatRate.parse(status);

        assertNotNull(rate);
        assertEquals(new BigDecimal("7.5"), rate.rate());
        assertEquals(new BigDecimal("5.49"), rate.taxableBase());
        assertEquals(new BigDecimal("0.41"), rate.vatAmount());
    }

    @Test
    @DisplayName("Debería devolver null y hacer rollback para la fila TOTAL")
    void testParseReturnsNullForTotalRow() throws ParseException {
        ParserStatusInfo status = new ParserStatusInfo("    TOTAL                 11,65             0,85");
        VatRate rate = VatRate.parse(status);

        assertNull(rate);
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería devolver null si la línea no tiene un porcentaje")
    void testParseReturnsNullForMissingPercent() throws ParseException {
        ParserStatusInfo status = new ParserStatusInfo("     IVA           BASE IMPONIBLE (€)     CUOTA (€)");
        VatRate rate = VatRate.parse(status);

        assertNull(rate);
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería devolver null si no hay más líneas")
    void testParseReturnsNullForEmptyInput() throws ParseException {
        assertNull(VatRate.parse(new ParserStatusInfo(String.join("\n", List.of()))));
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si el porcentaje está fuera de rango")
    void testConstructorConstraints() {
        assertThrows(IllegalArgumentException.class,
                () -> new VatRate(new BigDecimal("-1"), BigDecimal.ONE, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new VatRate(new BigDecimal("101"), BigDecimal.ONE, BigDecimal.ZERO));
    }
}
