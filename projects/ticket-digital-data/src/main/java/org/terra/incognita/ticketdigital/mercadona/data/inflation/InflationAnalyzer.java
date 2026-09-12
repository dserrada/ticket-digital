package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Fachada del análisis de inflación: agrupa los pasos de {@link YearBasketDataBuilder},
 * {@link BasketSelector} y {@link InflationIndexCalculator} en una única llamada.
 */
public final class InflationAnalyzer {

    /** Nº mínimo de meses distintos con compras para considerar un año "completo". */
    public static final int DEFAULT_MIN_COMPLETE_MONTHS = 10;

    /** Nº mínimo de veces que debe comprarse un producto en toda la serie para entrar en la cesta. */
    public static final int DEFAULT_MIN_TOTAL_PURCHASE_COUNT = 3;

    private InflationAnalyzer() {
    }

    public static InflationReport analyze(List<PurchasedItemRecord> records) {
        return analyze(records, DEFAULT_MIN_COMPLETE_MONTHS, DEFAULT_MIN_TOTAL_PURCHASE_COUNT);
    }

    public static InflationReport analyze(List<PurchasedItemRecord> records, int minCompleteMonths, int minTotalPurchaseCount) {
        Selection selection = select(records, minCompleteMonths, minTotalPurchaseCount);

        List<YearInflationIndex> indices = InflationIndexCalculator.compute(selection.years(), selection.baseYear(), selection.basket());

        return new InflationReport(selection.baseYear(), selection.basketYear(),
                selection.basket().stream().sorted().toList(), indices);
    }

    public static BasketComposition analyzeBasketComposition(List<PurchasedItemRecord> records) {
        return analyzeBasketComposition(records, DEFAULT_MIN_COMPLETE_MONTHS, DEFAULT_MIN_TOTAL_PURCHASE_COUNT);
    }

    public static BasketComposition analyzeBasketComposition(List<PurchasedItemRecord> records, int minCompleteMonths, int minTotalPurchaseCount) {
        Selection selection = select(records, minCompleteMonths, minTotalPurchaseCount);
        YearBasketData baseYearData = selection.years().get(selection.baseYear());

        BigDecimal totalSpend = BigDecimal.ZERO;
        for (ProductKey key : selection.basket()) {
            ProductYearStats stats = baseYearData.productStats().get(key);
            if (stats != null) {
                totalSpend = totalSpend.add(stats.totalSpent());
            }
        }

        List<ProductWeight> weights = buildWeights(selection.basket(), baseYearData, totalSpend);
        weights.sort(Comparator.comparing(ProductWeight::weightPercent).reversed()
                .thenComparing(w -> w.key().id()));

        return new BasketComposition(selection.baseYear(), selection.basketYear(), totalSpend, weights);
    }

    // Reparte el 100% entre los productos de la cesta por el método del "mayor resto" (largest
    // remainder): redondear cada peso a 2 decimales de forma independiente (como se hacía antes)
    // puede hacer que la suma no dé exactamente 100,00 (p.ej. tres pesos de 33,33...% redondean a
    // 33,33 cada uno, que suman 99,99). Aquí se calcula primero el peso exacto de cada producto, se
    // trunca a 2 decimales, y los "céntimos" de porcentaje que faltan hasta 100,00 se reparten de
    // uno en uno entre los productos con mayor resto (el que perdió más al truncar), para que la
    // suma final sea siempre exactamente 100,00 cuando totalSpend &gt; 0.
    private static List<ProductWeight> buildWeights(Set<ProductKey> basket, YearBasketData baseYearData, BigDecimal totalSpend) {
        record Entry(ProductKey key, BigDecimal spend, boolean hasData, BigDecimal exactPercent) {
        }

        List<Entry> entries = new ArrayList<>();
        for (ProductKey key : basket) {
            ProductYearStats stats = baseYearData.productStats().get(key);
            boolean hasData = stats != null;
            BigDecimal spend = hasData ? stats.totalSpent() : BigDecimal.ZERO;
            BigDecimal exactPercent = totalSpend.signum() == 0
                    ? BigDecimal.ZERO
                    : spend.multiply(BigDecimal.valueOf(100), MathContext.DECIMAL64).divide(totalSpend, 10, RoundingMode.HALF_UP);
            entries.add(new Entry(key, spend, hasData, exactPercent));
        }

        if (totalSpend.signum() == 0) {
            return entries.stream()
                    .map(e -> new ProductWeight(e.key(), e.spend(), BigDecimal.ZERO.setScale(2), e.hasData()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        // "Céntimos" de porcentaje (2 decimales) de cada producto tras truncar a la baja, y cuántos
        // faltan para llegar a 100,00 (10000 céntimos) sumando todos los truncamientos.
        List<Long> flooredCents = new ArrayList<>();
        long sumFlooredCents = 0;
        for (Entry e : entries) {
            BigDecimal flooredPercent = e.exactPercent().setScale(2, RoundingMode.DOWN);
            long cents = flooredPercent.movePointRight(2).longValueExact();
            flooredCents.add(cents);
            sumFlooredCents += cents;
        }
        long deficitCents = 10000L - sumFlooredCents;

        List<Integer> byRemainderDesc = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            byRemainderDesc.add(i);
        }
        byRemainderDesc.sort(Comparator
                .<Integer, BigDecimal>comparing(i -> entries.get(i).exactPercent()
                        .subtract(entries.get(i).exactPercent().setScale(2, RoundingMode.DOWN)))
                .reversed()
                .thenComparing(i -> entries.get(i).key().id()));

        long[] finalCents = flooredCents.stream().mapToLong(Long::longValue).toArray();
        for (int i = 0; i < deficitCents && i < byRemainderDesc.size(); i++) {
            finalCents[byRemainderDesc.get(i)]++;
        }

        List<ProductWeight> weights = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            BigDecimal weightPercent = BigDecimal.valueOf(finalCents[i], 2);
            weights.add(new ProductWeight(e.key(), e.spend(), weightPercent, e.hasData()));
        }
        return weights;
    }

    private static Selection select(List<PurchasedItemRecord> records, int minCompleteMonths, int minTotalPurchaseCount) {
        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, minCompleteMonths);

        int baseYear = BasketSelector.selectBaseYear(years)
                .orElseThrow(() -> new IllegalStateException(
                        "Ningún año tiene compras en al menos " + minCompleteMonths + " meses distintos; "
                                + "no se puede establecer un año base ni calcular la cesta de la compra."));

        int basketYear = BasketSelector.selectBasketYear(years)
                .orElseThrow(() -> new IllegalStateException(
                        "Ningún año tiene compras en al menos " + minCompleteMonths + " meses distintos; "
                                + "no se puede establecer un año base ni calcular la cesta de la compra."));

        Map<ProductKey, Long> totalPurchaseCounts = YearBasketDataBuilder.countPurchasesPerProduct(records);
        Set<ProductKey> basket = BasketSelector.buildBasket(years.get(basketYear), totalPurchaseCounts, minTotalPurchaseCount);

        return new Selection(years, baseYear, basketYear, basket);
    }

    private record Selection(SortedMap<Integer, YearBasketData> years, int baseYear, int basketYear, Set<ProductKey> basket) {
    }
}
