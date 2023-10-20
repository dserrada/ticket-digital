package org.terra.incognita.ticketdigital.mercadona.model;

import java.time.LocalDateTime;
import java.util.List;


/**
 * La información contenida en un ticket digital de Mercadona
 *
 * @param cifMercadona  La cabecera inicial con el nombre de la empresa y su CIF. Actualmente: "MERCADONA,S.A. A-46103834"
 * @param tienda        Información de la tienda donde se ha generado el ticket digital
 */
public record TicketMercadona (String cifMercadona, Tienda tienda, LocalDateTime fecha, List<Articulo> articulos, double importeTotal,
                               String tarjetaBancaria, String nc, String aut, String aid, String arc) {
    public float importeTotalCalculado() {
        float p = articulos.stream().map(a -> a.precioTotalCalculado()).reduce(0f, Float::sum);
        return (float) (Math.round(p*100.0)/100.0);
    }
}

