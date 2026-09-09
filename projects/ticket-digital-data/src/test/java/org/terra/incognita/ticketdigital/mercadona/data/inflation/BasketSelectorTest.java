package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.junit.jupiter.api.Test;
import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

class BasketSelectorTest {

    private static PurchasedItemRecord unitRecord(String id, LocalDateTime date) {
        return new PurchasedItemRecord(id, date, 1, BigDecimal.ONE, null, null, BigDecimal.ONE);
    }

    private static final int MIN_COMPLETE_MONTHS = 10;

    @Test
    void elAnioBaseEsElPrimerAnioCompletoAunqueLaSerieEmpieceAMitadDeAnio() {
        List<PurchasedItemRecord> records = new java.util.ArrayList<>();
        // 2023: solo 3 meses de datos (la serie empieza en julio) -> incompleto
        for (int month = 7; month <= 9; month++) {
            records.add(unitRecord("PAN", LocalDateTime.of(2023, month, 1, 10, 0)));
        }
        // 2024: 12 meses completos
        for (int month = 1; month <= 12; month++) {
            records.add(unitRecord("PAN", LocalDateTime.of(2024, month, 1, 10, 0)));
        }

        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, MIN_COMPLETE_MONTHS);

        assertFalse(years.get(2023).complete());
        assertTrue(years.get(2024).complete());
        assertEquals(Integer.valueOf(2024), BasketSelector.selectBaseYear(years).orElseThrow());
    }

    @Test
    void noHayAnioBaseSiNingunAnioEsCompleto() {
        List<PurchasedItemRecord> records = List.of(
                unitRecord("PAN", LocalDateTime.of(2023, 7, 1, 10, 0)),
                unitRecord("PAN", LocalDateTime.of(2023, 8, 1, 10, 0))
        );

        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, MIN_COMPLETE_MONTHS);
        assertTrue(BasketSelector.selectBaseYear(years).isEmpty());
    }

    @Test
    void laCestaSeEligeEntreLosAniosCompletosAunqueUnIncompletoTengaMasProductos() {
        List<PurchasedItemRecord> records = new java.util.ArrayList<>();
        // 2023: incompleto (solo 2 meses), pero con muchos productos distintos
        records.add(unitRecord("A", LocalDateTime.of(2023, 7, 1, 10, 0)));
        records.add(unitRecord("B", LocalDateTime.of(2023, 7, 5, 10, 0)));
        records.add(unitRecord("C", LocalDateTime.of(2023, 8, 1, 10, 0)));
        records.add(unitRecord("D", LocalDateTime.of(2023, 8, 5, 10, 0)));
        // 2024: completo (12 meses), con menos productos distintos
        for (int month = 1; month <= 12; month++) {
            records.add(unitRecord("PAN", LocalDateTime.of(2024, month, 1, 10, 0)));
        }

        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, MIN_COMPLETE_MONTHS);
        assertEquals(Integer.valueOf(2024), BasketSelector.selectBasketYear(years).orElseThrow());
    }

    @Test
    void buildBasketExcluyeProductosPorDebajoDelUmbralDeComprasHistoricas() {
        Map<ProductKey, ProductYearStats> stats = Map.of(
                new ProductKey("PAN", ProductType.UNIDAD), ProductYearStats.EMPTY,
                new ProductKey("CAPRICHO", ProductType.UNIDAD), ProductYearStats.EMPTY
        );
        YearBasketData basketYearData = new YearBasketData(2024, 12, true, stats);

        Map<ProductKey, Long> counts = Map.of(
                new ProductKey("PAN", ProductType.UNIDAD), 3L,
                new ProductKey("CAPRICHO", ProductType.UNIDAD), 2L
        );

        Set<ProductKey> basket = BasketSelector.buildBasket(basketYearData, counts, 3);

        assertTrue(basket.contains(new ProductKey("PAN", ProductType.UNIDAD)), "3 compras alcanza el umbral de 3");
        assertFalse(basket.contains(new ProductKey("CAPRICHO", ProductType.UNIDAD)), "2 compras no alcanza el umbral de 3");
    }
}
