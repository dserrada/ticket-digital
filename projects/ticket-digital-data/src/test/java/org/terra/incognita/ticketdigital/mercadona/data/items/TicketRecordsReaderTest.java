package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketRecordsReaderTest {

    // Ticket real reutilizado de los recursos de test del parser (el directorio de trabajo de
    // los tests de Gradle es el del propio módulo).
    private static final Path SAMPLE_TICKET =
            Path.of("../ticket-digital-parser/src/test/resources/20230907 Mercadona 33,50 €.pdf");

    @Test
    void elMismoTicketEnDosFicherosSoloSeCuentaUnaVez(@TempDir Path dataDir) throws IOException {
        // Reproduce lo visto con el downloader de Gmail: el mismo PDF guardado dos veces, la
        // segunda con el id del mensaje como sufijo ("...-1a0fdcec7973cfd7.pdf").
        Files.copy(SAMPLE_TICKET, dataDir.resolve("20230907 Mercadona 33,50 €.pdf"));
        Files.copy(SAMPLE_TICKET, dataDir.resolve("20230907 Mercadona 33,50 €-1a0fdcec7973cfd7.pdf"));

        Path singleDir = Files.createDirectory(dataDir.resolve("single"));
        Files.copy(SAMPLE_TICKET, singleDir.resolve("20230907 Mercadona 33,50 €.pdf"));
        List<PurchasedItemRecord> expected = TicketRecordsReader.readAll(singleDir);
        Files.delete(singleDir.resolve("20230907 Mercadona 33,50 €.pdf"));

        List<PurchasedItemRecord> records = TicketRecordsReader.readAll(dataDir);

        assertEquals(expected, records);
    }
}
