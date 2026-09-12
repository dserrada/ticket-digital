package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardPaymentTest {

    @Test
    @DisplayName("Debería parsear correctamente con marca suelta y sin 'Verificado por dispositivo'")
    void testParseValidNoVerificadoWithStandaloneBrand() throws ParseException {
        List<String> lines = List.of(
                "TARJ. BANCARIA: **** **** **** 3851",
                "N.C: 003812591                                AUT: R91211",
                "AID: A0000000041010                             ARC: 3030",
                "",
                "",
                "MASTERCARD",
                "Importe: 33,50 €                           MASTERCARD"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        CardPayment cardPayment = CardPayment.parse(status);

        assertNotNull(cardPayment);
        assertEquals("3851", cardPayment.lastFourDigits());
        assertEquals("003812591", cardPayment.nc());
        assertEquals("R91211", cardPayment.aut());
        assertEquals("A0000000041010", cardPayment.aid());
        assertEquals("3030", cardPayment.arc());
        assertEquals("MASTERCARD", cardPayment.brand());
        assertEquals(new BigDecimal("33.50"), cardPayment.amount());
    }

    @Test
    @DisplayName("Debería parsear correctamente con 'Verificado por dispositivo' y marca con espacio")
    void testParseValidWithVerificadoPorDispositivo() throws ParseException {
        List<String> lines = List.of(
                "TARJ. BANCARIA: **** **** **** 1293",
                "N.C: 032849226                              AUT: 724541",
                "AID: A0000000031010                            ARC: 00",
                "Verificado por dispositivo",
                "",
                "",
                "Importe: 122,05 €                               Visa Debit"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        CardPayment cardPayment = CardPayment.parse(status);

        assertNotNull(cardPayment);
        assertEquals("1293", cardPayment.lastFourDigits());
        assertEquals("032849226", cardPayment.nc());
        assertEquals("724541", cardPayment.aut());
        assertEquals("A0000000031010", cardPayment.aid());
        assertEquals("00", cardPayment.arc());
        assertEquals("Visa Debit", cardPayment.brand());
        assertEquals(new BigDecimal("122.05"), cardPayment.amount());
    }

    @Test
    @DisplayName("Debería parsear correctamente sin punto tras TARJ y con el símbolo € mal codificado (Â€)")
    void testParseValidNoPeriodAfterTarjAndMojibakeEuro() throws ParseException {
        List<String> lines = List.of(
                "TARJ BANCARIA: **** **** **** 3844",
                "N.C: 003812591                              AUT: R04080",
                "AID: A0000000041010                            ARC: 00",
                "MASTERCARD",
                "Importe: 12,20 Â€ MASTERCARD"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        CardPayment cardPayment = CardPayment.parse(status);

        assertNotNull(cardPayment);
        assertEquals("3844", cardPayment.lastFourDigits());
        assertEquals("MASTERCARD", cardPayment.brand());
        assertEquals(new BigDecimal("12.20"), cardPayment.amount());
    }

    @Test
    @DisplayName("Debería parsear el formato alternativo sin N.C, con AUT/ARC/AID en una línea y AID solo en la siguiente")
    void testParseValidAlternativeFormatWithoutNc() throws ParseException {
        List<String> lines = List.of(
                "TARJ. BANCARIA:  **** **** **** 5045",
                "AUT: 142263 ARC: 00 AID: ",
                "A0000000041010",
                "Verificado por dispositivo",
                "Importe: 66,97 €   DEBIT ",
                "                                      MASTERCARD"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        CardPayment cardPayment = CardPayment.parse(status);

        assertNotNull(cardPayment);
        assertEquals("5045", cardPayment.lastFourDigits());
        assertNull(cardPayment.nc());
        assertEquals("142263", cardPayment.aut());
        assertEquals("00", cardPayment.arc());
        assertEquals("A0000000041010", cardPayment.aid());
        assertEquals("DEBIT", cardPayment.brand());
        assertEquals(new BigDecimal("66.97"), cardPayment.amount());
        // La línea suelta con la marca tras el Importe: se descarta como relleno, sin
        // consumir la línea siguiente (que no existe aquí, así que no queda nada)
        assertFalse(status.hasNext());
    }

    @Test
    @DisplayName("Debería devolver null si la línea de tarjeta enmascarada no coincide")
    void testParseReturnsNullForInvalidMaskedLine() throws ParseException {
        List<String> lines = List.of(
                "ESTO NO ES UNA TARJETA",
                "N.C: 1234567                                  AUT: Z12345"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertNull(CardPayment.parse(status));
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería devolver null si la línea N.C/AUT no coincide")
    void testParseReturnsNullForInvalidNcAutLine() throws ParseException {
        List<String> lines = List.of(
                "TARJ. BANCARIA: **** **** **** 1234",
                "ESTO NO ES N.C NI AUT"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertNull(CardPayment.parse(status));
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería devolver null si la línea AID/ARC no coincide")
    void testParseReturnsNullForInvalidAidArcLine() throws ParseException {
        List<String> lines = List.of(
                "TARJ. BANCARIA: **** **** **** 1234",
                "N.C: 1234567                                  AUT: Z12345",
                "ESTO NO ES AID NI ARC"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertNull(CardPayment.parse(status));
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería lanzar ParseException si aparece una línea de relleno no reconocida")
    void testParseThrowsForUnrecognizedFillerLine() {
        List<String> lines = List.of(
                "TARJ. BANCARIA: **** **** **** 1234",
                "N.C: 1234567                                  AUT: Z12345",
                "AID: A0000000041010                           ARC: 46113",
                "esto no es una línea de relleno reconocida",
                "Importe: 9,64 €                          MASTERCARD"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> CardPayment.parse(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si nunca aparece la línea Importe")
    void testParseThrowsForMissingImporteLine() {
        List<String> lines = List.of(
                "TARJ. BANCARIA: **** **** **** 1234",
                "N.C: 1234567                                  AUT: Z12345",
                "AID: A0000000041010                           ARC: 46113",
                "",
                ""
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> CardPayment.parse(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si se supera el límite de líneas de relleno")
    void testParseThrowsWhenFillerLineBoundExceeded() {
        List<String> lines = new ArrayList<>(List.of(
                "TARJ. BANCARIA: **** **** **** 1234",
                "N.C: 1234567                                  AUT: Z12345",
                "AID: A0000000041010                           ARC: 46113"
        ));
        for (int i = 0; i < CardPayment.MAX_FILLER_LINES + 1; i++) {
            lines.add("");
        }
        lines.add("Importe: 9,64 €                          MASTERCARD");

        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> CardPayment.parse(status));
    }
}
