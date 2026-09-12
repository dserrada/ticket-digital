package org.terra.incognita.ticketdigital.mercadona;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.items.CSVGenerator;
import org.terra.incognita.ticketdigital.mercadona.data.items.XlsxDatosWriter;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TicketMercadonaAllPDFTest {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadonaAllPDFTest.class);

    // Prueba manual con mis tickets reales, fuera del repo: solo existe en mi máquina,
    // así que se omite (no falla) en cualquier otro checkout, incluido CI.
    @Test
    void pruebaFicheroAllPDF() {
        Path dataDir = Path.of("../../../../GMailExtractor/mails/");
        assumeTrue(Files.isDirectory(dataDir), () -> "No existe " + dataDir.toAbsolutePath() + ", se omite la prueba");
        assertDoesNotThrow(() -> {
            CSVGenerator.writeCSVToFile(dataDir, Path.of("data-output.csv").toFile());
            XlsxDatosWriter.writeXlsxToFile(dataDir, Path.of("data-output.xlsx").toFile());
        });
        logger.info("Finished");
    }

}
