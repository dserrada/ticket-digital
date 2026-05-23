package org.terra.incognita.ticketdigital.mercadona.model;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    public void pruebaFicheroPDFExterno() throws Exception {
        // Simplemente verificamos que no explota
        TicketMercadona ticket = TicketMercadona.parse(Path.of("src/test/resources/20250809 Mercadona 122,05 €.pdf"));
        assertNotNull(ticket);
        logger.debug("Ticket parseado: {}", ticket);
        logger.debug("Importe final: {}", ticket.precioTotalEnEuros());
        // TODO: Comprobar que lo que ha leido sea correcto
    }

    @Test
    public void pruebaFicheroPDFConProductosFrescos() throws Exception {
        TicketMercadona ticket = TicketMercadona.parse(Path.of("src/test/resources/20241108 Mercadona 77,71 €.pdf"));
        assertNotNull(ticket);

        // Verifica el total (suma de precios calculados)
        assertEquals(new BigDecimal("77.71"), ticket.precioTotalEnEuros());

        // Verifica que hay artículos frescos parseados correctamente
        List<FreshItemByWeight> freshItems = ticket.items().stream()
                .filter(i -> i instanceof FreshItemByWeight)
                .map(i -> (FreshItemByWeight) i)
                .toList();

        assertEquals(2, freshItems.size());

        FreshItemByWeight lubina = freshItems.get(0);
        assertEquals("LUBINA", lubina.id());
        assertEquals("PESCADO", lubina.freshType());
        assertEquals(new BigDecimal("0.762"), lubina.pesoKg());
        assertEquals(new BigDecimal("8.25"), lubina.precioPorKilogramo());

        FreshItemByWeight langostino = freshItems.get(1);
        assertEquals("LANGOSTINO COCIDO", langostino.id());
        assertEquals("PESCADO", langostino.freshType());
        assertEquals(new BigDecimal("0.518"), langostino.pesoKg());
        assertEquals(new BigDecimal("10.95"), langostino.precioPorKilogramo());

        assertEquals(ticket.pagadoEnEuros(),ticket.precioTotalEnEuros());
    }

    @Test
    public void pruebaIndentacionPreservadaEnPDF() throws Exception {
        // Verifica que IndentPreservingTextStripper produce más indentación para PESCADO
        // que para los artículos regulares (p.ej. "1 SALSA BOLOÑESA")
        Path pdfPath = Path.of("src/test/resources/20241108 Mercadona 77,71 €.pdf");
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            String text = IndentPreservingTextStripper.extractText(doc);
            logger.debug("Texto extraído con indentación:\n{}", text);

            String[] lines = text.split("\\n");

            int indentPescado = -1;
            int indentItemFresco = -1;
            int indentItemRegular = -1;

            for (String line : lines) {
                String trimmed = line.stripLeading();
                int indent = line.length() - trimmed.length();
                if (trimmed.startsWith("PESCADO") && indentPescado == -1) {
                    indentPescado = indent;
                } else if ((trimmed.startsWith("LUBINA") || trimmed.startsWith("LANGOSTINO")) && indentItemFresco == -1) {
                    indentItemFresco = indent;
                } else if (trimmed.startsWith("1 SALSA") && indentItemRegular == -1) {
                    indentItemRegular = indent;
                }
            }

            logger.info("Indentación PESCADO={}, item fresco={}, item regular={}",
                    indentPescado, indentItemFresco, indentItemRegular);

            assertTrue(indentPescado >= 0, "No se encontró la línea PESCADO");
            assertTrue(indentItemFresco >= 0, "No se encontró un item fresco (LUBINA/LANGOSTINO)");
            assertTrue(indentItemRegular >= 0, "No se encontró un item regular");

            // Los ítems frescos deben estar más indentados que PESCADO
            assertTrue(indentItemFresco > indentPescado,
                    "Los ítems frescos deben tener más indentación que PESCADO: fresco=%d, PESCADO=%d"
                            .formatted(indentItemFresco, indentPescado));
            // PESCADO debe estar más indentado que los ítems regulares
            assertTrue(indentPescado > indentItemRegular,
                    "PESCADO debe tener más indentación que los ítems regulares: PESCADO=%d, regular=%d"
                            .formatted(indentPescado, indentItemRegular));
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
        assertEquals(new BigDecimal("0.48"), ticket.items().get(0).precioCalculado());

        // Second item: PANECILLO 11UDS
        assertNotNull(ticket.items().get(1));
        assertEquals("PANECILLO 11UDS", ticket.items().get(1).id());
        assertEquals(new BigDecimal("1.10"), ticket.items().get(1).precioCalculado());

        // Third item: FRANKFURT VIENA QUES
        assertNotNull(ticket.items().get(2));
        assertEquals("FRANKFURT VIENA QUES", ticket.items().get(2).id());
        assertEquals(2, ((NItemsByUnit)ticket.items().get(2)).cantidad());
        assertEquals(new BigDecimal("5.60"), ticket.items().get(2).precioCalculado());

        // Fourth item: CHORIZO 4PACK
        assertNotNull(ticket.items().get(3));
        assertEquals("CHORIZO 4PACK", ticket.items().get(3).id());
        assertEquals(new BigDecimal("1.97"), ticket.items().get(3).precioCalculado());

        // Fifth item: BANANA
        assertNotNull(ticket.items().get(4));
        assertEquals("BANANA", ticket.items().get(4).id());
        assertEquals(new BigDecimal("0.49"), ticket.items().get(4).precioCalculado());
    }

}