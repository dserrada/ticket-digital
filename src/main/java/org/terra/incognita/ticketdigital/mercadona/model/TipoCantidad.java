package org.terra.incognita.ticketdigital.mercadona.model;

/**
 * El tipo de cantidad en la que se mide los articulos
 *
 *  El artículo puede venderse por unidades o por kilos (también podría
 *  por litros, pero nunca lo he visto)
 */
public enum TipoCantidad {
    /**
     * El producto se vende por unidades
     */
    UNIDAD,
    /**
     * El producto se vende por kilos
     */
    KILO
}
