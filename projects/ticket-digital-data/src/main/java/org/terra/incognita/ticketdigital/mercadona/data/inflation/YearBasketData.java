package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.util.Map;

/**
 * Datos agregados de un año: cuántos meses distintos tuvieron alguna compra (para
 * decidir si es un año "completo" y por tanto fiable como año base o para elegir la
 * cesta), y las estadísticas de cada producto comprado ese año.
 *
 * @param complete si tuvo compras en al menos el número mínimo de meses configurado
 */
public record YearBasketData(int year, int monthsWithPurchases, boolean complete,
                              Map<ProductKey, ProductYearStats> productStats) {

    public YearBasketData {
        productStats = Map.copyOf(productStats);
    }

    public int distinctProductCount() {
        return productStats.size();
    }
}
