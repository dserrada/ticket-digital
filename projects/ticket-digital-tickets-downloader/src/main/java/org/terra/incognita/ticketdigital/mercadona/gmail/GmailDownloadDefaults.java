package org.terra.incognita.ticketdigital.mercadona.gmail;

import java.nio.file.Path;

/**
 * Valores por defecto usados cuando no se especifican opciones en la línea de comandos.
 * Todo lo relacionado con secretos (credenciales, token) y estado vive fuera del
 * repositorio, bajo el directorio del usuario.
 */
public final class GmailDownloadDefaults {

    public static final String QUERY = "label:mercadona from: ticket_digital@mail.mercadona.com";
    public static final String FILENAME_REGEX = ".*\\.pdf$";

    private static final String STATE_FILE_NAME = "gmail-download-state.properties";

    private GmailDownloadDefaults() {
    }

    private static Path homeDir() {
        return Path.of(System.getProperty("user.home"), ".ticket-digital");
    }

    public static Path dataDir() {
        return homeDir().resolve("data");
    }

    public static Path credentialsFile() {
        return homeDir().resolve("credentials.json");
    }

    public static Path tokenDirectory() {
        return homeDir().resolve("tokens");
    }

    public static Path stateFile() {
        return homeDir().resolve("state").resolve(STATE_FILE_NAME);
    }
}
