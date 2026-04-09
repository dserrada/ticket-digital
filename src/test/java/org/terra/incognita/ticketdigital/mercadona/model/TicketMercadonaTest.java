package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketMercadonaTest {

    /**
     * El formato de las fechas en los tickets del mercadona.
     * FIXME: Realmente no debe estar aquí, pero dentro de un record no se puede poner
     */
    public static DateTimeFormatter MERCADONA_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadonaTest.class);


    @Test
    public void pruebaFichero() throws Exception {
        // Simplemente verificamos que no explota
        TicketMercadona ticket = TicketMercadona.parse(Path.of("src/test/resources/20230101090000 Ticket Digital Mercadona.txt"));
        assertNotNull(ticket);
        logger.debug("Ticket parseado: {}", ticket);
        logger.debug("Importe final: {}", ticket.precioTotalEnEuros());
        // TODO: Comprobar que lo que ha leido sea correcto
    }


    @Test
    public void pruebaFicheroPDF() throws Exception {
        // Simplemente verificamos que no explota
        TicketMercadona ticket = TicketMercadona.parse(Path.of("src/test/resources/20230907 Mercadona 33,50 €.pdf"));
        assertNotNull(ticket);
        logger.debug("Ticket parseado: {}", ticket);
        logger.debug("Importe final: {}", ticket.precioTotalEnEuros());
        // TODO: Comprobar que lo que ha leido sea correcto
    }

    @Test
    public void pruebaFicheroAllPDF() throws Exception {
        Path dataDir = Path.of("../data/");

        // Skip test if directory doesn't exist
        if (!Files.exists(dataDir)) {
            logger.warn("Data directory does not exist: {}", dataDir.toAbsolutePath());
            return;
        }

        List<Path> pdfFiles = new ArrayList<>();

        // Tree walk to collect all PDF files
        Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().toLowerCase().endsWith(".pdf")) {
                    pdfFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Found {} PDF files in {}", pdfFiles.size(), dataDir.toAbsolutePath());

        // Parse each PDF file
        for (Path pdfFile : pdfFiles) {
            logger.debug("Processing PDF: {}", pdfFile);
            try {
                TicketMercadona ticket = TicketMercadona.parse(pdfFile);
                assertNotNull(ticket);
                logger.debug("Successfully parsed ticket from: {}", pdfFile.getFileName());
                logger.debug("Importe final: {}", ticket.precioTotalEnEuros());
            } catch (Exception e) {
                logger.error("Failed to parse PDF: {}", pdfFile, e);
                throw e;
            }
        }
    }

    @Test
    void pruebaBasica() throws Exception {
        String ticketData = """
                                    MERCADONA, S.A. A-46103834
                                    AV. DE FERRRAN EL CATÒLIC, 1
                                       46113 MONCADA
                                     TELÉFONO: 961309467
                                  01/01/2023 09:00 OP: 257136
                              FACTURA SIMPLIFICADA: 4567-891-113122
                
                
                
                
                           Descripción                       P. Unit    Importe
                       1   BARRA DE PAN                                    0,48
                       1   PANECILLO 11UDS                                 1,10
                       2   FRANKFURT VIENA QUES                 2,80       5,60
                       1   CHORIZO 4PACK                                   1,97
                       1   BANANA
                               0,336 kg                   1,45 €/kg        0,49
                                                         TOTAL (€)         9,64
                                                TARJETA BANCARIA           9,64
                
                            IVA           BASE IMPONIBLE (€)     CUOTA (€)
                            10%                   8,54             0,85
                             0%                   1,10             0,00
                           TOTAL                 11,65             0,85
                
                       TARJ. BANCARIA: **** **** **** 1234
                       N.C: 1234567                                  AUT: Z12345
                       AID: A0000000041010                           ARC: 46113
                
                
                       MASTERCARD
                       Importe: 12,48 €                         MASTERCARD
                       
                       
                       
                       
                             SE ADMITEN DEVOLUCIONES CON TICKET
                       
                """;

        logger.debug("Entrando en test");
        TicketMercadona ticket = TicketMercadona.parse(ticketData);
        logger.debug("Ticket parseado: {}", ticket);

        assertEquals("MERCADONA, S.A.", ticket.shopData().shopName());
        assertEquals("A-46103834", ticket.shopData().cif());
        assertEquals("AV. DE FERRRAN EL CATÒLIC, 1", ticket.shopData().address());
        assertEquals("46113", ticket.shopData().postalCode());
        assertEquals("MONCADA", ticket.shopData().state());
        assertEquals("961309467", ticket.shopData().phoneNumber());

        assertEquals(LocalDateTime.parse("01/01/2023 09:00", MERCADONA_DATE_TIME_FORMAT), ticket.header().fechaCompra());
        assertEquals("01/01/2023 09:00", ticket.header().fechaCompra().format(MERCADONA_DATE_TIME_FORMAT));
        assertEquals("257136", ticket.header().OP());
        assertEquals("4567-891-113122", ticket.header().codigoFacturaSimplificada());

        assertNotNull(ticket.items());
        assertEquals(5,ticket.items().size());

        // First item: BARRA DE PAN
        assertNotNull(ticket.items().get(0));
        assertEquals("BARRA DE PAN", ticket.items().get(0).id());
        assertEquals(new BigDecimal("0.48"), ticket.items().get(0).precioTotal());

        // Second item: PANECILLO 11UDS
        assertNotNull(ticket.items().get(1));
        assertEquals("PANECILLO 11UDS", ticket.items().get(1).id());
        assertEquals(new BigDecimal("1.10"), ticket.items().get(1).precioTotal());

        // Third item: FRANKFURT VIENA QUES
        assertNotNull(ticket.items().get(2));
        assertEquals("FRANKFURT VIENA QUES", ticket.items().get(2).id());
        assertEquals(2, ((ItemByUnit)ticket.items().get(2)).cantidad());
        assertEquals(new BigDecimal("5.60"), ticket.items().get(2).precioTotal());

        // Fourth item: CHORIZO 4PACK
        assertNotNull(ticket.items().get(3));
        assertEquals("CHORIZO 4PACK", ticket.items().get(3).id());
        assertEquals(new BigDecimal("1.97"), ticket.items().get(3).precioTotal());

        // Fifth item: BANANA
        assertNotNull(ticket.items().get(4));
        assertEquals("BANANA", ticket.items().get(4).id());
        assertEquals(new BigDecimal("0.49"), ticket.items().get(4).precioTotal());
    }

}