package org.terra.incognita.ticketdigital.mercadona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.items.CSVGenerator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Comando de clase (no de método): a diferencia de un subcomando de método picocli, su
 * receptor no depende de cuál sea el comando padre, así que se puede reutilizar tal cual
 * como subcomando de {@code ticket-digital-data} y de {@code ticket-digital-launcher}
 * sin duplicar lógica.
 */
@Command(name = "write-items-csv",
         mixinStandardHelpOptions = true,
         description = "Genera un fichero csv con la información de todos los items comprados.")
public class WriteItemsCsvCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(WriteItemsCsvCommand.class);

    @Option(names = {"--data-dir"},
            description = "Directorio donde están los ficheros pdf con los tickets a analizar "
                    + "(por defecto: ~/.ticket-digital/data).",
            paramLabel = "<directorio-datos>")
    private String dataDir;

    @Option(names = {"--csv-file"}, description = "Nombre del fichero donde se escribe la información en csv.", required = true, paramLabel = "<fichero-csv>")
    private String csvFile;

    @Option(names = {"-v", "--verbose"}, description = "Activa logs de nivel DEBUG.")
    private boolean verbose;

    @Override
    public Integer call() throws IOException {
        Verbosity.apply(verbose);
        logger.debug("Ejecutando operación write-items-csv...");
        Path resolvedDataDir = dataDir != null ? UserPaths.resolve(dataDir) : TicketDataDefaults.dataDir();
        File resolvedCsvFile = UserPaths.resolve(csvFile).toFile();
        CSVGenerator.writeCSVToFile(resolvedDataDir, resolvedCsvFile);
        return 0;
    }
}
