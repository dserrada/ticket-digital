package org.terra.incognita.ticketdigital.mercadona.gmail;

import java.nio.file.Path;

/**
 * Parámetros de una ejecución de descarga de tickets desde Gmail.
 */
public record DownloadRequest(
        Path dataDir,
        Path credentialsFile,
        Path tokenDirectory,
        Path stateFile,
        String query,
        String filenameRegex) {
}
