package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

/**
 * Decide, a partir de los datos agregados por año, cuál es el año base de la serie y
 * qué productos forman la cesta de la compra.
 */
public final class BasketSelector {

    private BasketSelector() {
    }

    /**
     * El año base es el primer año (cronológicamente) que sea "completo": el primero
     * con datos suficientemente representativos de un año entero de precios. Un año a
     * mitad de temporada (p.ej. el primero de la serie, si esta empieza en verano) no
     * es un buen año base aunque sea el más antiguo.
     */
    public static Optional<Integer> selectBaseYear(SortedMap<Integer, YearBasketData> years) {
        return years.values().stream()
                .filter(YearBasketData::complete)
                .map(YearBasketData::year)
                .min(Integer::compareTo);
    }

    /**
     * El año usado para construir la cesta es, entre los años completos, el que tiene
     * mayor variedad de productos distintos: es el que mejor representa la compra
     * habitual. En caso de empate, se elige el más antiguo.
     */
    public static Optional<Integer> selectBasketYear(SortedMap<Integer, YearBasketData> years) {
        return years.values().stream()
                .filter(YearBasketData::complete)
                .max((a, b) -> {
                    int byCount = Integer.compare(a.distinctProductCount(), b.distinctProductCount());
                    return byCount != 0 ? byCount : Integer.compare(b.year(), a.year());
                })
                .map(YearBasketData::year);
    }

    /**
     * Los productos del año-cesta que además se han comprado, en toda la serie
     * histórica, al menos {@code minTotalPurchaseCount} veces. Descarta los productos
     * comprados de forma puntual, que no son representativos de una compra habitual.
     */
    public static Set<ProductKey> buildBasket(YearBasketData basketYearData,
                                               Map<ProductKey, Long> totalPurchaseCounts,
                                               int minTotalPurchaseCount) {
        Set<ProductKey> basket = new TreeSet<>();
        for (ProductKey key : basketYearData.productStats().keySet()) {
            long count = totalPurchaseCounts.getOrDefault(key, 0L);
            if (count >= minTotalPurchaseCount) {
                basket.add(key);
            }
        }
        return basket;
    }
}
