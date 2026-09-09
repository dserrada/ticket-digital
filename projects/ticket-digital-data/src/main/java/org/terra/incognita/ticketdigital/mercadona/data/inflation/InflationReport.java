package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.util.List;

/**
 * Resultado completo del análisis de inflación: año base, año usado para construir la
 * cesta, la propia cesta y los índices de cada año de la serie.
 */
public record InflationReport(int baseYear, int basketYear, List<ProductKey> basket,
                               List<YearInflationIndex> yearIndices) {
}
