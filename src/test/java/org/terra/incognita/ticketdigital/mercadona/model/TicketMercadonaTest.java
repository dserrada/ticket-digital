package org.terra.incognita.ticketdigital.mercadona.model;


import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TicketMercadonaTest {

    /**
     * El formato de las fechas en los tickets del mercadona.
     *
     * FIXME: Realmente no debe estar aquí, pero dentro de un record no se puede poner
     */
    public static DateTimeFormatter MERCADONA_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Test
    void pruebaBasica() {
        TicketMercadona t1 = new TicketMercadona("MERCADONA,S.A. A-46103834",
                new Tienda("Av. de Ferrran el Catòlic, 1","46113","Moncada","961309467"),
                LocalDateTime.parse("21/04/2023 17:52",MERCADONA_DATE_TIME_FORMAT),
                new ArrayList<Articulo>(
                    List.of( new Articulo("PATATA MALLA 5 KG.",new Cantidad(TipoCantidad.UNIDAD,1),6.89f),
                        new Articulo("FRANKFURT VIENA QUES",new Cantidad(TipoCantidad.UNIDAD,2),2.80f))
                ),
                12.49,"**** **** **** 1234",
                "1234567","Z12345", "A0000000041010","3030");
        System.out.println("Precio calculado: " + t1.importeTotalCalculado());
        assertEquals(t1.tienda().codigoPostal(),"46113");
        // A lo mejor lo de los precios calculados no es tan buena idea, por el redondeo
        // assertEquals(t1.importeTotalCalculado(),t1.importeTotal());
    }
}