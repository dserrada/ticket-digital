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
    // 20230915 Mercadona 75,51 €.pdf
    public void pruebaFicheroAllPDF() throws Exception {
        Path dataDir = Path.of("../../../../GMailExtractor/mails/");
        Set<String> excluded = Set.of( // PESCADO MULTIPLE
                "20230915 Mercadona 75,51 €.pdf","20231013 Mercadona 198,28 €.pdf","20231117 Mercadona 95,51 €.pdf",
                "20240223 Mercadona 60,99 €.pdf","20240517 Mercadona 64,85 €.pdf","20241108 Mercadona 77,71 €.pdf",
                "20250926 Mercadona 73,32 €.pdf");
        List<PurchasedItem> items = extractFromPDF(dataDir,excluded);
        logger.info("Parsed {} items", items.size());

    }

}
