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
     * @param ticketData Contenido en texto del fichero ticket.
     */
    public ParserStatusInfo(String ticketData) {
        this.iterator = ticketData.lines().toList().listIterator();
    }

    /**
     * Comprueba si hay más líneas para leer.
     *
     * @return true si hay más líneas, false en caso contrario.
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Devuelve la siguiente línea del ticket.
     *
     * @return La siguiente línea.
     */
    public String next() {
        return iterator.next();
    }

    /**
     * Vuelve atrás un número determinado de posiciones.
     *
     * @param n Número de posiciones a volver atrás.
     */
    public void rollback(int n) {
        for (int i = 0; i < n; i++) {
            if (iterator.hasPrevious()) {
                iterator.previous();
            }
        }
    }

    /**
     * Devuelve el índice de la siguiente línea que se va a leer.
     *
     * @return El índice de la siguiente línea.
     */
    public int nextIndex() {
        return iterator.nextIndex();
    }

    /**
     * Devuelve el índice de la línea que se acaba de leer.
     *
     * @return El índice de la línea leída.
     */
    public int previousIndex() {
        return iterator.previousIndex();
    }
}
