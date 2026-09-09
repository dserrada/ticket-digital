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

        List<ProductWeight> weights = new ArrayList<>();
        for (ProductKey key : selection.basket()) {
            ProductYearStats stats = baseYearData.productStats().get(key);
            boolean hasData = stats != null;
            BigDecimal spend = hasData ? stats.totalSpent() : BigDecimal.ZERO;
            BigDecimal weightPercent = totalSpend.signum() == 0
                    ? BigDecimal.ZERO
                    : spend.multiply(BigDecimal.valueOf(100), MathContext.DECIMAL64).divide(totalSpend, 2, RoundingMode.HALF_UP);
            weights.add(new ProductWeight(key, spend, weightPercent, hasData));
        }
        weights.sort(Comparator.comparing(ProductWeight::weightPercent).reversed()
                .thenComparing(w -> w.key().id()));

        return new BasketComposition(selection.baseYear(), selection.basketYear(), totalSpend, weights);
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
