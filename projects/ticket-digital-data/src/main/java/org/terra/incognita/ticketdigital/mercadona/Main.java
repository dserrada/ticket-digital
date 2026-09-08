package org.terra.incognita.ticketdigital.mercadona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.items.CSVGenerator;
import org.terra.incognita.ticketdigital.mercadona.data.items.XlsxDatosWriter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(name = "ticket-digital-data",
         mixinStandardHelpOptions = true,
         version = "1.0",
         description = "Interfaz de línea de comandos para procesar tickets digitales de Mercadona ya descargados. "
                 + "Para descargarlos desde Gmail, usa ticket-digital-tickets-downloader "
                 + "(o ticket-digital-launcher, que incluye ambos).")
public class Main implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);


    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "write-items-csv",
             mixinStandardHelpOptions = true,
             description = "Genera un fichero csv con la información de todos los items comprados.")
    public Integer writeItemsCsv(
            @Option(names = {"--data-dir"}, description = "Directorio donde están los ficheros pdf con los tickets a analizar.", required = true, paramLabel = "<directorio-datos>") String dataDir,
            @Option(names = {"--csv-file"}, description = "Nombre del fichero donde se escribe la información en csv.", required = true, paramLabel = "<fichero-csv>") String csvFile,
            @Option(names = {"-v", "--verbose"}, description = "Activa logs de nivel DEBUG.") boolean verbose)
    throws IOException {
        Verbosity.apply(verbose);
        // De momento solo el interfaz de linea de comandos, no enganchar con la lógica
        logger.debug("Ejecutando operación write-items-csv...");
        Path resolvedDataDir = UserPaths.resolve(dataDir);
        File resolvedCsvFile = UserPaths.resolve(csvFile).toFile();
        CSVGenerator.writeCSVToFile(resolvedDataDir, resolvedCsvFile);
        return 0;
    }

    @Command(name = "write-items-xlsx",
             mixinStandardHelpOptions = true,
             description = "Genera, a partir de la plantilla Mercadona-base.xlsx, un fichero xlsx con la información de todos los items comprados.")
    public Integer writeItemsXlsx(
            @Option(names = {"--data-dir"}, description = "Directorio donde están los ficheros pdf con los tickets a analizar.", required = true, paramLabel = "<directorio-datos>") String dataDir,
            @Option(names = {"--output-dir"}, description = "Directorio donde se escribe el fichero xlsx generado (Mercadona-yyyyMMdd.xlsx).", required = true, paramLabel = "<directorio-salida>") String outputDir,
            @Option(names = {"-v", "--verbose"}, description = "Activa logs de nivel DEBUG.") boolean verbose)
    throws IOException {
        Verbosity.apply(verbose);
        logger.debug("Ejecutando operación write-items-xlsx...");
        Path resolvedDataDir = UserPaths.resolve(dataDir);
        File resolvedOutputDir = UserPaths.resolve(outputDir).toFile();
        String fileName = "Mercadona-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + ".xlsx";
        XlsxDatosWriter.writeXlsxToFile(resolvedDataDir, new File(resolvedOutputDir, fileName));
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
