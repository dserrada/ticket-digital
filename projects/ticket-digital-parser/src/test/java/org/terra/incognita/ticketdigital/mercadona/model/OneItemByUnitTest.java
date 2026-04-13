package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OneItemByUnit class, focusing on the parser method.
 */
class OneItemByUnitTest {

    @Test
    @DisplayName("Debería parsear correctamente una línea válida con una unidad")
    void testParseValidSingleItem() throws ParseException {
        String line = "1 PRODUCTO 1,50";
        ParserStatusInfo status = new ParserStatusInfo(List.of(line).listIterator());
        OneItemByUnit item = OneItemByUnit.parse(status);

        assertNotNull(item, "El item no debería ser null");
        assertEquals("PRODUCTO", item.id());
        assertEquals(1, item.cantidad());
        assertNull(item.precioPorUnidad());
        assertEquals(new BigDecimal("1.50"), item.precio());
        assertEquals(new BigDecimal("1.50"), item.precioTotal());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con muchos espacios entre descripción e importe")
    void testParseValidItemWithManySpaces() throws ParseException {
        // Ejemplo real del archivo de ticket
        String line = "1   BARRA DE PAN                                    0,48";
        ParserStatusInfo status = new ParserStatusInfo(List.of(line).listIterator());
        OneItemByUnit item = OneItemByUnit.parse(status);

        assertNotNull(item);
        assertEquals("BARRA DE PAN", item.id());
        assertEquals(new BigDecimal("0.48"), item.precio());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con caracteres especiales en el ID")
    void testParseValidItemWithSpecialChars() throws ParseException {
        String line = "1 CHORIZO 4PACK/EXTRA 1,97";
        ParserStatusInfo status = new ParserStatusInfo(List.of(line).listIterator());
        OneItemByUnit item = OneItemByUnit.parse(status);

        assertNotNull(item);
        assertEquals("CHORIZO 4PACK/EXTRA", item.id());
        assertEquals(new BigDecimal("1.97"), item.precio());

        line = "1 QUESO 50% DTO. 2,00";
        status = new ParserStatusInfo(List.of(line).listIterator());
        item = OneItemByUnit.parse(status);
        assertNotNull(item);
        assertEquals("QUESO 50% DTO.", item.id());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con espacios al principio y al final")
    void testParseValidItemWithLeadingTrailingSpaces() throws ParseException {
        String line = "  1 PAN BLANCO 0,50  ";
        ParserStatusInfo status = new ParserStatusInfo(List.of(line).listIterator());
        OneItemByUnit item = OneItemByUnit.parse(status);

        assertNotNull(item);
        assertEquals("PAN BLANCO", item.id());
        assertEquals(new BigDecimal("0.50"), item.precio());
    }

    @Test
    @DisplayName("Debería devolver null para líneas que no coinciden con el patrón")
    void testParseReturnsNullForInvalidPatterns() throws ParseException {
        String[] invalidLines = {
                "2 PRODUCTO 1,00 2,00", // Empieza por 2
                "PRODUCTO 1,50",        // No empieza por 1
                "1 PRODUCTO",           // Falta el precio
                "1 PRODUCTO 1.50",      // Precio con punto en vez de coma
                "1 PRODUCTO 1,5",       // Precio con un solo decimal
                "1 PRODUCTO 1,500",     // Precio con tres decimales (REGEX_PRECIO solo permite 2)
                "1 producto 1,50",      // ID en minúsculas (REGEX_ID solo permite A-Z mayúsculas y otros)
                "",                     // Línea vacía
                "1  1,50"               // Falta la descripción
        };

        for (String line : invalidLines) {
            ParserStatusInfo status = new ParserStatusInfo(List.of(line).listIterator());
            OneItemByUnit item = OneItemByUnit.parse(status);
            assertNull(item, "Debería devolver null para la línea: [" + line + "]");
            // Verificar que el iterador volvió atrás
            assertTrue(status.iterator().hasNext());
            assertEquals(0, status.iterator().nextIndex());
            assertEquals(line, status.iterator().next());
        }
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si el ID es nulo o vacío en el constructor")
    void testConstructorConstraints() {
        assertThrows(NullPointerException.class, () -> new OneItemByUnit(null, 1, null, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new OneItemByUnit("", 1, null, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new OneItemByUnit("  ", 1, null, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new OneItemByUnit("PROD", 0, null, new BigDecimal("1.00")));
    }
}