package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the NItemsByUnit class, focusing on the parser method.
 */
class NItemsByUnitTest {

    @Test
    @DisplayName("Debería parsear correctamente una línea válida con varias unidades")
    void testParseValidMultipleItems() throws ParseException {
        // Ejemplo real del ticket: 2   FRANKFURT VIENA QUES                 2,80       5,60
        String line = "2   FRANKFURT VIENA QUES                 2,80       5,60";
        ParserStatusInfo status = new ParserStatusInfo(line);
        NItemsByUnit item = NItemsByUnit.parse(status);

        assertNotNull(item, "El item no debería ser null");
        assertEquals("FRANKFURT VIENA QUES", item.id());
        assertEquals(2, item.cantidad());
        assertEquals(new BigDecimal("2.80"), item.precioPorUnidad());
        assertEquals(new BigDecimal("5.60"), item.precio());
        assertEquals(new BigDecimal("5.60"), item.precioTotal());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con 4 unidades y muchos espacios")
    void testParseValid4Items() throws ParseException {
        // Ejemplo del comentario en el código: 4 TORTILLA PAT C/CEB 1,5L 2,60 10,40
        String line = "4   TORTILLA PAT C/CEB 1,5L            2,60       10,40";
        ParserStatusInfo status = new ParserStatusInfo(line);
        NItemsByUnit item = NItemsByUnit.parse(status);

        assertNotNull(item);
        assertEquals("TORTILLA PAT C/CEB 1,5L", item.id());
        assertEquals(4, item.cantidad());
        assertEquals(new BigDecimal("2.60"), item.precioPorUnidad());
        assertEquals(new BigDecimal("10.40"), item.precio());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con 1 unidad e ignorar el precio unitario (comportamiento actual)")
    void testParseValid1ItemWithExplicitUnitPrice() throws ParseException {
        // En el código actual, si cantidad es 1, el precioPorUnidad se queda como null 
        // a pesar de que el regex lo captura.
        String line = "1 PRODUCTO EXPLICITO 1,50 1,50";
        ParserStatusInfo status = new ParserStatusInfo(line);
        NItemsByUnit item = NItemsByUnit.parse(status);

        assertNotNull(item);
        assertEquals("PRODUCTO EXPLICITO", item.id());
        assertEquals(1, item.cantidad());
        assertNull(item.precioPorUnidad());
        assertEquals(new BigDecimal("1.50"), item.precio());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con caracteres especiales en el ID")
    void testParseValidItemWithSpecialChars() throws ParseException {
        String line = "3 CHORIZO 4PACK/EXTRA 2,00 6,00";
        ParserStatusInfo status = new ParserStatusInfo(line);
        NItemsByUnit item = NItemsByUnit.parse(status);

        assertNotNull(item);
        assertEquals("CHORIZO 4PACK/EXTRA", item.id());
        assertEquals(new BigDecimal("2.00"), item.precioPorUnidad());
        assertEquals(new BigDecimal("6.00"), item.precio());

        line = "2 QUESO 50% DTO. 1,50 3,00";
        status = new ParserStatusInfo(line);
        item = NItemsByUnit.parse(status);
        assertNotNull(item);
        assertEquals("QUESO 50% DTO.", item.id());
    }

    @Test
    @DisplayName("Debería parsear correctamente una línea con espacios al principio y al final")
    void testParseValidItemWithLeadingTrailingSpaces() throws ParseException {
        String line = "  2 PAN BLANCO 0,50 1,00  ";
        ParserStatusInfo status = new ParserStatusInfo(line);
        NItemsByUnit item = NItemsByUnit.parse(status);

        assertNotNull(item);
        assertEquals("PAN BLANCO", item.id());
        assertEquals(new BigDecimal("0.50"), item.precioPorUnidad());
        assertEquals(new BigDecimal("1.00"), item.precio());
    }

    @Test
    @DisplayName("Debería devolver null para líneas que no coinciden con el patrón")
    void testParseReturnsNullForInvalidPatterns() throws ParseException {
        String[] invalidLines = {
                "1 BARRA DE PAN 0,48",    // Solo 3 campos (manejado por OneItemByUnit)
                "2 PRODUCTO 1,50",        // Faltan campos (total o unidad)
                "PRODUCTO 1,50 3,00",     // Falta la cantidad
                "2 PRODUCTO 1.50 3.00",   // Precio con punto en vez de coma
                "2 PRODUCTO 1,5 3,0",     // Precio con un solo decimal
                "2 PRODUCTO 1,500 3,000", // Precio con tres decimales (REGEX_PRECIO permite 2)
                "2 producto 1,50 3,00",   // ID en minúsculas (REGEX_ID no las permite)
                "",                       // Línea vacía
                "2  1,50 3,00"            // Falta la descripción
        };

        for (String line : invalidLines) {
            ParserStatusInfo status = new ParserStatusInfo(line + "\n");
            NItemsByUnit item = NItemsByUnit.parse(status);
            assertNull(item, "Debería devolver null para la línea: [" + line + "]");
            // Verificar que el iterador volvió atrás
            assertTrue(status.hasNext());
            assertEquals(0, status.nextIndex());
            assertEquals(line, status.next());
        }
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si el ID es nulo o vacío en el constructor")
    void testConstructorConstraints() {
        assertThrows(NullPointerException.class, () -> new NItemsByUnit(null, 2, new BigDecimal("1.00"), new BigDecimal("2.00")));
        assertThrows(IllegalArgumentException.class, () -> new NItemsByUnit("", 2, new BigDecimal("1.00"), new BigDecimal("2.00")));
        assertThrows(IllegalArgumentException.class, () -> new NItemsByUnit("  ", 2, new BigDecimal("1.00"), new BigDecimal("2.00")));
        assertThrows(IllegalArgumentException.class, () -> new NItemsByUnit("PROD", 0, new BigDecimal("1.00"), new BigDecimal("2.00")));
    }
}
