package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.terra.incognita.ticketdigital.mercadona.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

    // El listado de compras se muestra de la más reciente a la más antigua.
    public static final Comparator<PurchasedItemRecord> BY_DATE_DESCENDING =
            Comparator.comparing(PurchasedItemRecord::date).reversed();

    public static  List<PurchasedItemRecord> fromTicket(TicketMercadona ticket) {
        LocalDateTime date = ticket.header().purchaseDate();
        return ticket.items().stream()
                .map(item -> switch (item) {
                    case OneItemByUnit u ->
                            new PurchasedItemRecord(ProductNameNormalizer.normalize(u.id()), date, u.quantity(), unitPriceOrFallback(u.quantity(), u.unitPrice(), u.price()), null, null, u.price());
                    case NItemsByUnit n ->
                            new PurchasedItemRecord(ProductNameNormalizer.normalize(n.id()), date, n.quantity(), unitPriceOrFallback(n.quantity(), n.unitPrice(), n.price()), null, null, n.price());
                    case ItemByWeight w ->
                            // INFO: Lo de poner el 1 a huevo no se si es buena idea o no.
                            new PurchasedItemRecord(ProductNameNormalizer.normalize(w.id()), date, 1, null, w.weightKg(), w.pricePerKilogram(), w.price());
                    case FreshItemByWeight f ->
                            new PurchasedItemRecord(ProductNameNormalizer.normalize(f.id()), date, 1, null, f.weightKg(), f.pricePerKilogram(), f.price());
                })
                .toList();
    }

    // Si solo se ha comprado una unidad el precio por unidad coincide con el precio total,
    // pero el parseador lo deja a null porque el ticket no lo indica explícitamente.
    private static BigDecimal unitPriceOrFallback(int quantity, BigDecimal unitPrice, BigDecimal price) {
        return quantity == 1 && unitPrice == null ? price : unitPrice;
    }

    public String toCSV() {
        return String.join(";",
                escapeCSV(id),
                escapeCSV(DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date)),
                escapeCSV(String.valueOf(units)),
                escapeCSV(unitPrice == null ? "" : unitPrice.toString().replace('.', ',')),
                escapeCSV(weightKg == null ? "" : weightKg.toString().replace('.', ',')),
                escapeCSV(pricePerKg == null ? "" : pricePerKg.toString().replace('.', ',')),
                escapeCSV(price == null ? "" : price.toString().replace('.', ',')));
    }

    public static String headerCSV() {
        return String.join(";", "id", "fecha", "unidades", "precioPorUnidad", "pesoKg", "precioKg", "precio");
    }

    // RFC 4180 adaptado al delimitador ';' que usa este fichero: si el valor contiene el
    // delimitador, comillas o un salto de línea, se envuelve entre comillas dobles y cualquier
    // comilla interna se duplica. Sin esto, un id de producto con un ';' (el propio delimitador)
    // desplazaría en silencio el resto de columnas de la fila.
    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }




}
