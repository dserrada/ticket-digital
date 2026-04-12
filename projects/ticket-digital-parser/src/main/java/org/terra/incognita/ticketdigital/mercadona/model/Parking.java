package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Parking(LocalTime start, LocalTime end) {
    // INFO: En teoría no debo poner el día, porque no lo tengo en esta línea, pero a lo mejor sería conveniente
    public Parking {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times cannot be null");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Parking.class);

    private static final Pattern FIRST_PARKING_PATTERN = Pattern.compile(
            "^\\s*1 PARKING 0,00\\s*$");

    private static final Pattern SECOND_PARKING_LINE = Pattern.compile(
            "^\\s*ENTRADA\\s*(?<horaEntrada>[0-9]{2}:[0-9]{2})\\s+" +
                    "\\s*SALIDA\\s*(?<horaSalida>[0-9]{2}:[0-9]{2})\\s*$"

    );


    /**
     * Parsea dos lineas para obtener información del parking
     *
     * @param lines text line to parse
     * @return información del parking o null si no es un parking
     */
    public static Parking parse(final int position, final List<String> lines) throws ParseException {
        // Analizo las dos líneas que contienen toda la información
        // 1 PARKING 0,00
        //  ENTRADA  19:10       SALIDA  19:48

        String firstLine = lines.get(position);
        String secondLine = lines.get(position + 1);

        Matcher matcher1 = FIRST_PARKING_PATTERN.matcher(firstLine);
        Matcher matcher2 = SECOND_PARKING_LINE.matcher(secondLine);
        if ( !(matcher1.matches() && matcher2.matches()) ) {
            logger.debug("Line {} no es del tipo {}",firstLine, Parking.class.getSimpleName());
            return null;
        }


        logger.debug("Parsing parking, firstLine {}, secondLine: {}", firstLine, secondLine);

        // Por desgracia el matcher1 no tiene ninguna información
        LocalTime start = LocalTime.parse(matcher2.group("horaEntrada"));
        LocalTime end = LocalTime.parse(matcher2.group("horaSalida"));
        logger.debug("start {}, end {}", start, end);

        return new Parking(start, end);
    }



}
