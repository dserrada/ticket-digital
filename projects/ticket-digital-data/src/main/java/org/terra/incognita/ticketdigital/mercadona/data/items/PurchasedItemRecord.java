package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.terra.incognita.ticketdigital.mercadona.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Un registro de la compra de un producto.
 * Se va a utilizar paar representar en una hoja de calculo el registro de la compra de un producto.
 *
 * @param id el identificador del producto
 * @param fecha la fecha de compra del producto
 * @param unidades el número de unidades compradas
 * @param precioPorUnidad el precio por unidad del producto
 * @param pesoKg el peso del producto en kilogramos (incompatible con producto comprado por unidades)
 * @param precioKg el precio por kilogramo del producto
 * @param precio el precio total de la compra del producto
 */
public record PurchasedItemRecord(String id, LocalDateTime fecha, Integer unidades, BigDecimal precioPorUnidad,
                                  BigDecimal pesoKg, BigDecimal precioKg, BigDecimal precio) {

    public static  List<PurchasedItemRecord> fromTicket(TicketMercadona ticket) {
        LocalDateTime fecha = ticket.header().fechaCompra();
        return ticket.items().stream()
                .map(item -> switch (item) {
                    case OneItemByUnit u ->
                            new PurchasedItemRecord(u.id(), fecha, u.cantidad(), u.precioPorUnidad(), null, null, u.precio());
                    case NItemsByUnit n ->
                            new PurchasedItemRecord(n.id(), fecha, n.cantidad(), n.precioPorUnidad(), null, null, n.precio());
                    case ItemByWeight w ->
                            // INFO: Lo de poner el 1 a huevo no se si es buena idea o no.
                            new PurchasedItemRecord(w.id(), fecha, 1, null, w.pesoKg(), w.precioPorKilogramo(), w.precio());
                    case FreshItemByWeight f ->
                            new PurchasedItemRecord(f.id(), fecha, 1, null, f.pesoKg(), f.precioPorKilogramo(), f.precio());
                })
                .toList();
    }

    public String toCSV() {
        // TODO: Mejorar el formato de salida a CSV
        return String.format("%s;%s;%s;%s;%s;%s;%s", id,
                DateTimeFormatter.ofPattern("dd/MM/yyyy").format(fecha),
                unidades,
                precioPorUnidad == null ? "" : precioPorUnidad.toString().replace('.', ','),
                pesoKg == null ? "" : pesoKg.toString().replace('.', ','),
                precioKg == null ? "" :  precioKg.toString().replace('.', ','),
                precio == null ? "" : precio.toString().replace('.', ','));
    }

    public static String headerCSV() {
        return String.format("%s;%s;%s;%s;%s;%s;%s", "id", "fecha", "unidades", "precioPorUnidad", "pesoKg", "precioKg", "precio");
    }

    
    
    
}
