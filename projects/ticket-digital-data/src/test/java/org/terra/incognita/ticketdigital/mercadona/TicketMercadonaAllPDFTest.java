package org.terra.incognita.ticketdigital.mercadona;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.items.CSVGenerator;
import org.terra.incognita.ticketdigital.mercadona.model.PurchasedItem;

import java.nio.file.Path;

public class TicketMercadonaAllPDFTest {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadonaAllPDFTest.class);

    @Test
    public void pruebaFicheroAllPDF() throws Exception {
        Path dataDir = Path.of("../../../../GMailExtractor/mails/");
        CSVGenerator.writeCSVToFile(dataDir, Path.of("data-output.csv").toFile());
        logger.info("Finished");

    }

}
