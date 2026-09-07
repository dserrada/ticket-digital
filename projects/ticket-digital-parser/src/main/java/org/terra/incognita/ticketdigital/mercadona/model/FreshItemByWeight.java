package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FreshItemByWeight(String id, BigDecimal weightKg, BigDecimal pricePerKilogram, BigDecimal price, String freshType) implements PurchasedItem {
    private static final Logger logger = LoggerFactory.getLogger(FreshItemByWeight.class);

    protected static final Pattern FIRST_WEIGHT_PATTERN = Pattern.compile(
            "^\\s*" +
                    REGEX_ID +
                    "$");

    protected static final Pattern SECOND_WEIGHT_LINE = ItemByWeight.SECOND_WEIGHT_LINE;

    protected static final Pattern FRESH_TYPE_PATTERN = Pattern.compile("^\\s*(?<type>PESCADO)\\s*$");

    public FreshItemByWeight {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(weightKg, "weightKg must not be null");
        Objects.requireNonNull(pricePerKilogram, "pricePerKilogram must not be null");
        Objects.requireNonNull(freshType, "freshType must not be null");

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
     * Parsea un artículo fresco, devolviendo un item por llamada.
     *
     * La primera vez que se invoca sobre una sección de frescos, lee la línea de cabecera
     * (p.ej. "PESCADO") y la registra en {@code status}. En llamadas sucesivas, el estado
     * almacenado en {@code status} indica que ya estamos dentro de la sección.
     *
     * En cada llamada se leen las dos líneas del artículo (nombre + datos de peso). La sección
     * termina —y se devuelve null— cuando la indentación de la línea de nombre baja al nivel
     * de la cabecera o cuando el par de líneas no cumple el formato esperado. En ese caso la
     * línea que causó la salida se devuelve al iterador y se limpia el estado de la sección.
     *
     * @param status Estado del parseador; almacena la cabecera de la sección activa
     * @return El artículo fresco parseado, o null si no hay sección activa ni cabecera válida
     * @throws ParseException si el formato del ticket no es válido
     */
    public static FreshItemByWeight parse(ParserStatusInfo status) throws ParseException {
        if (!status.hasNext()) return null;

        if (!status.isInFreshSection()) {
            // Intentamos entrar en una nueva sección de frescos
            String headerLine = status.next();
            Matcher headerMatcher = FRESH_TYPE_PATTERN.matcher(headerLine);

            if (!headerMatcher.matches()) {
                status.rollback(1);
                return null;
            }

            String freshType = headerMatcher.group("type");
            int headerIndent = countLeadingSpaces(headerLine);
            status.enterFreshSection(freshType, headerIndent);
            logger.debug("Sección de frescos: type={}, indent={}", freshType, headerIndent);
        }

        // Leemos el siguiente artículo dentro de la sección activa
        String nameLine = status.next();
        int nameIndent = countLeadingSpaces(nameLine);

        if (nameIndent <= status.getFreshHeaderIndent()) {
            logger.debug("Fin de sección '{}' por indentación. Línea devuelta: [{}]",
                    status.getCurrentFreshType(), nameLine);
            status.rollback(1);
            status.exitFreshSection();
            return null;
        }

        Matcher nameMatcher = FIRST_WEIGHT_PATTERN.matcher(nameLine);
        if (!nameMatcher.matches()) {
            logger.debug("Fin de sección '{}': nombre [{}] no cumple el patrón",
                    status.getCurrentFreshType(), nameLine);
            status.rollback(1);
            status.exitFreshSection();
            return null;
        }

        if (!status.hasNext()) {
            status.rollback(1);
            status.exitFreshSection();
            return null;
        }

        String weightLine = status.next();
        Matcher weightMatcher = SECOND_WEIGHT_LINE.matcher(weightLine);
        if (!weightMatcher.matches()) {
            logger.debug("Fin de sección '{}': peso [{}] no cumple el patrón",
                    status.getCurrentFreshType(), weightLine);
            status.rollback(2);
            status.exitFreshSection();
            return null;
        }

        String id = nameMatcher.group("id").trim();
        String freshType = status.getCurrentFreshType();
        BigDecimal weightKg = PurchasedItem.parseWeight(weightMatcher.group("weight"));
        BigDecimal pricePerKilogram = PurchasedItem.parseUnitPrice(weightMatcher.group("pricePerKg"));
        BigDecimal price = PurchasedItem.parseUnitPrice(weightMatcher.group("price"));

        logger.debug("Artículo fresco parseado: id={}, type={}, weight={}, pricePerKg={}", id, freshType, weightKg, pricePerKilogram);
        return new FreshItemByWeight(id, weightKg, pricePerKilogram, price, freshType);
    }

    private static int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }
}
