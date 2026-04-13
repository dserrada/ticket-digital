package org.terra.incognita.ticketdigital.mercadona.model;

import java.util.ListIterator;

/**
 * Mantiene la información del estado del parseador del ticket de Mercadona.
 */
public class ParserStatusInfo {
    private ListIterator<String> iterator;

    /**
     * Constructor para ParserStatusInfo.
     *
     * @param iterator Iterador sobre las líneas del ticket.
     */
    public ParserStatusInfo(ListIterator<String> iterator) {
        this.iterator = iterator;
    }

    /**
     * Devuelve el iterador sobre las líneas del ticket.
     *
     * @return El iterador.
     */
    public ListIterator<String> iterator() {
        return iterator;
    }
}
