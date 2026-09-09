package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.junit.jupiter.api.Test;
import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InflationAnalyzerTest {

    private static final int MIN_COMPLETE_MONTHS = 3;
    private static final int MIN_TOTAL_PURCHASE_COUNT = 3;

    private static PurchasedItemRecord unitRecord(String id, LocalDateTime date) {
        return new PurchasedItemRecord(id, date, 1, BigDecimal.ONE, null, null, BigDecimal.ONE);
    }

    @Test
    void elAnioBaseEsElPrimerAnioCompletoCuandoLaSerieEmpiezaAMitadDeAnio() {
        List<PurchasedItemRecord> records = new ArrayList<>();
        // 2023: solo 2 meses -> incompleto para el umbral de 3
        records.add(unitRecord("PAN", LocalDateTime.of(2023, 7, 1, 10, 0)));
        records.add(unitRecord("PAN", LocalDateTime.of(2023, 8, 1, 10, 0)));
        // 2024: 4 meses -> completo
        for (int month = 1; month <= 4; month++) {
            records.add(unitRecord("PAN", LocalDateTime.of(2024, month, 1, 10, 0)));
        }

        InflationReport report = InflationAnalyzer.analyze(records, MIN_COMPLETE_MONTHS, MIN_TOTAL_PURCHASE_COUNT);

        assertEquals(2024, report.baseYear());
    }

    @Test
    void lanzaExcepcionConMensajeClaroSiNingunAnioEsCompleto() {
        List<PurchasedItemRecord> records = List.of(
                unitRecord("PAN", LocalDateTime.of(2023, 7, 1, 10, 0)),
                unitRecord("PAN", LocalDateTime.of(2023, 8, 1, 10, 0))
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> InflationAnalyzer.analyze(records, MIN_COMPLETE_MONTHS, MIN_TOTAL_PURCHASE_COUNT));
        assertTrue(ex.getMessage().contains("año base") || ex.getMessage().contains("Ningún año"),
                "El mensaje debe explicar por qué no se puede calcular: " + ex.getMessage());
    }

    @Test
    void filtraDeLaCestaLosProductosCompradosPocasVecesEnTodaLaSerie() {
        List<PurchasedItemRecord> records = new ArrayList<>();
        // PAN y HUEVOS: recurrentes (3 compras cada uno) -> entran en la cesta
        for (int month = 1; month <= 3; month++) {
            records.add(unitRecord("PAN", LocalDateTime.of(2024, month, 1, 10, 0)));
            records.add(unitRecord("HUEVOS", LocalDateTime.of(2024, month, 15, 10, 0)));
        }
        // QUESO: comprado 2 veces -> por debajo del umbral de 3, se excluye
        records.add(unitRecord("QUESO", LocalDateTime.of(2024, 1, 5, 10, 0)));
        records.add(unitRecord("QUESO", LocalDateTime.of(2024, 2, 5, 10, 0)));
        // CAPRICHO: comprado una sola vez -> se excluye
        records.add(unitRecord("CAPRICHO", LocalDateTime.of(2024, 1, 20, 10, 0)));

        InflationReport report = InflationAnalyzer.analyze(records, MIN_COMPLETE_MONTHS, MIN_TOTAL_PURCHASE_COUNT);

        List<String> basketIds = report.basket().stream().map(ProductKey::id).toList();
        assertTrue(basketIds.contains("PAN"));
        assertTrue(basketIds.contains("HUEVOS"));
        assertFalse(basketIds.contains("QUESO"), "Comprado solo 2 veces, por debajo del umbral de 3");
        assertFalse(basketIds.contains("CAPRICHO"), "Comprado una sola vez");
    }
}
