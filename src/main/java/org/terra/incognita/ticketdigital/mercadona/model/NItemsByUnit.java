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
 * @param cantidad integer quantity, at least 1
 * @param precioPorUnidad unit price in euros, with cent precision
 * @param precio el precio de las unidades compradas en euros, con centimos
 */
public record NItemsByUnit(String id, int cantidad, BigDecimal precioPorUnidad, BigDecimal precio) implements PurchasedItem {

    private static final Logger logger = LoggerFactory.getLogger(NItemsByUnit.class);

//    4 TORTILLA PAT C/CEB 1,5L 2,60 10,40
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s*" +
                    REGEX_CANTIDAD + "\\s+" +
                    REGEX_ID + "\\s+" +
                    REGEX_PRECIO_UNIDAD + "\\s+" +
                    REGEX_PRECIO + "\\s*" +
                    "$");

    public NItemsByUnit {
        Objects.requireNonNull(id, "id must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (cantidad < 1) {
            throw new IllegalArgumentException("cantidad must be at least 1");
        }
    }

    @Override
    public BigDecimal precioTotal() {
        if ( precioPorUnidad == null ) {
            return precio.setScale(2, RoundingMode.UNNECESSARY);
        } else {
            return precioPorUnidad.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.UNNECESSARY);
        }
    }


    /**
     * Parsea la información de una linea de compra por unidades
     */
    public static NItemsByUnit parse(final int position, final List<String> linea) throws ParseException {
        String line = linea.get(position);
        // La primera línea tiene que ser de alguna de las dos siguientes formas
        // 1   PANECILLO 11UDS                                 1,10
        // 2   FRANKFURT VIENA QUES                 2,80       5,60
        // El primero es el número de unidades (entero) y el segundo, optativo, el precio por unidad
        Matcher matcher = LINE_PATTERN.matcher(line);

        if (!matcher.matches()) {
            logger.debug("Line [{}] no es del tipo {}, regex: {}",line, NItemsByUnit.class.getSimpleName(), matcher.pattern());
            return null;
        }

        logger.debug("Parsing unitLine item, line {}", line);

        int cantidad = Integer.parseInt(matcher.group("cantidad"));
        String id = matcher.group("id").trim();

        BigDecimal precioPorUnidad = null;
        BigDecimal precio = null;
        if ( cantidad > 1 ) {
            String precioPorUnidadStr = matcher.group("precioUnidad").trim();
            if (!precioPorUnidadStr.isEmpty()) {
                precioPorUnidad = PurchasedItem.parseUnitPrice(precioPorUnidadStr);
            } else {
                throw new ParseException("Missing unit price in line: " + line, -1);
            }
        }

        precio = PurchasedItem.parseUnitPrice(matcher.group("precio"));
        logger.debug("cantidad {}, id {}, precioPorUnidad {}, precio {}", cantidad, id, precioPorUnidad, precio);

        return new NItemsByUnit(id, cantidad, precioPorUnidad,precio);

    }

}
