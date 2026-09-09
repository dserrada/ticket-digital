package org.terra.incognita.ticketdigital.mercadona.data.inflation;

/**
 * Cómo se compra un producto: por unidades o por peso. Un mismo nombre de producto
 * comprado a veces de una forma y a veces de otra se trata como dos productos
 * distintos de cara a la cesta de la compra, ya que su precio y cantidad no son
 * comparables entre sí (€/ud frente a €/kg).
 */
public enum ProductType {
    UNIDAD,
    PESO
}
