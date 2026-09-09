package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.junit.jupiter.api.Test;
import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InflationAnalyzerBasketCompositionTest {

    private static final int MIN_COMPLETE_MONTHS = 3;
    private static final int MIN_TOTAL_PURCHASE_COUNT = 3;

    private static PurchasedItemRecord unitRecord(String id, LocalDateTime date, BigDecimal price) {
        return new PurchasedItemRecord(id, date, 1, price, null, null, price);
    }

    @Test
    void calculaElPesoRelativoDeCadaProductoSobreElGastoDelAnioBase() {
        List<PurchasedItemRecord> records = List.of(
                // PAN: 3 compras de 1,00 € -> gasto 3,00 €
                unitRecord("PAN", LocalDateTime.of(2024, 1, 1, 10, 0), BigDecimal.ONE),
                unitRecord("PAN", LocalDateTime.of(2024, 2, 1, 10, 0), BigDecimal.ONE),
                unitRecord("PAN", LocalDateTime.of(2024, 3, 1, 10, 0), BigDecimal.ONE),
                // LECHE: 3 compras de 3,00 € -> gasto 9,00 €
                unitRecord("LECHE", LocalDateTime.of(2024, 1, 1, 10, 0), new BigDecimal("3.00")),
                unitRecord("LECHE", LocalDateTime.of(2024, 2, 1, 10, 0), new BigDecimal("3.00")),
                unitRecord("LECHE", LocalDateTime.of(2024, 3, 1, 10, 0), new BigDecimal("3.00"))
        );

        BasketComposition composition = InflationAnalyzer.analyzeBasketComposition(records, MIN_COMPLETE_MONTHS, MIN_TOTAL_PURCHASE_COUNT);

        assertEquals(2024, composition.baseYear());
        assertEquals(0, composition.totalBaseYearSpend().compareTo(new BigDecimal("12.00")));
        assertEquals(2, composition.weights().size());

        // Ordenado de mayor a menor peso: LECHE (75%) antes que PAN (25%)
        ProductWeight first = composition.weights().get(0);
        assertEquals("LECHE", first.key().id());
        assertEquals(0, first.weightPercent().compareTo(new BigDecimal("75.00")));
        assertTrue(first.hasBaseYearData());

        ProductWeight second = composition.weights().get(1);
        assertEquals("PAN", second.key().id());
        assertEquals(0, second.weightPercent().compareTo(new BigDecimal("25.00")));
    }
}
