package org.terra.incognita.ticketdigital.mercadona.gmail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

/**
 * Estado persistido entre ejecuciones para que la descarga sea incremental: recuerda
 * la fecha del mensaje más reciente ya procesado, para no volver a pedir a Gmail todo
 * el histórico en cada ejecución.
 * <p>
 * Se aplica un margen de solape ({@link #OVERLAP}) al construir la cláusula
 * {@code after:} de la query, para cubrir mensajes que pudieran llegar con fecha
 * ligeramente anterior a la última vista (zonas horarias, entregas tardías). El propio
 * {@link GmailTicketDownloader} evita redescargar ficheros que ya existen en disco, lo
 * que hace inofensivo ese solape.
 */
public final class DownloadState {

    private static final String LAST_MESSAGE_DATE_KEY = "lastMessageDate";
    private static final Duration OVERLAP = Duration.ofDays(1);

    private final Instant lastMessageDate;

    private DownloadState(Instant lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public static DownloadState initial() {
        return new DownloadState(Instant.EPOCH);
    }

    public static DownloadState load(Path stateFile) throws IOException {
        if (!Files.exists(stateFile)) {
            return initial();
        }
        Properties properties = new Properties();
        try (var in = Files.newInputStream(stateFile)) {
            properties.load(in);
        }
        String value = properties.getProperty(LAST_MESSAGE_DATE_KEY);
        return value == null ? initial() : new DownloadState(Instant.parse(value));
    }

    public void save(Path stateFile) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(LAST_MESSAGE_DATE_KEY, lastMessageDate.toString());
        Path parent = stateFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var out = Files.newOutputStream(stateFile)) {
            properties.store(out, "Estado de ticket-digital-tickets-downloader: no editar a mano.");
        }
    }

    public Instant lastMessageDate() {
        return lastMessageDate;
    }

    public DownloadState withLastMessageDate(Instant candidate) {
        return candidate.isAfter(lastMessageDate) ? new DownloadState(candidate) : this;
    }

    /**
     * Añade a la query base la cláusula {@code after:} correspondiente al estado actual,
     * o la devuelve sin modificar si aún no hay estado previo (primera ejecución).
     */
    public String appendIncrementalClause(String baseQuery) {
        if (lastMessageDate.equals(Instant.EPOCH)) {
            return baseQuery;
        }
        long afterEpochSeconds = lastMessageDate.minus(OVERLAP).getEpochSecond();
        return baseQuery + " after:" + afterEpochSeconds;
    }
}
