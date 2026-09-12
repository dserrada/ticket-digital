package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cantidad total comprada y gasto total de un producto en un año concreto.
 *
 * @param totalQuantity cantidad total comprada ese año (unidades o kg, según el {@link ProductType})
 * @param totalSpent    importe total gastado ese año en ese producto
 */
public record ProductYearStats(BigDecimal totalQuantity, BigDecimal totalSpent) {

    public static final ProductYearStats EMPTY = new ProductYearStats(BigDecimal.ZERO, BigDecimal.ZERO);

    public ProductYearStats plus(BigDecimal quantity, BigDecimal spent) {
        return new ProductYearStats(totalQuantity.add(quantity), totalSpent.add(spent));
    }

    /**
     * Precio medio ponderado de ese producto ese año (gasto total / cantidad total).
     *
     * @return el precio medio, o {@code null} si no se compró nada ese año
     */
    public BigDecimal averagePrice() {
        if (totalQuantity.signum() == 0) return null;
        return totalSpent.divide(totalQuantity, MathContext.DECIMAL64);
    }
}
