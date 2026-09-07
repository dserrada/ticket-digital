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
 * @param date la fecha de compra del producto
 * @param units el número de unidades compradas
 * @param unitPrice el precio por unidad del producto
 * @param weightKg el peso del producto en kilogramos (incompatible con producto comprado por unidades)
 * @param pricePerKg el precio por kilogramo del producto
 * @param price el precio total de la compra del producto
 */
public record PurchasedItemRecord(String id, LocalDateTime date, Integer units, BigDecimal unitPrice,
                                  BigDecimal weightKg, BigDecimal pricePerKg, BigDecimal price) {

    public static  List<PurchasedItemRecord> fromTicket(TicketMercadona ticket) {
        LocalDateTime date = ticket.header().purchaseDate();
        return ticket.items().stream()
                .map(item -> switch (item) {
                    case OneItemByUnit u ->
                            new PurchasedItemRecord(u.id(), date, u.quantity(), u.unitPrice(), null, null, u.price());
                    case NItemsByUnit n ->
                            new PurchasedItemRecord(n.id(), date, n.quantity(), n.unitPrice(), null, null, n.price());
                    case ItemByWeight w ->
                            // INFO: Lo de poner el 1 a huevo no se si es buena idea o no.
                            new PurchasedItemRecord(w.id(), date, 1, null, w.weightKg(), w.pricePerKilogram(), w.price());
                    case FreshItemByWeight f ->
                            new PurchasedItemRecord(f.id(), date, 1, null, f.weightKg(), f.pricePerKilogram(), f.price());
                })
                .toList();
    }

    public String toCSV() {
        // TODO: Mejorar el formato de salida a CSV
        return String.format("%s;%s;%s;%s;%s;%s;%s", id,
                DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date),
                units,
                unitPrice == null ? "" : unitPrice.toString().replace('.', ','),
                weightKg == null ? "" : weightKg.toString().replace('.', ','),
                pricePerKg == null ? "" :  pricePerKg.toString().replace('.', ','),
                price == null ? "" : price.toString().replace('.', ','));
    }

    public static String headerCSV() {
        return String.format("%s;%s;%s;%s;%s;%s;%s", "id", "fecha", "unidades", "precioPorUnidad", "pesoKg", "precioKg", "precio");
    }




}
