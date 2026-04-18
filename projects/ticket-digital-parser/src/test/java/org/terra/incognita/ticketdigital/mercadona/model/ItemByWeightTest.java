package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ItemByWeight class, focusing on the parser method.
 */
class ItemByWeightTest {

    @Test
    @DisplayName("Debería parsear correctamente un artículo por peso válido (ejemplo Banana)")
    void testParseValidItem() throws ParseException {
        // Ejemplo real del archivo de ticket
        List<String> lines = List.of(
                "1   BANANA",
                "        0,336 kg                   1,45 €/kg        0,49"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ItemByWeight item = ItemByWeight.parser(status);

        assertNotNull(item, "El item no debería ser null");
        assertEquals("BANANA", item.id(), "El ID debería ser BANANA");
        assertEquals(new BigDecimal("0.336"), item.pesoKg());
        assertEquals(new BigDecimal("1.45"), item.precioPorKilogramo());
        // El precioTotal() calculado por el record es pesoKg * precioPorKilogramo
        // 0.336 * 1.45 = 0.4872, que redondeado HALF_UP a 2 decimales es 0.49
        assertEquals(new BigDecimal("0.49"), item.precioCalculado());
    }

    @Test
    @DisplayName("Debería parsear correctamente un artículo con espacios extra y caracteres especiales")
    void testParseValidItemWithSpacesAndSpecialChars() throws ParseException {
        List<String> lines = List.of(
                "  1   PLÁTANO DE CANARIAS / EXTRA    ",
                "    1,200 kg   5,50 €/kg   6,60  "
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ItemByWeight item = ItemByWeight.parser(status);

        assertNotNull(item);
        assertEquals("PLÁTANO DE CANARIAS / EXTRA", item.id());
        assertEquals(new BigDecimal("1.200"), item.pesoKg());
        assertEquals(new BigDecimal("5.50"), item.precioPorKilogramo());
        assertEquals(new BigDecimal("6.60"), item.precioCalculado());
    }

    @Test
    @DisplayName("Debería devolver null si la primera línea no empieza por 1")
    void testParseReturnsNullForInvalidFirstLineStart() throws ParseException {
        List<String> lines = List.of(
                "2   BANANA",
                "        0,336 kg                   1,45 €/kg        0,49"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ItemByWeight item = ItemByWeight.parser(status);
        assertNull(item, "Debería devolver null si la línea no empieza por 1");
        // Verificar que el iterador volvió al principio
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería devolver null si la primera línea tiene un ID inválido (minúsculas)")
    void testParseReturnsNullForInvalidID() throws ParseException {
        List<String> lines = List.of(
                "1   banana",
                "        0,336 kg                   1,45 €/kg        0,49"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ItemByWeight item = ItemByWeight.parser(status);
        assertNull(item, "Debería devolver null si el ID tiene minúsculas (según REGEX_ID)");
        assertEquals(0, status.nextIndex());
    }

    @Test
    @DisplayName("Debería devolver null si la segunda línea no tiene el formato correcto (faltan kg o €/kg)")
    void testParseReturnsNullForInvalidSecondLineFormat() throws ParseException {
        List<String> lines1 = List.of(
                "1   BANANA",
                "        0,336                      1,45 €/kg        0,49" // Falta 'kg'
        );
        assertNull(ItemByWeight.parser(new ParserStatusInfo(String.join("\n", lines1))));

        List<String> lines2 = List.of(
                "1   BANANA",
                "        0,336 kg                   1,45             0,49" // Falta '€/kg'
        );
        assertNull(ItemByWeight.parser(new ParserStatusInfo(String.join("\n", lines2))));
    }

    @Test
    @DisplayName("Debería devolver null si los precios o pesos usan punto en lugar de coma")
    void testParseReturnsNullForPointsInsteadOfCommas() throws ParseException {
        List<String> lines = List.of(
                "1   BANANA",
                "        0.336 kg                   1.45 €/kg        0.49"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ItemByWeight item = ItemByWeight.parser(status);
        assertNull(item, "Debería devolver null si se usan puntos en lugar de comas");
    }

    @Test
    @DisplayName("Debería devolver null si faltan campos en la segunda línea")
    void testParseReturnsNullForMissingFields() throws ParseException {
        List<String> lines = List.of(
                "1   BANANA",
                "        0,336 kg                   0,49" // Falta precio/kg
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        ItemByWeight item = ItemByWeight.parser(status);
        assertNull(item);
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException para valores inválidos en el constructor")
    void testConstructorConstraints() {
        assertThrows(NullPointerException.class, () -> new ItemByWeight(null, new BigDecimal("1.000"), new BigDecimal("1.00"),new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new ItemByWeight("", new BigDecimal("1.000"), new BigDecimal("1.00"),new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new ItemByWeight("BANANA", BigDecimal.ZERO, new BigDecimal("1.00"),new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new ItemByWeight("BANANA", new BigDecimal("-0.500"), new BigDecimal("1.00"),new BigDecimal("1.00")));
        assertThrows(NullPointerException.class, () -> new ItemByWeight("BANANA", new BigDecimal("1.000"), null,null));
    }

    @Test
    @DisplayName("Debería calcular correctamente el precio total")
    void testPrecioCalculadoCalculation() {
        ItemByWeight item = new ItemByWeight("TEST", new BigDecimal("0.500"), new BigDecimal("2.00"),new BigDecimal("1.00"));
        assertEquals(new BigDecimal("1.00"), item.precioCalculado());

        // Caso con redondeo (0.336 * 1.45 = 0.4872 -> 0.49)
        item = new ItemByWeight("TEST", new BigDecimal("0.336"), new BigDecimal("1.45"),new BigDecimal("0.49"));
        assertEquals(new BigDecimal("0.49"), item.precioCalculado());
    }
}