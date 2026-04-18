package org.terra.incognita.ticketdigital.mercadona;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.PurchasedItem;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.terra.incognita.ticketdigital.mercadona.ItemsAggregator.extractFromPDF;

public class TicketMercadonaAllPDFTest {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadonaAllPDFTest.class);

    @Test
    public void pruebaFicheroAllPDF() throws Exception {
        Path dataDir = Path.of("../../../../GMailExtractor/mails/");
        List<PurchasedItem> items = extractFromPDF(dataDir);
        logger.info("Parsed {} items", items.size());

    }

}
