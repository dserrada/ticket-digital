package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InflationReportFormatterTest {

    private static InflationReport sampleReport() {
        List<YearInflationIndex> indices = List.of(
                new YearInflationIndex(2023, true, true, new BigDecimal("100.00"), new BigDecimal("100.00"), 2),
                new YearInflationIndex(2024, false, false, null, null, 0),
                new YearInflationIndex(2025, true, false, new BigDecimal("103.45"), new BigDecimal("97.80"), 2)
        );
        List<ProductKey> basket = List.of(new ProductKey("PAN", ProductType.UNIDAD));
        return new InflationReport(2023, 2023, basket, indices);
    }

    @Test
    void marcaElAnioBaseYLosAniosIncompletosEIndicesNulosComoNoDisponible() {
        String output = InflationReportFormatter.format(sampleReport(), false);

        assertTrue(output.contains("BASE"));
        assertTrue(output.contains("incompleto"));
        assertTrue(output.contains("n/d"));
    }

    @Test
    void muestraLosIndicesComoPorcentajeDeVariacionFrenteAlAnioBase() {
        String output = InflationReportFormatter.format(sampleReport(), false);

        // índice 103,45 (base=100) se muestra como variación: +3,45%
        assertTrue(output.contains("+3,45%"), output);
        // índice 97,80 se muestra como variación negativa: -2,20%
        assertTrue(output.contains("-2,20%"), output);
    }

    @Test
    void listaLaCestaSoloEnModoVerbose() {
        String normal = InflationReportFormatter.format(sampleReport(), false);
        String verbose = InflationReportFormatter.format(sampleReport(), true);

        assertFalse(normal.contains("Productos de la cesta"));
        assertTrue(verbose.contains("Productos de la cesta"));
        assertTrue(verbose.contains("PAN"));
    }
}
