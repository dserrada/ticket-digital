package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ShopData class, focusing on the parser method.
 */
class ShopDataTest {

    @Test
    @DisplayName("Debería parsear correctamente datos de tienda válidos")
    void testParseValidShopData() throws ParseException {
        // Datos basados en el archivo de ejemplo
        List<String> lines = List.of(
                "             MERCADONA, S.A. A-46103834",
                "             AV. DE FERRRAN EL CATÒLIC, 1",
                "                46113 MONCADA",
                "              TELÉFONO: 961309467"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ShopData shopData = ShopData.parser(status);

        assertNotNull(shopData);
        assertEquals("MERCADONA, S.A.", shopData.shopName());
        assertEquals("A-46103834", shopData.cif());
        assertEquals("AV. DE FERRRAN EL CATÒLIC, 1", shopData.address());
        assertEquals("46113", shopData.postalCode());
        assertEquals("MONCADA", shopData.state());
        assertEquals("961309467", shopData.phoneNumber());
    }

    @Test
    @DisplayName("Debería parsear correctamente con espacios mínimos y otros formatos de dirección")
    void testParseValidShopDataMinimalSpaces() throws ParseException {
        List<String> lines = List.of(
                "OTRA EMPRESA, S.A. B12345678",
                "CALLE FALSA, 123",
                "28001 MADRID",
                "TELÉFONO: 912345678"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ShopData shopData = ShopData.parser(status);

        assertNotNull(shopData);
        assertEquals("OTRA EMPRESA, S.A.", shopData.shopName());
        assertEquals("B12345678", shopData.cif());
        assertEquals("CALLE FALSA, 123", shopData.address());
        assertEquals("28001", shopData.postalCode());
        assertEquals("MADRID", shopData.state());
        assertEquals("912345678", shopData.phoneNumber());
    }

    @Test
    @DisplayName("Debería lanzar ParseException si faltan líneas")
    void testParseThrowsExceptionForMissingLines() {
        List<String> lines = List.of(
                "MERCADONA, S.A. A-46103834",
                "AV. DE FERRRAN EL CATÒLIC, 1",
                "46113 MONCADA"
                // Falta la 4ª línea
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> ShopData.parser(status));
        // Verificar que el iterador volvió al principio
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería lanzar ParseException si hay líneas vacías")
    void testParseThrowsExceptionForBlankLines() {
        List<String> lines = List.of(
                "MERCADONA, S.A. A-46103834",
                "  ", // Línea en blanco
                "46113 MONCADA",
                "TELÉFONO: 961309467"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> ShopData.parser(status));
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si falta 'S.A.' en la primera línea")
    void testParseThrowsExceptionForMissingSA() {
        List<String> lines = List.of(
                "MERCADONA, SL A-46103834", // Debería ser S.A. según implementación
                "CALLE",
                "12345 LOCALIDAD",
                "TELÉFONO: 123"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(IllegalArgumentException.class, () -> ShopData.parser(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si falta el CP o Localidad")
    void testParseThrowsExceptionForInvalidCPLocalidad() {
        List<String> lines = List.of(
                "MERCADONA, S.A. A-46103834",
                "CALLE",
                "46113", // Falta la localidad
                "TELÉFONO: 123"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> ShopData.parser(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si falta ':' en la línea de teléfono")
    void testParseThrowsExceptionForInvalidPhoneLine() {
        List<String> lines = List.of(
                "MERCADONA, S.A. A-46103834",
                "CALLE",
                "46113 MONCADA",
                "TELÉFONO 961309467" // Falta ':'
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> ShopData.parser(status));
    }
}
