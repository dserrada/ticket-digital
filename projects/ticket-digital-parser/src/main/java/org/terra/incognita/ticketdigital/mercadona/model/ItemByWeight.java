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
 * @param weightKg product weight in kilograms, with gram precision
 * @param pricePerKilogram price per kilogram in euros, with cent precision
 */
public record ItemByWeight(String id, BigDecimal weightKg, BigDecimal pricePerKilogram, BigDecimal price) implements PurchasedItem {

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
                    REGEX_WEIGHT + "\\s+" +
                    REGEX_PRICE_PER_KG + "\\s+" +
                    REGEX_PRICE + "\\s*$"

    );


    public ItemByWeight {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(weightKg, "weightKg must not be null");
        Objects.requireNonNull(pricePerKilogram, "pricePerKilogram must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (weightKg.signum() <= 0) {
            throw new IllegalArgumentException("weightKg must be greater than zero");
        }

        weightKg = weightKg.setScale(3, RoundingMode.UNNECESSARY);
        pricePerKilogram = pricePerKilogram.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public BigDecimal calculatedPrice() {
        return weightKg.multiply(pricePerKilogram).setScale(2, RoundingMode.HALF_UP);
    }




    /**
     * Parses a text line and creates a weight-based purchase.
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return parsed weight purchase or null if the line is not a weight purchase
     */
    public static ItemByWeight parse(ParserStatusInfo status) throws ParseException, UnsupportedOperationException  {
        // Analizo las dos líneas que contienen toda la información
        // 1   BANANA
        //      0,336 kg                   1,45 €/kg        0,49
        // El primero es el número de unidades (entero) y el segundo, optativo, el precio por unidad

        if (!status.hasNext()) return null;
        String firstLine = status.next();
        if (!status.hasNext()) {
            status.rollback(1);
            return null;
        }
        String secondLine = status.next();

        Matcher matcher1 = FIRST_WEIGHT_PATTERN.matcher(firstLine);
        Matcher matcher2 = SECOND_WEIGHT_LINE.matcher(secondLine);
        if ( !(matcher1.matches() && matcher2.matches()) ) {
            logger.debug("Line [{}] no es del tipo {} , matcher1: {}, matcher2: {}",firstLine, ItemByWeight.class.getSimpleName(), matcher1.matches(), matcher2.matches());
            status.rollback(2);
            return null;
        }


        logger.debug("Parsing weight item, firstLine {}, secondLine: {}", firstLine, secondLine);

        String id = matcher1.group("id").trim();
        String sWeight = matcher2.group("weight");
        BigDecimal weightKg = PurchasedItem.parseWeight(sWeight);
        String sPricePerKg = matcher2.group("pricePerKg");
        BigDecimal pricePerKilogram = PurchasedItem.parseUnitPrice(sPricePerKg);
        String sPrice = matcher2.group("price");
        BigDecimal price = PurchasedItem.parseUnitPrice(sPrice);
        logger.debug("weightKg {}, pricePerKg {}, price {}, id: [{}]", weightKg, pricePerKilogram, price, id);

        return new ItemByWeight(id, weightKg, pricePerKilogram, price);
    }
}
