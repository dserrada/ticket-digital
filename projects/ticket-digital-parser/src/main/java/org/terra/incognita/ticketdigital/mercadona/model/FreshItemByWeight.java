package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// INFO: Debería externder ItemByWeight pero Java no permite la externsión de records
// Este código SMELLS porque es una copia de ItemByWeight, pero con un constructor diferente
public record FreshItemByWeight(String id, BigDecimal pesoKg, BigDecimal precioPorKilogramo, BigDecimal precio, String freshType) implements PurchasedItem {
    private static final Logger logger = LoggerFactory.getLogger(FreshItemByWeight.class);

    protected static final Pattern FIRST_WEIGHT_PATTERN = Pattern.compile(
            "^\\s*" +
                    REGEX_ID +
                    "$");

    protected static final Pattern SECOND_WEIGHT_LINE = ItemByWeight.SECOND_WEIGHT_LINE;

    // De momento solo es pescado
    protected static final Pattern FRESH_TYPE_PATTERN = Pattern.compile("^\\s*(?<tipo>PESCADO)\\s*$");


    public FreshItemByWeight {
        Objects.requireNonNull(id, "nombre must not be null");
        Objects.requireNonNull(pesoKg, "pesoKg must not be null");
        Objects.requireNonNull(precioPorKilogramo, "precioPorKilogramo must not be null");
        Objects.requireNonNull(freshType, "freshType must not be null");

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
    public BigDecimal precioCalculado() {
        return pesoKg.multiply(precioPorKilogramo).setScale(2, RoundingMode.HALF_UP);
    }




    /**
     * Parses a text line and creates a weight-based purchase.
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return parsed weight purchase or null if the line is not a weight purchase
     */
    public static FreshItemByWeight parse(ParserStatusInfo status) throws ParseException, UnsupportedOperationException {
        // Analizo las dos líneas que contienen toda la información
        // PESCADO
        //     SALMON ENTERO
        //      0,336 kg                   1,45 €/kg        0,49
        // El primero es el número de unidades (entero) y el segundo, optativo, el precio por unidad
        // Pero ojo, agrupa, y puedo tener lo siguiente
        // PESCADO
        //    LUBINA
        //  0,806 kg 7,95 €/kg 6,41
        //    SALMON ENTERO
        //  1,454 kg 9,95 €/kg 14,47
        //    LANGOSTINO COCIDO
        //  0,536 kg 10,95 €/kg 5,87

        if (!status.hasNext()) return null;
        String freshTypeLine = status.next();
        if (!status.hasNext()) {
            status.rollback(1);
            return null;
        }
        String firstLine = status.next();
        if (!status.hasNext()) {
            status.rollback(2);
            return null;
        }
        String secondLine = status.next();

        Matcher matcherType = FRESH_TYPE_PATTERN.matcher(freshTypeLine);
        /*
        Matcher matcher1 = FIRST_WEIGHT_PATTERN.matcher(firstLine);
        Matcher matcher2 = SECOND_WEIGHT_LINE.matcher(secondLine);

        if ( !(matcherType.matches() && matcher1.matches() && matcher2.matches()) ) {
            logger.debug("Line [{}] no es del tipo {} , matcherType: {}, matcher1: {}, matcher2: {}", freshTypeLine, FreshItemByWeight.class.getSimpleName(), matcherType.matches(), matcher1.matches(), matcher2.matches());
            status.rollback(3);
            return null;
        }


        logger.debug("Parsing weight item, typeLine: {} firstLine {}, secondLine: {}", freshTypeLine,  firstLine, secondLine);

        String freshType = matcherType.group("tipo");
        // Realmente todavía no se como implementar esta información, teniendo en cuenta que solo me afecta a 9 ficheros
        // (de 500)
        // Los ficheros con datos erroeneos son
        String [] pdfsWithThisData = {"20230915 Mercadona 75,51 €.pdf",
                "20231013 Mercadona 198,28 €.pdf",
                "20231117 Mercadona 95,51 €.pdf",
                "20240223 Mercadona 60,99 €.pdf",
                "20240517 Mercadona 64,85 €.pdf",
                "20241108 Mercadona 77,71 €.pdf",
                "20250926 Mercadona 73,32 €.pdf"};
        // De momento damos como que no somos capaces de parsear estos datos, es tarea
        // del que lo llama ignorar el fichero en este caso
         */
        if ( matcherType.matches() ) {
            throw new UnsupportedOperationException("No se puede parsear el tipo de producto en la linea:  " +
                    freshTypeLine);
        } else {
            logger.debug("Line [{}] no es del tipo {}",freshTypeLine, FreshItemByWeight.class.getSimpleName());
            status.rollback(1);
            return null;
        }

    }
}
