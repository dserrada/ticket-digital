package org.terra.incognita.ticketdigital.mercadona.model;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecer del tickek con información general de la compra
 *
 * @param purchaseDate       La fecha de compra del ticket
 * @param operationCode      ¿código de coperaciones?
 * @param simplifiedInvoiceNumber   El número de factura simplificada
 */
public record TicketHeader(LocalDateTime purchaseDate,
                           String operationCode,
                           String simplifiedInvoiceNumber)  {


    public static final DateTimeFormatter MERCADONA_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final int EXPECTED_LINES = 2;

    public static TicketHeader parse(ParserStatusInfo status) throws ParseException, UnsupportedOperationException  {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < EXPECTED_LINES; i++) {
            if (status.hasNext()) {
                lines.add(status.next());
            } else {
                status.rollback(lines.size());
                throw new ParseException("Expected " + EXPECTED_LINES + " lines, got " + lines.size(), -1);
            }
        }
        // TODO: Mejor validarlas una a una para poder dar información de que linea falla
        boolean missingLine = lines.stream().anyMatch(line -> line == null || line.isBlank());
        if ( missingLine ) {
            throw new ParseException("Missing or blank lines in input", -1);
        }

        String line = lines.get(0).trim();
        String[] dateTimeParts = line.split("\\s+");
        if (dateTimeParts.length < 4) {
            throw new ParseException("Expected at least purchaseDate, purchaseTime, 'OP:' and OP value in line: '" + line + "'",-1);
        }

        String purchaseDateStr = dateTimeParts[0];
        String purchaseTimeStr = dateTimeParts[1];

        if (!"OP:".equals(dateTimeParts[2])) {
            throw new ParseException("Expected 'OP:' at index 2 in line: '" + line + "'",-1);
        }

        String operationCode = dateTimeParts[3];

        LocalDateTime purchaseDate = LocalDateTime.parse(purchaseDateStr + " " + purchaseTimeStr, MERCADONA_DATE_TIME_FORMAT);

        line = lines.get(1);
        String[] invoiceParts = line.split(":");
        if (invoiceParts.length < 2) {
            throw new ParseException("Expected ':' in factura simplificada line: '" + line + "'",-1);
        }
        String simplifiedInvoiceNumber = invoiceParts[1].trim();

        return new TicketHeader(purchaseDate, operationCode, simplifiedInvoiceNumber);
    }

}
