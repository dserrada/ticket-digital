package org.terra.incognita.ticketdigital.mercadona.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purchase of a product sold by weight.
 *
 * @param id product name or identifier
 * @param pesoKg product weight in kilograms, with gram precision
 * @param precioPorKilogramo price per kilogram in euros, with cent precision
 */
public record ItemByWeight(String id, BigDecimal pesoKg, BigDecimal precioPorKilogramo) implements PurchasedItem {

    private static final Logger logger = LoggerFactory.getLogger(ItemByWeight.class);

    protected static final Pattern FIRST_WEIGHT_PATTERN = Pattern.compile(
            "^\\s*" +
                    "1" + "\\s+" +
                    REGEX_ID + "\\s*" +
                    "$");

    /**
     * Expresión regular de la segunda línea de un artículo vendido por peso.
     *
     * Ejemplo de línea que cumple esto es: "1 kg 2,50 €/kg 2,50"
     */
    protected static final Pattern SECOND_WEIGHT_LINE = Pattern.compile(
            "^\\s*" +
                    REGEX_PESO + "\\s+" +
                    REGEX_PRECIO_KG + "\\s+" +
                    REGEX_PRECIO + "\\s*$"

    );


    public ItemByWeight {
        Objects.requireNonNull(id, "nombre must not be null");
        Objects.requireNonNull(pesoKg, "pesoKg must not be null");
        Objects.requireNonNull(precioPorKilogramo, "precioPorKilogramo must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("nombre must not be blank");
        }
        if (pesoKg.signum() <= 0) {
            throw new IllegalArgumentException("pesoKg must be greater than zero");
        }

        pesoKg = pesoKg.setScale(3, RoundingMode.UNNECESSARY);
        precioPorKilogramo = precioPorKilogramo.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public BigDecimal precioTotal() {
        return pesoKg.multiply(precioPorKilogramo).setScale(2, RoundingMode.HALF_UP);
    }




    /**
     * Parses a text line and creates a weight-based purchase.
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return parsed weight purchase or null if the line is not a weight purchase
     */
    public static ItemByWeight parse(ParserStatusInfo status) throws ParseException {
        // Analizo las dos líneas que contienen toda la información
        // 1   BANANA
        //      0,336 kg                   1,45 €/kg        0,49
        // El primero es el número de unidades (entero) y el segundo, optativo, el precio por unidad

        if (!status.iterator().hasNext()) return null;
        String firstLine = status.iterator().next();
        if (!status.iterator().hasNext()) {
            status.iterator().previous();
            return null;
        }
        String secondLine = status.iterator().next();

        Matcher matcher1 = FIRST_WEIGHT_PATTERN.matcher(firstLine);
        Matcher matcher2 = SECOND_WEIGHT_LINE.matcher(secondLine);
        if ( !(matcher1.matches() && matcher2.matches()) ) {
            logger.debug("Line [{}] no es del tipo {} , matcher1: {}, matcher2: {}",firstLine, ItemByWeight.class.getSimpleName(), matcher1.matches(), matcher2.matches());
            status.iterator().previous();
            status.iterator().previous();
            return null;
        }


        logger.debug("Parsing weight item, firstLine {}, secondLine: {}", firstLine, secondLine);

        String id = matcher1.group("id").trim();
        String sPeso = matcher2.group("peso");
        BigDecimal pesoKg = PurchasedItem.parseWeight(sPeso);
        String sPrecioKg = matcher2.group("precioKg");
        BigDecimal precioPorKilogramo = PurchasedItem.parseUnitPrice(sPrecioKg);
        String sPrecio = matcher2.group("precio");
        BigDecimal precio = PurchasedItem.parseUnitPrice(sPrecio);
        logger.debug("pesoKg {}, precioKg {}, precio {}, id: [{}]", pesoKg, precioPorKilogramo, precio, id);

        return new ItemByWeight(id, pesoKg, precioPorKilogramo);
    }
}
