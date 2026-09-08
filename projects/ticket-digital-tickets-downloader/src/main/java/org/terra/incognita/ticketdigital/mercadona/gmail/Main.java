package org.terra.incognita.ticketdigital.mercadona.gmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Interfaz de línea de comandos independiente: descarga los tickets nuevos desde Gmail
 * sin depender de {@code ticket-digital-data}. Ese otro módulo expone el mismo comando
 * como subcomando {@code download-tickets}, delegando en {@link GmailTicketDownloader}.
 */
@Command(name = "download-tickets",
         mixinStandardHelpOptions = true,
         version = "1.0",
         description = "Descarga desde Gmail los PDF de tickets nuevos (no descargados aún) a un directorio local.")
public class Main implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = "--data-dir",
            description = "Directorio donde se guardan los PDF descargados (por defecto: ~/.ticket-digital/data).",
            paramLabel = "<directorio-datos>")
    private String dataDir;

    @Option(names = "--credentials-file",
            description = "Fichero credentials.json obtenido de Google Cloud Console "
                    + "(por defecto: ~/.ticket-digital/credentials.json). Ver README.md.",
            paramLabel = "<credentials.json>")
    private String credentialsFile;

    @Option(names = "--token-dir",
            description = "Directorio donde se guarda el token OAuth obtenido tras autenticarse "
                    + "(por defecto: ~/.ticket-digital/tokens).",
            paramLabel = "<directorio-token>")
    private String tokenDir;

    @Option(names = "--state-file",
            description = "Fichero donde se guarda el estado de la última descarga, para que la "
                    + "descarga sea incremental (por defecto: ~/.ticket-digital/state/gmail-download-state.properties).",
            paramLabel = "<fichero-estado>")
    private String stateFile;

    @Option(names = "--query",
            description = "Query de búsqueda de Gmail (por defecto: '${DEFAULT-VALUE}').",
            defaultValue = GmailDownloadDefaults.QUERY)
    private String query;

    @Option(names = "--filename-regex",
            description = "Expresión regular que debe matchear el nombre del adjunto a descargar "
                    + "(por defecto: '${DEFAULT-VALUE}').",
            defaultValue = GmailDownloadDefaults.FILENAME_REGEX)
    private String filenameRegex;

    @Option(names = {"-v", "--verbose"},
            description = "Activa logs de nivel DEBUG, con el detalle de qué está haciendo la descarga "
                    + "en cada momento (mensaje que se está procesando, adjuntos encontrados, etc).")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        Verbosity.apply(verbose);
        DownloadRequest request = new DownloadRequest(
                dataDir != null ? UserPaths.resolve(dataDir) : GmailDownloadDefaults.dataDir(),
                credentialsFile != null ? UserPaths.resolve(credentialsFile) : GmailDownloadDefaults.credentialsFile(),
                tokenDir != null ? UserPaths.resolve(tokenDir) : GmailDownloadDefaults.tokenDirectory(),
                stateFile != null ? UserPaths.resolve(stateFile) : GmailDownloadDefaults.stateFile(),
                query,
                filenameRegex);
        int downloaded = new GmailTicketDownloader().download(request);
        logger.info("Descargados {} ticket(s) nuevo(s) en {}", downloaded, request.dataDir());
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
