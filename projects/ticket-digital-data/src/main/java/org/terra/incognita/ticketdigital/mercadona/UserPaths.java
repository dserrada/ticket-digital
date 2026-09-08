package org.terra.incognita.ticketdigital.mercadona;

import java.nio.file.Path;

/**
 * Expande un {@code ~} inicial al directorio del usuario, igual que hacen las shells
 * Unix. Necesario porque Java no lo hace por sí solo: si la shell que invoca el programa
 * no expande el {@code ~} (por ejemplo cmd.exe o PowerShell en Windows, o si el argumento
 * va entre comillas), llega literalmente como parte del nombre de fichero/directorio, y
 * el programa acaba creando una carpeta llamada {@code ~} en vez de resolver el home real.
 */
final class UserPaths {

    private UserPaths() {
    }

    static Path resolve(String raw) {
        if (raw.equals("~")) {
            return Path.of(System.getProperty("user.home"));
        }
        if (raw.startsWith("~/") || raw.startsWith("~\\")) {
            return Path.of(System.getProperty("user.home")).resolve(raw.substring(2));
        }
        return Path.of(raw);
    }
}
