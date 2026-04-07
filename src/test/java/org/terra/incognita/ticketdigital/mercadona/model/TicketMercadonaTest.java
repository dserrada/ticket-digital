package org.terra.incognita.ticketdigital.mercadona.model;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        /*
        assertEquals("BANANA", ticket.productos().get(4).descripcion());
        assertEquals(new BigDecimal("0.336"), ticket.productos().get(4).pesoKg());
        assertNull(ticket.productos() .get(4).precioUnitario());
        assertEquals(new BigDecimal("1.45"), ticket.productos().get(4).precioPorKilo());
        assertEquals(new BigDecimal("0.49"), ticket.productos().get(4).importe());
        */
    }

}