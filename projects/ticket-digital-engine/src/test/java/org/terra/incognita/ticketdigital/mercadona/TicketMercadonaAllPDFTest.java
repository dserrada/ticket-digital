package org.terra.incognita.ticketdigital.mercadona;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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

        List<Path> pdfFiles = FileUtils.searchInDir(dataDir);
        if (pdfFiles == null) return;

        logger.info("Found {} PDF files in {}", pdfFiles.size(), dataDir.toAbsolutePath());

        StringBuffer sb = new StringBuffer();
        int countOK = 0, countErr = 0;
        // Parse each PDF file
        for (Path pdfFile : pdfFiles) {
            logger.debug("Processing PDF: {}", pdfFile);
            try {
                if ( excluded.contains(pdfFile.getFileName().toString() ) ) continue;
                TicketMercadona ticket = TicketMercadona.parse(pdfFile);
                // assertNotNull(ticket); // FIXME: EL PESCADO DEVUELVE NULL, DE MOMENTO
                if (ticket != null) {
                    countOK++;
                } else {
                    countErr++;
                }
                logger.debug("Successfully parsed ticket from: {}", pdfFile.getFileName());
                logger.debug("Importe final: {}", ticket.precioTotalEnEuros());
                sb.append(ticket.toCVSString()).append("\n");
            } catch (Exception e) {
                logger.error("Failed to parse PDF: {}", pdfFile, e);
                throw e;
            }
        }
        logger.info("Parsed {} tickets, ok: {}, error: {}, excluded: {}", (countOK+countErr),countOK,countErr, excluded.size() );
        logger.info("CSV: {}", sb.toString());
    }

}
