package org.terra.incognita.ticketdigital.mercadona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.items.XlsxDatosWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

/**
 * Comando de clase (no de método): a diferencia de un subcomando de método picocli, su
 * receptor no depende de cuál sea el comando padre, así que se puede reutilizar tal cual
 * como subcomando de {@code ticket-digital-data} y de {@code ticket-digital-launcher}
 * sin duplicar lógica.
 */
@Command(name = "write-items-xlsx",
         mixinStandardHelpOptions = true,
         description = "Genera, a partir de la plantilla Mercadona-base.xlsx, un fichero xlsx con la información de todos los items comprados.")
public class WriteItemsXlsxCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(WriteItemsXlsxCommand.class);

    @Option(names = {"--data-dir"},
            description = "Directorio donde están los ficheros pdf con los tickets a analizar "
                    + "(por defecto: ~/.ticket-digital/data).",
            paramLabel = "<directorio-datos>")
    private String dataDir;

    @Option(names = {"--output-dir"}, description = "Directorio donde se escribe el fichero xlsx generado (Mercadona-yyyyMMdd.xlsx).", required = true, paramLabel = "<directorio-salida>")
    private String outputDir;

    @Option(names = {"-v", "--verbose"}, description = "Activa logs de nivel DEBUG.")
    private boolean verbose;

    @Override
    public Integer call() throws IOException {
        Verbosity.apply(verbose);
        logger.debug("Ejecutando operación write-items-xlsx...");
        Path resolvedDataDir = dataDir != null ? UserPaths.resolve(dataDir) : TicketDataDefaults.dataDir();
        File resolvedOutputDir = UserPaths.resolve(outputDir).toFile();
        String fileName = "Mercadona-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + ".xlsx";
        XlsxDatosWriter.writeXlsxToFile(resolvedDataDir, new File(resolvedOutputDir, fileName));
        return 0;
    }
}
