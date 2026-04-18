package org.terra.incognita.ticketdigital.mercadona.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Interfaz para los objetos que representan productos comprados en una tienda
 */
public sealed interface PurchasedItem permits OneItemByUnit, ItemByWeight, NItemsByUnit, FreshItemByWeight{

    /**
     * The purchased products are all sold in euros.
     */
    public static String EURO_SYMBOL = "€";
    public static Locale SPANISH_LOCALE = Locale.of("es", "ES");
    public static NumberFormat SPANISH_PRICE_FORMAT = NumberFormat.getCurrencyInstance(SPANISH_LOCALE);

    public static String REGEX_CANTIDA  D = "\\s*(?<cantidad>\\d+)";
    public static String REGEX_ID = "(.*?)(?<id>[0-9A-ZÑÁÉÍÓÚ`,%\\+\\/\\.\\s\\-]+)";
    public static String REGEX_PRECIO_UNIDAD = "(?<precioUnidad>\\d*,\\d{2})";
    public static String REGEX_PRECIO = "(?<precio>\\d*,\\d{2})";
    public static String REGEX_PESO = "(?<peso>[0-9]+(?:[\\.,][0-9]{1,3})?)\\s+kg";
    public static String REGEX_PRECIO_KG = "(?<precioKg>[0-9]+(?:[\\.,][0-9]{1,2})?)\\s*€\\/kg";


    /**
     * Nombre o identificador del producto comparado
     *
     * Debe existir en todos las clases (records) que lo implementen un dato id que corresponda con este
     */
    String id();

    /**
     * El precio total del producto comprado. Este campo es extraido del ticket
     *
     * Debe existir en todos las clases (records) que lo implementen un dato precio que corresponda con este
     */
    BigDecimal precio();

    /**
     * Precio total del producto comparado, en euros y con precisión de centimos.
     * Se cálcula a partir del precio unitario o por kg y la cantidad de producto comprado.
     * Debe ser igual al precio que aparece en el ticket (campo precio)
     */
    BigDecimal precioCalculado();

    static BigDecimal parseUnitPrice(String precio) {
        return new BigDecimal(precio.replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
    }

    static BigDecimal parseWeight(String precio) {
        return new BigDecimal(precio.replace(',', '.')).setScale(3, RoundingMode.UNNECESSARY);
    }

    static String printSpanishPrice(BigDecimal price) {
        return SPANISH_PRICE_FORMAT.toString() + " " + EURO_SYMBOL;
    }



}
