// File: ItemByWeightTest.java
package org.terra.incognita.ticketdigital.mercadona.model;

import jdk.jfr.Enabled;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemByWeightTest {

    /**
     * Test for parsing a valid weight item.
     */
    @Test
    void testParseValidItem() throws ParseException {
        List<String> lines = List.of(
                "    1   BANANA",
                "    0,336 kg                   1,45 €/kg        0,49"
        );

        ItemByWeight item = ItemByWeight.parse(0, lines);

        assertEquals("BANANA", item.id());
        assertEquals(new BigDecimal("0.336"), item.pesoKg());
        assertEquals(new BigDecimal("1.45"), item.precioPorKilogramo());
        assertEquals(new BigDecimal("0.49"), item.precioTotal());
    }


    /**
     * Test for parsing a valid weight item.
     */
    @Test
    @Disabled(" Los espacios del final los debe quitar")
    void testParseValidItem2() throws ParseException {
        List<String> lines = List.of(
                "    1   PLÁTANO DE CANARIAS      ",
                "    0,336 kg                   1235,45 €/kg        415,11"
        );

        ItemByWeight item = ItemByWeight.parse(0, lines);

        assertEquals("PLÁTANO DE CANARIAS", item.id());
        assertEquals(new BigDecimal("0.336"), item.pesoKg());
        assertEquals(new BigDecimal("1235.45"), item.precioPorKilogramo());
        assertEquals(new BigDecimal("415.11"), item.precioTotal());
    }

    /**
     * Test for invalid first line in parse method.
     */
    @Test
    void testParseInvalidFirstLine() {
        List<String> lines = List.of(
                "INVALID LINE",
                "    0,336 kg                   1,45 €/kg        0,49"
        );

        ParseException exception = assertThrows(ParseException.class, () -> ItemByWeight.parse(0, lines));
        assertTrue(exception.getMessage().contains("Invalid weight format in first line"));
    }

    /**
     * Test for invalid second line in parse method.
     */
    @Test
    void testParseInvalidSecondLine() {
        List<String> lines = List.of(
                "1   BANANA",
                "INVALID SECOND LINE"
        );

        ParseException exception = assertThrows(ParseException.class, () -> ItemByWeight.parse(0, lines));
        assertTrue(exception.getMessage().contains("Invalid weight format in second line"));
    }

    /**
     * Test for isItemByWeight with valid lines.
     */
    @Test
    void testIsItemByWeightValid() {
        List<String> lines = List.of(
                "     1   BANANA",
                "    0,336 kg                   1,45 €/kg        0,49"
        );

        boolean result = ItemByWeight.isItemByWeight(0, lines);
        assertTrue(result);
    }
    
    

    /**
     * Test for isItemByWeight with invalid first line.
     */
    @Test
    void testIsItemByWeightInvalidFirstLine() {
        List<String> lines = List.of(
                "    INVALID LINE",
                "      0,336 kg                   1,45 €/kg        0,49"
        );

        boolean result = ItemByWeight.isItemByWeight(0, lines);
        assertFalse(result);
    }

    /**
     * Test for isItemByWeight with invalid second line.
     */
    @Test
    void testIsItemByWeightInvalidSecondLine() {
        List<String> lines = List.of(
                "   1   BANANA",
                "     INVALID SECOND LINE"
        );

        boolean result = ItemByWeight.isItemByWeight(0, lines);
        assertFalse(result);
    }

    /**
     * Test for constructor when weight is zero.
     */
    @Test
    void testConstructorInvalidWeightZero() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ItemByWeight("BANANA", BigDecimal.ZERO, new BigDecimal("1.45")));
        assertTrue(exception.getMessage().contains("pesoKg must be greater than zero"));
    }

    /**
     * Test for constructor when weight is negative.
     */
    @Test
    void testConstructorInvalidNegativeWeight() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ItemByWeight("BANANA", new BigDecimal("-1.00"), new BigDecimal("1.45")));
        assertTrue(exception.getMessage().contains("pesoKg must be greater than zero"));
    }

    /**
     * Test for constructor when id is null.
     */
    @Test
    void testConstructorIdNull() {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
                new ItemByWeight(null, new BigDecimal("0.336"), new BigDecimal("1.45")));
        assertTrue(exception.getMessage().contains("nombre must not be null"));
    }

    /**
     * Test for constructor when id is blank.
     */
    @Test
    void testConstructorIdBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ItemByWeight("   ", new BigDecimal("0.336"), new BigDecimal("1.45")));
        assertTrue(exception.getMessage().contains("nombre must not be blank"));
    }

    /**
     * Test for constructor when precioPorKilogramo is null.
     */
    @Test
    void testConstructorPrecioPorKilogramoNull() {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
                new ItemByWeight("BANANA", new BigDecimal("0.336"), null));
        assertTrue(exception.getMessage().contains("precioPorKilogramo must not be null"));
    }
}