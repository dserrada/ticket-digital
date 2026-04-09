package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ItemByUnit class.
 * <p>
 * Specifically, these tests cover the `precioTotal` method, which calculates
 * the total price of a product sold by unit.
 */
public class ItemByUnitTest {

    // FIXME: Los tests auteogenerados son estupidos, borrar.
    @Test
    void testPrecioTotalWithValidSingleUnit() {
        // Arrange
        ItemByUnit item = new ItemByUnit("FRANKFURT VIENA QUES", 1, new BigDecimal("5.60"), new BigDecimal("5.60"));

        // Act
        BigDecimal totalPrice = item.precioTotal();

        // Assert
        assertEquals(new BigDecimal("5.60"), totalPrice, "Total price should match for single unit.");
    }


    @Test
    void testLinea() throws ParseException {
        String linea = "1 PAN BLANCO FAMILIAR 1,25";
        ItemByUnit item = ItemByUnit.parse(0, List.of(linea));
        assertNotNull(item);
    }

    @Test
    void testPrecioTotalWithValidMultipleUnits() {
        // Arrange
        ItemByUnit item = new ItemByUnit("FRANKFURT VIENA QUES", 3, new BigDecimal("2.80"), new BigDecimal("8.40"));

        // Act
        BigDecimal totalPrice = item.precioTotal();

        // Assert
        assertEquals(new BigDecimal("8.40"), totalPrice, "Total price should match for multiple units.");
    }

    @Test
    void testPrecioTotalThrowsExceptionOnRounding() {
        // Arrange
        ItemByUnit item = new ItemByUnit("INVALID ROUNDING", 3, new BigDecimal("2.333"), new BigDecimal("6.999"));

        // Act & Assert
        assertThrows(ArithmeticException.class, item::precioTotal, "Should throw ArithmeticException due to unnecessary rounding.");
    }

    @Test
    void testPrecioTotalWithZeroCentDifferenceInCalculation() {
        // Arrange
        ItemByUnit item = new ItemByUnit("PERFECT ROUNDING", 2, new BigDecimal("4.00"), new BigDecimal("8.00"));

        // Act
        BigDecimal totalPrice = item.precioTotal();

        // Assert
        assertEquals(new BigDecimal("8.00"), totalPrice, "The calculated price should match the manually specified total.");
    }
}