package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Agrega la lista de compras por año y, dentro de cada año, por producto.
 */
public final class YearBasketDataBuilder {

    private YearBasketDataBuilder() {
    }

    /**
     * @param minCompleteMonths número mínimo de meses distintos con alguna compra para
     *                          considerar un año "completo" (ver {@link YearBasketData#complete()})
     */
    public static SortedMap<Integer, YearBasketData> build(List<PurchasedItemRecord> records, int minCompleteMonths) {
        Map<Integer, Map<ProductKey, ProductYearStats>> statsByYear = new HashMap<>();
        Map<Integer, Set<Integer>> monthsByYear = new HashMap<>();

        for (PurchasedItemRecord record : records) {
            int year = record.date().getYear();
            ProductKey key = ProductKey.of(record);

            Map<ProductKey, ProductYearStats> stats = statsByYear.computeIfAbsent(year, y -> new HashMap<>());
            stats.merge(key, new ProductYearStats(ProductKey.quantityOf(record), record.price()),
                    (existing, addition) -> existing.plus(addition.totalQuantity(), addition.totalSpent()));

            monthsByYear.computeIfAbsent(year, y -> new HashSet<>()).add(record.date().getMonthValue());
        }

        SortedMap<Integer, YearBasketData> result = new TreeMap<>();
        for (Integer year : statsByYear.keySet()) {
            int monthsWithPurchases = monthsByYear.get(year).size();
            boolean complete = monthsWithPurchases >= minCompleteMonths;
            result.put(year, new YearBasketData(year, monthsWithPurchases, complete, statsByYear.get(year)));
        }
        return result;
    }

    /**
     * Número de veces que se ha comprado cada producto en toda la serie (todos los años,
     * completos o no) — una línea de ticket cuenta como una compra, independientemente de
     * cuántas unidades o kg tuviera esa línea.
     */
    public static Map<ProductKey, Long> countPurchasesPerProduct(List<PurchasedItemRecord> records) {
        Map<ProductKey, Long> counts = new HashMap<>();
        for (PurchasedItemRecord record : records) {
            counts.merge(ProductKey.of(record), 1L, Long::sum);
        }
        return counts;
    }
}
