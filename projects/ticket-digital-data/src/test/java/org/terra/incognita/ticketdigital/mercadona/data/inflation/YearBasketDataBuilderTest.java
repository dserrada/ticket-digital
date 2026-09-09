package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.junit.jupiter.api.Test;
import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

class YearBasketDataBuilderTest {

    private static PurchasedItemRecord unitRecord(String id, LocalDateTime date, int units, BigDecimal unitPrice) {
        BigDecimal price = unitPrice.multiply(BigDecimal.valueOf(units));
        return new PurchasedItemRecord(id, date, units, unitPrice, null, null, price);
    }

    private static PurchasedItemRecord weightRecord(String id, LocalDateTime date, BigDecimal weightKg, BigDecimal pricePerKg) {
        BigDecimal price = weightKg.multiply(pricePerKg);
        return new PurchasedItemRecord(id, date, 1, null, weightKg, pricePerKg, price);
    }

    @Test
    void calculaElPrecioMedioPonderadoDeUnProductoEnUnAnio() {
        List<PurchasedItemRecord> records = List.of(
                unitRecord("LECHE", LocalDateTime.of(2024, 3, 1, 10, 0), 2, new BigDecimal("1.00")),
                unitRecord("LECHE", LocalDateTime.of(2024, 5, 1, 10, 0), 3, new BigDecimal("1.20"))
        );

        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, 1);

        ProductYearStats stats = years.get(2024).productStats().get(new ProductKey("LECHE", ProductType.UNIDAD));
        assertEquals(0, stats.totalQuantity().compareTo(new BigDecimal("5")));
        assertEquals(0, stats.totalSpent().compareTo(new BigDecimal("5.60")));
        assertEquals(0, stats.averagePrice().compareTo(new BigDecimal("1.12")));
    }

    @Test
    void marcaUnAnioComoCompletoSegunElUmbralDeMesesDistintos() {
        List<PurchasedItemRecord> records = List.of(
                unitRecord("PAN", LocalDateTime.of(2023, 7, 1, 10, 0), 1, BigDecimal.ONE),
                unitRecord("PAN", LocalDateTime.of(2023, 8, 1, 10, 0), 1, BigDecimal.ONE),
                unitRecord("PAN", LocalDateTime.of(2023, 8, 15, 10, 0), 1, BigDecimal.ONE) // mismo mes que el anterior
        );

        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, 3);
        assertEquals(2, years.get(2023).monthsWithPurchases());
        assertFalse(years.get(2023).complete());

        years = YearBasketDataBuilder.build(records, 2);
        assertTrue(years.get(2023).complete());
    }

    @Test
    void unMismoProductoComoUnidadYComoPesoCuentaComoDosClaves() {
        List<PurchasedItemRecord> records = List.of(
                unitRecord("JAMON", LocalDateTime.of(2024, 1, 1, 10, 0), 1, new BigDecimal("3.00")),
                weightRecord("JAMON", LocalDateTime.of(2024, 2, 1, 10, 0), new BigDecimal("0.400"), new BigDecimal("10.00"))
        );

        SortedMap<Integer, YearBasketData> years = YearBasketDataBuilder.build(records, 1);
        Map<ProductKey, ProductYearStats> stats = years.get(2024).productStats();

        assertEquals(2, stats.size());
        assertTrue(stats.containsKey(new ProductKey("JAMON", ProductType.UNIDAD)));
        assertTrue(stats.containsKey(new ProductKey("JAMON", ProductType.PESO)));
    }

    @Test
    void cuentaLasComprasDeCadaProductoEnTodaLaSerieIncluyendoAniosIncompletos() {
        List<PurchasedItemRecord> records = List.of(
                unitRecord("PAN", LocalDateTime.of(2023, 7, 1, 10, 0), 1, BigDecimal.ONE),
                unitRecord("PAN", LocalDateTime.of(2024, 1, 1, 10, 0), 1, BigDecimal.ONE),
                unitRecord("PAN", LocalDateTime.of(2024, 2, 1, 10, 0), 1, BigDecimal.ONE)
        );

        Map<ProductKey, Long> counts = YearBasketDataBuilder.countPurchasesPerProduct(records);
        assertEquals(Long.valueOf(3L), counts.get(new ProductKey("PAN", ProductType.UNIDAD)));
    }
}
