package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Calcula, para cada año de la serie, los índices de Laspeyres y Paasche frente al año
 * base, sobre la cesta de productos ya seleccionada.
 *
 * <p>Ambos índices usan el método "matched-model": si un producto de la cesta no tiene
 * precio conocido en el año base o en el año que se está calculando, se excluye de las
 * dos sumas de ESE año (no se imputa ningún precio). Como consecuencia, si el año usado
 * para construir la cesta es distinto del año base y algún producto de la cesta no se
 * compró en el año base, ese producto queda excluido de TODOS los años de la serie (nunca
 * hay precio/cantidad base con los que compararlo) — es el comportamiento esperado del
 * método, no un error.
 */
public final class InflationIndexCalculator {

    private InflationIndexCalculator() {
    }

    public static List<YearInflationIndex> compute(SortedMap<Integer, YearBasketData> years, int baseYear, Set<ProductKey> basket) {
        YearBasketData baseYearData = years.get(baseYear);
        if (baseYearData == null) {
            throw new IllegalArgumentException("El año base " + baseYear + " no está entre los años con datos");
        }

        List<YearInflationIndex> result = new ArrayList<>();
        for (YearBasketData yearData : years.values()) {
            result.add(computeYear(yearData, baseYearData, basket));
        }
        return result;
    }

    private static YearInflationIndex computeYear(YearBasketData yearData, YearBasketData baseYearData, Set<ProductKey> basket) {
        BigDecimal laspeyresNum = BigDecimal.ZERO;
        BigDecimal laspeyresDen = BigDecimal.ZERO;
        BigDecimal paascheNum = BigDecimal.ZERO;
        BigDecimal paascheDen = BigDecimal.ZERO;
        int matched = 0;

        for (ProductKey key : basket) {
            ProductYearStats base = baseYearData.productStats().get(key);
            ProductYearStats target = yearData.productStats().get(key);
            if (base == null || target == null) continue;

            BigDecimal p0 = base.averagePrice();
            BigDecimal q0 = base.totalQuantity();
            BigDecimal pt = target.averagePrice();
            BigDecimal qt = target.totalQuantity();
            if (p0 == null || pt == null) continue;

            matched++;
            laspeyresNum = laspeyresNum.add(pt.multiply(q0));
            laspeyresDen = laspeyresDen.add(p0.multiply(q0));
            paascheNum = paascheNum.add(pt.multiply(qt));
            paascheDen = paascheDen.add(p0.multiply(qt));
        }

        BigDecimal laspeyres = index(laspeyresNum, laspeyresDen);
        BigDecimal paasche = index(paascheNum, paascheDen);
        boolean isBaseYear = yearData.year() == baseYearData.year();
        return new YearInflationIndex(yearData.year(), yearData.complete(), isBaseYear, laspeyres, paasche, matched);
    }

    private static BigDecimal index(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) return null;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
