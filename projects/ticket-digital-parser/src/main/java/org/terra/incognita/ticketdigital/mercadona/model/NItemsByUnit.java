package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Purchase of a product sold by units.
 *
 * @param id product name or identifier
 * @param quantity integer quantity, at least 1
 * @param unitPrice unit price in euros, with cent precision
 * @param price total price of the purchased units in euros, with cent precision
 */
public record NItemsByUnit(String id, int quantity, BigDecimal unitPrice, BigDecimal price) implements PurchasedItem {

    private static final Logger logger = LoggerFactory.getLogger(NItemsByUnit.class);

//    4 TORTILLA PAT C/CEB 1,5L 2,60 10,40
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s*" +
                    REGEX_QUANTITY + "\\s+" +
                    REGEX_ID + "\\s+" +
                    REGEX_UNIT_PRICE + "\\s+" +
                    REGEX_PRICE + "\\s*" +
                    "$");

    public NItemsByUnit {
        Objects.requireNonNull(id, "id must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
    }

    @Override
    public BigDecimal calculatedPrice() {
        if ( unitPrice == null ) {
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } else {
            return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.UNNECESSARY);
        }
    }


    /**
     * Parsea la información de una linea de compra por unidades
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return El item parseado o null si no coincide con el patrón
     */
    public static NItemsByUnit parse(ParserStatusInfo status) throws ParseException, UnsupportedOperationException  {
        if (!status.hasNext()) return null;
        String line = status.next();

        // La primera línea tiene que ser de alguna de las dos siguientes formas
        // 1   PANECILLO 11UDS                                 1,10
        // 2   FRANKFURT VIENA QUES                 2,80       5,60
        // El primero es el número de unidades (entero) y el segundo, optativo, el precio por unidad
        Matcher matcher = LINE_PATTERN.matcher(line);

        if (!matcher.matches()) {
            logger.debug("Line [{}] no es del tipo {}, regex: {}",line, NItemsByUnit.class.getSimpleName(), matcher.pattern());
            status.rollback(1);
            return null;
        }

        logger.debug("Parsing unitLine item, line {}", line);

        int quantity = Integer.parseInt(matcher.group("quantity"));
        String id = matcher.group("id").trim();

        BigDecimal unitPrice = null;
        BigDecimal price;
        if ( quantity > 1 ) {
            String unitPriceStr = matcher.group("unitPrice").trim();
            if (!unitPriceStr.isEmpty()) {
                unitPrice = PurchasedItem.parseUnitPrice(unitPriceStr);
            } else {
                throw new ParseException("Missing unit price in line: " + line, -1);
            }
        }

        price = PurchasedItem.parseUnitPrice(matcher.group("price"));
        logger.debug("quantity {}, id [{}], unitPrice {}, price {}", quantity, id, unitPrice, price);

        return new NItemsByUnit(id, quantity, unitPrice, price);

    }

}
