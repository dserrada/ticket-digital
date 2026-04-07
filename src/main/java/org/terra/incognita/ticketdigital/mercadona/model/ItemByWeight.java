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

    private static final Pattern FIRST_WEIGHT_PATTERN = Pattern.compile(
            "^\\s*1\\s+" +
                    "(?<id>[0-9A-ZÑÁÉÍÓÚ\\s\\-]+)\\s*$");

    /**
     * Expresión regular de la segunda línea de un artículo vendido por peso.
     *
     * Ejemplo de línea que cumple esto es: "1 kg 2,50 €/kg 2,50"
     */
    private static final Pattern SECOND_WEIGHT_LINE = Pattern.compile(
            "^\\s*(?<peso>[0-9]+(?:[\\.,][0-9]{1,3})?)\\s+kg\\s+" +
                    "(?<precioKg>[0-9]+(?:[\\.,][0-9]{1,2})?)\\s*€\\/kg\\s+" +
                    "(?<importe>[0-9]+(?:[\\.,][0-9]{1,2})?)\\s*$"

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


    public static boolean isItemByWeight(final int position, final List<String> lines) {
        // 1   BANANA
        //      0,336 kg                   1,45 €/kg        0,49
        // El primero es el número de unidades (entero) y el segundo, optativo, el precio por unidad
        String firstLine = lines.get(position);
        String secondLine = lines.get(position + 1);
        return FIRST_WEIGHT_PATTERN.matcher(firstLine).matches() && SECOND_WEIGHT_LINE.matcher(secondLine).matches();
    }


    /**
     * Parses a text line and creates a weight-based purchase.
     * 
     * <p>Implementation pending.</p>
     *
     * @param lines text line to parse
     * @return parsed weight purchase
     */
    public static ItemByWeight parse(final int position, final List<String> lines) throws ParseException {
        // Analizo las dos líneas que contienen toda la información
        // En la primera linea el producto
        String firstLine = lines.get(position);
        String secondLine = lines.get(position + 1);

        logger.debug("Parsing weight item, firstLine {}, secondLine: {}", firstLine, secondLine);

        Matcher matcher1 = FIRST_WEIGHT_PATTERN.matcher(firstLine);
        if ( !matcher1.matches() ) {
            // FIXME: Necesario para que los grupos estén rellenos
            throw new ParseException("Invalid weight format in first line: " + firstLine, position);
        }
        String id = matcher1.group("id");

        Matcher matcher2 = SECOND_WEIGHT_LINE.matcher(secondLine);
        if ( !matcher2.matches() ) {
            // FIXME: Necesario para que los grupos estén rellenos
            throw new ParseException("Invalid weight format in second line: " + secondLine, position + 1);
        }
        String sPeso = matcher2.group("peso");
        BigDecimal pesoKg = PurchasedItem.parseWeight(sPeso);
        String sPrecioKg = matcher2.group("precioKg");
        BigDecimal precioPorKilogramo = PurchasedItem.parseUnitPrice(sPrecioKg);
        String sImporte = matcher2.group("importe");
        BigDecimal importe = PurchasedItem.parseUnitPrice(sImporte);

        return new ItemByWeight(id, pesoKg, precioPorKilogramo);
    }
}
