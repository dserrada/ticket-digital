package org.terra.incognita.ticketdigital.mercadona.gmail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GmailTicketDownloaderTest {

    private static final String FILENAME = "20261002 Mercadona 63,03 €.pdf";
    private static final String MESSAGE_ID = "1a0fdcec7973cfd7";
    private static final byte[] TICKET = "ticket 3484-012-019714".getBytes(StandardCharsets.UTF_8);

    @Test
    void ficheroNuevoSeGuardaConSuNombre(@TempDir Path dataDir) throws IOException {
        assertEquals(dataDir.resolve(FILENAME),
                GmailTicketDownloader.uniqueTarget(dataDir, MESSAGE_ID, FILENAME, TICKET));
    }

    @Test
    void elMismoAdjuntoYaDescargadoNoSeVuelveAGuardar(@TempDir Path dataDir) throws IOException {
        // Reproduce el bug: la primera descarga se guarda sin el id del mensaje, y al volver a
        // pasar por el mismo mensaje (solape de DownloadState) se guardaba una segunda copia
        // idéntica como "...-<idMensaje>.pdf".
        Files.write(dataDir.resolve(FILENAME), TICKET);

        assertNull(GmailTicketDownloader.uniqueTarget(dataDir, MESSAGE_ID, FILENAME, TICKET));
    }

    @Test
    void mismoNombreConOtroContenidoSeGuardaConElIdDelMensaje(@TempDir Path dataDir) throws IOException {
        Files.write(dataDir.resolve(FILENAME), "otro ticket".getBytes(StandardCharsets.UTF_8));

        assertEquals(dataDir.resolve("20261002 Mercadona 63,03 €-" + MESSAGE_ID + ".pdf"),
                GmailTicketDownloader.uniqueTarget(dataDir, MESSAGE_ID, FILENAME, TICKET));
    }

    @Test
    void adjuntoYaGuardadoConElIdDelMensajeNoSeVuelveAGuardar(@TempDir Path dataDir) throws IOException {
        Files.write(dataDir.resolve(FILENAME), "otro ticket".getBytes(StandardCharsets.UTF_8));
        Files.write(dataDir.resolve("20261002 Mercadona 63,03 €-" + MESSAGE_ID + ".pdf"), TICKET);

        assertNull(GmailTicketDownloader.uniqueTarget(dataDir, MESSAGE_ID, FILENAME, TICKET));
    }
}
