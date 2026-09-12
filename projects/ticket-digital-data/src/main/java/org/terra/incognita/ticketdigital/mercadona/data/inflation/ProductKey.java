package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Identifica un producto dentro de la cesta de la compra: su nombre (ya normalizado
 * por {@code ProductNameNormalizer}) y cómo se compra (por unidad o por peso).
 */
public record ProductKey(String id, ProductType type) implements Comparable<ProductKey> {

    private static final Comparator<ProductKey> ORDER =
            Comparator.comparing(ProductKey::id).thenComparing(ProductKey::type);

    public static ProductKey of(PurchasedItemRecord record) {
        return new ProductKey(record.id(), record.weightKg() != null ? ProductType.PESO : ProductType.UNIDAD);
    }

    /**
     * La cantidad comprada en ese registro, en la unidad de medida propia de su tipo
     * (kg si es {@link ProductType#PESO}, unidades si es {@link ProductType#UNIDAD}).
     */
    public static BigDecimal quantityOf(PurchasedItemRecord record) {
        return record.weightKg() != null ? record.weightKg() : BigDecimal.valueOf(record.units());
    }

    @Override
    public int compareTo(ProductKey other) {
        return ORDER.compare(this, other);
    }
}
