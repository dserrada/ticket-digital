package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class InflationIndexCalculatorTest {

    private static final ProductKey LECHE = new ProductKey("LECHE", ProductType.UNIDAD);
    private static final ProductKey PAN = new ProductKey("PAN", ProductType.UNIDAD);
    private static final Set<ProductKey> BASKET = Set.of(LECHE, PAN);

    private static ProductYearStats stats(String quantity, String spent) {
        return new ProductYearStats(new BigDecimal(quantity), new BigDecimal(spent));
    }

    @Test
    void calculaLaspeyresYPaascheConUnCasoNumericoVerificadoAMano() {
        // Año base 2023: LECHE 10 uds a 1,00 (10,00) ; PAN 5 uds a 2,00 (10,00)
        Map<ProductKey, ProductYearStats> base = Map.of(
                LECHE, stats("10", "10.00"),
                PAN, stats("5", "10.00")
        );
        // Año 2024: LECHE 8 uds a 1,20 (9,60) ; PAN 6 uds a 2,20 (13,20)
        Map<ProductKey, ProductYearStats> year2024 = Map.of(
                LECHE, stats("8", "9.60"),
                PAN, stats("6", "13.20")
        );

        SortedMap<Integer, YearBasketData> years = new TreeMap<>();
        years.put(2023, new YearBasketData(2023, 12, true, base));
        years.put(2024, new YearBasketData(2024, 12, true, year2024));

        List<YearInflationIndex> result = InflationIndexCalculator.compute(years, 2023, BASKET);

        YearInflationIndex base2023 = result.get(0);
        assertTrue(base2023.baseYear());
        assertEquals(0, base2023.laspeyres().compareTo(new BigDecimal("100.00")));
        assertEquals(0, base2023.paasche().compareTo(new BigDecimal("100.00")));

        YearInflationIndex target2024 = result.get(1);
        assertFalse(target2024.baseYear());
        // Laspeyres = 100*(1,20*10 + 2,20*5)/(1,00*10 + 2,00*5) = 100*23/20 = 115,00
        assertEquals(0, target2024.laspeyres().compareTo(new BigDecimal("115.00")));
        // Paasche = 100*(9,60 + 13,20)/(1,00*8 + 2,00*6) = 100*22,80/20 = 114,00
        assertEquals(0, target2024.paasche().compareTo(new BigDecimal("114.00")));
        assertEquals(2, target2024.matchedProductCount());
    }

    @Test
    void excluyeDeAmbasSumasUnProductoDeLaCestaNoCompradoEseAnio() {
        Map<ProductKey, ProductYearStats> base = Map.of(
                LECHE, stats("10", "10.00"),
                PAN, stats("5", "10.00")
        );
        // 2025: solo se compró LECHE, PAN no aparece ese año
        Map<ProductKey, ProductYearStats> year2025 = Map.of(
                LECHE, stats("5", "6.00")
        );

        SortedMap<Integer, YearBasketData> years = new TreeMap<>();
        years.put(2023, new YearBasketData(2023, 12, true, base));
        years.put(2025, new YearBasketData(2025, 12, true, year2025));

        List<YearInflationIndex> result = InflationIndexCalculator.compute(years, 2023, BASKET);
        YearInflationIndex target2025 = result.get(1);

        // Solo LECHE contribuye: 100*(1,20*10)/(1,00*10) = 120,00 (Laspeyres),
        // 100*(1,20*5)/(1,00*5) = 120,00 (Paasche)
        assertEquals(1, target2025.matchedProductCount());
        assertEquals(0, target2025.laspeyres().compareTo(new BigDecimal("120.00")));
        assertEquals(0, target2025.paasche().compareTo(new BigDecimal("120.00")));
    }

    @Test
    void devuelveIndicesNulosSiNingunProductoDeLaCestaTieneDatosEseAnio() {
        Map<ProductKey, ProductYearStats> base = Map.of(
                LECHE, stats("10", "10.00"),
                PAN, stats("5", "10.00")
        );
        Map<ProductKey, ProductYearStats> year2026 = Map.of(); // nada de la cesta comprado

        SortedMap<Integer, YearBasketData> years = new TreeMap<>();
        years.put(2023, new YearBasketData(2023, 12, true, base));
        years.put(2026, new YearBasketData(2026, 12, true, year2026));

        List<YearInflationIndex> result = InflationIndexCalculator.compute(years, 2023, BASKET);
        YearInflationIndex target2026 = result.get(1);

        assertEquals(0, target2026.matchedProductCount());
        assertNull(target2026.laspeyres());
        assertNull(target2026.paasche());
    }
}
