package org.terra.incognita.ticketdigital.mercadona.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Interfaz para los objetos que representan productos comprados en una tienda
 */
public sealed interface PurchasedItem permits ItemByUnit, ItemByWeight {

    /**
     * The purchased products are all sold in euros.
     */
    public static String EURO_SYMBOL = "€";


    /**
     * Nombre o identificador del producto comparado
     */
    String id();

    /**
     * Precio total del producto comparado, en euros y con precisión de centimos
     */
    BigDecimal precioTotal();

    static BigDecimal parseUnitPrice(String precio) {
        return new BigDecimal(precio.replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
    }

    static BigDecimal parseWeight(String precio) {
        return new BigDecimal(precio.replace(',', '.')).setScale(3, RoundingMode.UNNECESSARY);
    }



}
