package org.terra.incognita.ticketdigital.mercadona.gmail;

import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;

/**
 * Comprueba que el token OAuth concedido tiene <strong>únicamente</strong> el alcance de
 * solo lectura de Gmail. Nunca se debe continuar la descarga con un token que tenga
 * permisos de escritura/modificación: si esto ocurriera (por ejemplo, por reutilizar un
 * token generado antes con más permisos), se aborta antes de hacer ninguna llamada a la
 * API de mensajes.
 */
public final class GmailScopeValidator {

    private GmailScopeValidator() {
    }

    public static void assertReadOnly(String grantedScope) {
        if (grantedScope == null || grantedScope.isBlank()) {
            throw new IllegalStateException(
                    "No se ha podido determinar el alcance (scope) concedido al token OAuth; "
                            + "se aborta la descarga por seguridad.");
        }
        String[] scopes = grantedScope.trim().split("\\s+");
        boolean onlyReadOnly = scopes.length == 1 && GmailScopes.GMAIL_READONLY.equals(scopes[0]);
        if (!onlyReadOnly) {
            throw new IllegalStateException(
                    "El token OAuth tiene un alcance distinto al esperado (" + GmailScopes.GMAIL_READONLY
                            + "): [" + String.join(", ", Arrays.asList(scopes)) + "]. Se aborta la descarga por "
                            + "seguridad para no arriesgarse a modificar el contenido de Gmail.");
        }
    }
}
