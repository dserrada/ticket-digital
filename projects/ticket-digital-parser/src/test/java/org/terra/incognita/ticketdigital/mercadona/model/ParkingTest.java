package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Parking class, focusing on the parser method.
 */
class ParkingTest {

    @Test
    @DisplayName("Debería parsear correctamente un parking válido")
    void testParseValidParking() throws ParseException {
        // Ejemplo típico
        List<String> lines = List.of(
                "1 PARKING 0,00",
                "ENTRADA 19:10       SALIDA 19:48"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        Parking parking = Parking.parser(status);

        assertNotNull(parking, "El parking no debería ser null");
        assertEquals(LocalTime.of(19, 10), parking.start());
        assertEquals(LocalTime.of(19, 48), parking.end());
    }

    @Test
    @DisplayName("Debería parsear correctamente un parking con espacios adicionales")
    void testParseValidParkingWithSpaces() throws ParseException {
        List<String> lines = List.of(
                "  1 PARKING 0,00  ",
                "   ENTRADA   09:30   SALIDA   10:15   "
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        Parking parking = Parking.parser(status);

        assertNotNull(parking);
        assertEquals(LocalTime.of(9, 30), parking.start());
        assertEquals(LocalTime.of(10, 15), parking.end());
    }

    @Test
    @DisplayName("Debería devolver null si la primera línea no coincide")
    void testParseReturnsNullForInvalidFirstLine() throws ParseException {
        List<String> lines = List.of(
                "1 PARKING 1,50", // El parking siempre parece ser 0,00 según el regex
                "ENTRADA 19:10 SALIDA 19:48"
        );
        ParserStatusInfo status = new ParserStatusInfo(String.join("\n", lines));
        Parking parking = Parking.parser(status);
        assertNull(parking, "Debería ser null si el precio del parking no es 0,00");
        assertEquals(0, status.nextIndex());

        lines = List.of(
                "2 PARKING 0,00", // No empieza por 1
                "ENTRADA 19:10 SALIDA 19:48"
        );
        assertNull(Parking.parser(new ParserStatusInfo(String.join("\n", lines))));
    }

    @Test
    @DisplayName("Debería devolver null si la segunda línea no coincide")
    void testParseReturnsNullForInvalidSecondLine() throws ParseException {
        List<String> lines = List.of(
                "1 PARKING 0,00",
                "ENTRADA 19:10" // Falta la salida
        );
        assertNull(Parking.parser(new ParserStatusInfo(String.join("\n", lines))));

        lines = List.of(
                "1 PARKING 0,00",
                "INGRESO 19:10 SALIDA 19:48" // Palabra INGRESO en vez de ENTRADA
        );
        assertNull(Parking.parser(new ParserStatusInfo(String.join("\n", lines))));
        
        lines = List.of(
                "1 PARKING 0,00",
                "ENTRADA 7:10 SALIDA 7:48" // Formato de hora sin dos dígitos (H:MM en vez de HH:MM)
        );
        assertNull(Parking.parser(new ParserStatusInfo(String.join("\n", lines))));
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si los tiempos son nulos en el constructor")
    void testConstructorConstraints() {
        assertThrows(IllegalArgumentException.class, () -> new Parking(null, LocalTime.now()));
        assertThrows(IllegalArgumentException.class, () -> new Parking(LocalTime.now(), null));
    }

    @Test
    @DisplayName("Debería devolver null si no hay suficientes líneas")
    void testParseReturnsNullForMissingSecondLine() throws ParseException {
        List<String> lines = List.of("1 PARKING 0,00");
        assertNull(Parking.parser(new ParserStatusInfo(String.join("\n", lines))));
    }
}
