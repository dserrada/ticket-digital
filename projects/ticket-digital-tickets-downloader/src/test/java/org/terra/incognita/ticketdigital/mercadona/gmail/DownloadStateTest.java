package org.terra.incognita.ticketdigital.mercadona.gmail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadStateTest {

    private Path tempDir;

    @BeforeEach
    void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("gmail-download-state-test");
    }

    @AfterEach
    void deleteTempDir() throws IOException {
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void sinEstadoPrevioNoAnadeClausulaAfter() {
        String query = DownloadState.initial().appendIncrementalClause("label:mercadona");

        assertEquals("label:mercadona", query);
    }

    @Test
    void conEstadoPrevioAnadeClausulaAfterConSolapeDeUnDia() {
        Instant lastMessageDate = Instant.parse("2026-06-15T10:00:00Z");
        DownloadState state = DownloadState.initial().withLastMessageDate(lastMessageDate);

        String query = state.appendIncrementalClause("label:mercadona");

        long expectedEpochSeconds = lastMessageDate.minus(Duration.ofDays(1)).getEpochSecond();
        assertEquals("label:mercadona after:" + expectedEpochSeconds, query);
    }

    @Test
    void withLastMessageDateSoloAvanzaHaciaAdelante() {
        Instant later = Instant.parse("2026-06-15T10:00:00Z");
        Instant earlier = Instant.parse("2026-06-01T10:00:00Z");
        DownloadState state = DownloadState.initial().withLastMessageDate(later).withLastMessageDate(earlier);

        assertEquals(later, state.lastMessageDate());
    }

    @Test
    void persisteYRecuperaElEstadoDeUnFichero() throws IOException {
        Path stateFile = tempDir.resolve("state").resolve("gmail-download-state.properties");
        Instant lastMessageDate = Instant.parse("2026-06-15T10:00:00Z");
        DownloadState.initial().withLastMessageDate(lastMessageDate).save(stateFile);

        DownloadState loaded = DownloadState.load(stateFile);

        assertTrue(Files.exists(stateFile));
        assertEquals(lastMessageDate, loaded.lastMessageDate());
    }

    @Test
    void loadDevuelveEstadoInicialSiElFicheroNoExiste() throws IOException {
        DownloadState loaded = DownloadState.load(tempDir.resolve("no-existe.properties"));

        assertEquals(Instant.EPOCH, loaded.lastMessageDate());
    }
}
