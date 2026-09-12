package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;

/**
 * Índices de inflación de un año frente al año base.
 *
 * @param laspeyres índice de Laspeyres (base=100), o {@code null} si ningún producto de
 *                   la cesta tiene precio comparable ese año
 * @param paasche índice de Paasche (base=100), o {@code null} en las mismas condiciones
 * @param matchedProductCount número de productos de la cesta con precio conocido tanto
 *                             en el año base como en este año (los que sí entran en las sumas)
 */
public record YearInflationIndex(int year, boolean complete, boolean baseYear,
                                  BigDecimal laspeyres, BigDecimal paasche, int matchedProductCount) {
}
