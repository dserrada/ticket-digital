package org.terra.incognita.ticketdigital.mercadona;

import java.nio.file.Path;

/**
 * Valores por defecto usados cuando no se especifica {@code --data-dir}. Coincide con
 * {@code GmailDownloadDefaults.dataDir()} de ticket-digital-tickets-downloader: ese es el
 * directorio donde {@code download-tickets} guarda los PDF por defecto, así que es el
 * directorio natural para analizar si no se indica otro.
 */
final class TicketDataDefaults {

    private TicketDataDefaults() {
    }

    static Path dataDir() {
        return Path.of(System.getProperty("user.home"), ".ticket-digital", "data");
    }
}
