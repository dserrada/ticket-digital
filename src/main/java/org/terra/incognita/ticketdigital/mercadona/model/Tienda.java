package org.terra.incognita.ticketdigital.mercadona.model;

/**
 * Información de la tienda donde se ha generado el ticket digital.
 *
 * @param direccion    Nombre de la calle donde se encuentra la tienda
 * @param codigoPostal Codigo postal como cadena
 * @param provincia    Nombre de la provincia donde se encuentra la tienda
 * @param telefono     Número de teléfono asociado a la tienda (como cadena)
 */
public record Tienda(String direccion, String codigoPostal, String provincia, String telefono) {
}
