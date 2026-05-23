package org.terra.incognita.ticketdigital.mercadona.model;

import java.util.ListIterator;

/**
 * Mantiene la información del estado del parseador del ticket de Mercadona.
 *
 * Además del iterador de líneas, almacena el estado de la sección de productos frescos
 * activa (si la hay), de forma que el parser de FreshItemByWeight puede procesar
 * un artículo por llamada sin perder el contexto de la cabecera de categoría.
 */
public class ParserStatusInfo {
    /**
     * El texto del ticket que estamos parseando como un iterator de lineas
     */
    private ListIterator<String> ticketTextLinesIterator;

    /**
     * Si estamos en un bloque de prodctos frescos (pescado de momento) aquí guardamos el tipo
     * de producto fresco, además y como se analiza producto a producto, aquí sabemos si estamos
     * parseando productos frescos que al fin y al cabo son productos al peso pero organizados en
     * una categoria).
     * Es decir, si esto es null no estamos parseando productos frescos. Si tiene valor indica el nombre
     * del grupo de productos frescos que estamos parseando.
     */
    private String currentFreshType = null;

    /**
     * Ya que los productos frescos se caracterizan por tener una indentación mayor que la de los productos
     * normales, guardamos la indentación de la cabecera de categoría para poder distingir cambios en la indentación.
     */
    private int freshHeaderIndent = -1;

    /**
     * Constructor para ParserStatusInfo.
     *
     * @param ticketData Contenido en texto del fichero ticket.
     */
    public ParserStatusInfo(String ticketData) {
        this.ticketTextLinesIterator = ticketData.lines().toList().listIterator();
    }

    /**
     * Comprueba si hay más líneas para leer.
     *
     * @return true si hay más líneas, false en caso contrario.
     */
    public boolean hasNext() {
        return ticketTextLinesIterator.hasNext();
    }

    /**
     * Devuelve la siguiente línea del ticket.
     *
     * @return La siguiente línea.
     */
    public String next() {
        return ticketTextLinesIterator.next();
    }

    /**
     * Vuelve atrás un número determinado de posiciones.
     *
     * @param n Número de posiciones a volver atrás.
     */
    public void rollback(int n) {
        for (int i = 0; i < n; i++) {
            if (ticketTextLinesIterator.hasPrevious()) {
                ticketTextLinesIterator.previous();
            }
        }
    }

    /**
     * Devuelve el índice de la siguiente línea que se va a leer.
     *
     * @return El índice de la siguiente línea.
     */
    public int nextIndex() {
        return ticketTextLinesIterator.nextIndex();
    }

    /**
     * Devuelve el índice de la línea que se acaba de leer.
     *
     * @return El índice de la línea leída.
     */
    public int previousIndex() {
        return ticketTextLinesIterator.previousIndex();
    }

    public boolean isInFreshSection() {
        return currentFreshType != null;
    }

    public String getCurrentFreshType() {
        return currentFreshType;
    }

    public int getFreshHeaderIndent() {
        return freshHeaderIndent;
    }

    public void enterFreshSection(String freshType, int headerIndent) {
        this.currentFreshType = freshType;
        this.freshHeaderIndent = headerIndent;
    }

    public void exitFreshSection() {
        this.currentFreshType = null;
        this.freshHeaderIndent = -1;
    }
}
