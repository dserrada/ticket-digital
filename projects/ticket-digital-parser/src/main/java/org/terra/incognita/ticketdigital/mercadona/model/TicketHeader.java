package org.terra.incognita.ticketdigital.mercadona.model;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecer del tickek con información general de la compra
 *
 * @param fechaCompra       La fecha de compra del ticket
 * @param OP                ¿código de coperaciones?
 * @param codigoFacturaSimplificada   El número de factura simplificada
 */
public record TicketHeader(LocalDateTime fechaCompra,
                           String OP,
                           String codigoFacturaSimplificada)  {


    public static final DateTimeFormatter MERCADONA_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final int EXPECTED_LINES = 2;

    public static TicketHeader parse(ParserStatusInfo status) throws ParseException {

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < EXPECTED_LINES; i++) {
            if (status.iterator().hasNext()) {
                lines.add(status.iterator().next());
            } else {
                for (int j = 0; j < lines.size(); j++) status.iterator().previous();
                throw new ParseException("Expected " + EXPECTED_LINES + " lines, got " + lines.size(), -1);
            }
        }
        // TODO: Mejor validarlas una a una para poder dar información de que linea falla
        boolean missingLine = lines.stream().anyMatch(line -> line == null || line.isBlank());
        if ( missingLine ) {
            throw new ParseException("Missing or blank lines in input", -1);
        }

        String line = lines.get(0).trim();
        String[] fechaHoraParts = line.split("\\s+");
        if (fechaHoraParts.length < 4) {
            throw new ParseException("Expected at least fechaCompra, horaCompra, 'OP:' and OP value in line: '" + line + "'",-1);
        }

        String fechaCompraStr = fechaHoraParts[0];
        String horaCompraStr = fechaHoraParts[1];

        if (!fechaHoraParts[2].equals("OP:")) {
            throw new ParseException("Expected 'OP:' at index 2 in line: '" + line + "'",-1);
        }

        String OP = fechaHoraParts[3];

        LocalDateTime fechaCompra = LocalDateTime.parse(fechaCompraStr + " " + horaCompraStr, MERCADONA_DATE_TIME_FORMAT);

        line = lines.get(1);
        String[] facturaParts = line.split(":");
        if (facturaParts.length < 2) {
            throw new ParseException("Expected ':' in factura simplificada line: '" + line + "'",-1);
        }
        String codigoFacturaSimplificada = facturaParts[1].trim();

        return new TicketHeader(fechaCompra, OP, codigoFacturaSimplificada);
    }

}
