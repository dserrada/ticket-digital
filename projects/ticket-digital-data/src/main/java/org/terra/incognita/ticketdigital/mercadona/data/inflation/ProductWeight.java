package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;

/**
 * Peso relativo de un producto de la cesta dentro del gasto total de la cesta en el
 * año base — lo que en un índice de precios se llama el "peso" de ese producto
 * (proporción del gasto total que representa).
 *
 * @param baseYearSpend importe gastado en ese producto en el año base
 * @param weightPercent {@code baseYearSpend} como porcentaje del gasto total de la cesta en el año base
 * @param hasBaseYearData si el producto tiene datos en el año base; si no los tiene (puede
 *                         pasar cuando el año-cesta es distinto del año base), su peso es 0
 *                         y queda excluido de los índices de inflación (método matched-model)
 */
public record ProductWeight(ProductKey key, BigDecimal baseYearSpend, BigDecimal weightPercent, boolean hasBaseYearData) {
}
