package org.terra.incognita.ticketdigital.mercadona.gmail;

import java.io.IOException;

/**
 * Consulta a Google el alcance (scope) realmente concedido a un access token. Aislado en
 * una interfaz para poder sustituirlo en tests sin hacer peticiones de red reales.
 */
@FunctionalInterface
public interface TokenScopeChecker {

    /**
     * @return el scope (o scopes, separados por espacio) concedido al token, o {@code null}
     * si la respuesta no lo incluye.
     */
    String fetchGrantedScope(String accessToken) throws IOException;
}
