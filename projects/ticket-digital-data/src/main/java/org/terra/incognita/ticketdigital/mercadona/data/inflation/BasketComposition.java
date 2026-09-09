package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Composición de la cesta de la compra: qué productos la forman y el peso relativo de
 * cada uno, calculado sobre el gasto del año base.
 *
 * @param weights ordenados de mayor a menor peso
 */
public record BasketComposition(int baseYear, int basketYear, BigDecimal totalBaseYearSpend, List<ProductWeight> weights) {
}
