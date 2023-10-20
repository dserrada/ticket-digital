package org.terra.incognita.ticketdigital.mercadona.model;

/**
 * Información de un tipo de articulo comprado en este ticket
 *
 * @param descripcion    El nombre del artículo
 * @param cantidad       Cantidad de producto vendido (pueden ser uniades, kilos, ...)
 * @param precioUnitario Precio en euros de cada unidad del artículo (ojo, al ser euros el redondeo será a centimos)
 */
public record Articulo(String descripcion, Cantidad cantidad, float precioUnitario) {
    /**
     * El precio calculado de todas las unidades
     *
     * FIXME: No se si es buena idea, ya que hay problemas con el redondeo.
     * Los precios realmente se deberian expresar como enteros de centimos (que es lo que realmente sirve)
     * @return
     */
    public float precioTotalCalculado() {
        float total = 0;
        switch ( cantidad.tipo() ) {
            case UNIDAD -> total += Math.round(cantidad.numero()) * precioUnitario;
            case KILO -> total += cantidad.numero() * precioUnitario;
        }
        return  (float) (Math.round(total * 100.0) / 100.0);
    }
}
