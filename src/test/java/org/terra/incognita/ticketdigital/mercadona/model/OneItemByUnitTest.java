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
public class OneItemByUnitTest {

    // FIXME: Los tests auteogenerados son estupidos, borrar.
    @Test
    void testPrecioTotalWithValidSingleUnit() {
        // Arrange
        OneItemByUnit item = new OneItemByUnit("FRANKFURT VIENA QUES", 1, new BigDecimal("5.60"), new BigDecimal("5.60"));

        // Act
        BigDecimal totalPrice = item.precioTotal();

        // Assert
        assertEquals(new BigDecimal("5.60"), totalPrice, "Total price should match for single unit.");
    }


    @Test
    void testLinea1() throws ParseException {
        String linea = "1 PAN BLANCO FAMILIAR 1,25";
        OneItemByUnit item = OneItemByUnit.parse(0, List.of(linea));
        assertNotNull(item);
    }

    @Test
    void testLinea2() throws ParseException {
        String linea = "1 SOLOMILLO CERDO 3,05";
        OneItemByUnit item = OneItemByUnit.parse(0, List.of(linea));
        assertNotNull(item);
        linea = "4 TORTILLA PAT C/CEB 1,5L 2,60 10,40";
        item = OneItemByUnit.parse(0, List.of(linea));
        assertNotNull(item);
    }






    @Test
    void testPrecioTotalWithValidMultipleUnits() {
        // Arrange
        OneItemByUnit item = new OneItemByUnit("FRANKFURT VIENA QUES", 3, new BigDecimal("2.80"), new BigDecimal("8.40"));

        // Act
        BigDecimal totalPrice = item.precioTotal();

        // Assert
        assertEquals(new BigDecimal("8.40"), totalPrice, "Total price should match for multiple units.");
    }

    @Test
    void testPrecioTotalThrowsExceptionOnRounding() {
        // Arrange
        OneItemByUnit item = new OneItemByUnit("INVALID ROUNDING", 3, new BigDecimal("2.333"), new BigDecimal("6.999"));

        // Act & Assert
        assertThrows(ArithmeticException.class, item::precioTotal, "Should throw ArithmeticException due to unnecessary rounding.");
    }

    @Test
    void testPrecioTotalWithZeroCentDifferenceInCalculation() {
        // Arrange
        OneItemByUnit item = new OneItemByUnit("PERFECT ROUNDING", 2, new BigDecimal("4.00"), new BigDecimal("8.00"));

        // Act
        BigDecimal totalPrice = item.precioTotal();

        // Assert
        assertEquals(new BigDecimal("8.00"), totalPrice, "The calculated price should match the manually specified total.");
    }
}