package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TicketHeader class, focusing on the parser method.
 */
class TicketHeaderTest {

    @Test
    @DisplayName("Debería parsear correctamente una cabecera de ticket válida")
    void testParseValidTicketHeader() throws ParseException {
        // Datos basados en el archivo de ejemplo
        List<String> lines = List.of(
                "           01/01/2023 09:00 OP: 257136",
                "       FACTURA SIMPLIFICADA: 4567-891-113122"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        TicketHeader header = TicketHeader.parser(status);

        assertNotNull(header);
        assertEquals(LocalDateTime.of(2023, 1, 1, 9, 0), header.fechaCompra());
        assertEquals("257136", header.OP());
        assertEquals("4567-891-113122", header.codigoFacturaSimplificada());
    }

    @Test
    @DisplayName("Debería lanzar ParseException si faltan líneas")
    void testParseThrowsExceptionForMissingLines() {
        List<String> lines = List.of(
                "01/01/2023 09:00 OP: 257136"
                // Falta la 2ª línea
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> TicketHeader.parser(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si hay líneas vacías")
    void testParseThrowsExceptionForBlankLines() {
        List<String> lines = List.of(
                "  ", // Línea en blanco
                "FACTURA SIMPLIFICADA: 4567-891-113122"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        assertThrows(ParseException.class, () -> TicketHeader.parser(status));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si faltan partes en la línea de fecha/hora")
    void testParseThrowsExceptionForInvalidDateTimeLine() {
        List<String> lines = List.of(
                "01/01/2023 09:00 OP:", // Falta el valor de OP
                "FACTURA SIMPLIFICADA: 4567-891-113122"
        );
        assertThrows(ParseException.class, () -> TicketHeader.parser(new ParserStatusInfo(String.join("\n", lines))));

        List<String> lines2 = List.of(
                "01/01/2023 09:00 257136", // Falta "OP:"
                "FACTURA SIMPLIFICADA: 4567-891-113122"
        );
        assertThrows(ParseException.class, () -> TicketHeader.parser(new ParserStatusInfo(String.join("\n", lines2))));
    }

    @Test
    @DisplayName("Debería lanzar DateTimeParseException para formatos de fecha inválidos")
    void testParseThrowsExceptionForInvalidDateFormat() {
        List<String> lines = List.of(
                "01-01-2023 09:00 OP: 257136", // Guiones en vez de barras
                "FACTURA SIMPLIFICADA: 4567-891-113122"
        );
        assertThrows(DateTimeParseException.class, () -> TicketHeader.parser(new ParserStatusInfo(String.join("\n", lines))));
    }

    @Test
    @DisplayName("Debería lanzar ParseException si falta ':' en la línea de factura")
    void testParseThrowsExceptionForInvalidFacturaLine() {
        List<String> lines = List.of(
                "01/01/2023 09:00 OP: 257136",
                "FACTURA SIMPLIFICADA 4567-891-113122" // Falta ':'
        );
        assertThrows(ParseException.class, () -> TicketHeader.parser(new ParserStatusInfo(String.join("\n", lines))));
    }
}
