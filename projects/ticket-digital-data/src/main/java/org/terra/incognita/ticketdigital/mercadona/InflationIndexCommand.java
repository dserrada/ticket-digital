package org.terra.incognita.ticketdigital.mercadona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.InflationAnalyzer;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.InflationReport;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.InflationReportFormatter;
import org.terra.incognita.ticketdigital.mercadona.data.items.PurchasedItemRecord;
import org.terra.incognita.ticketdigital.mercadona.data.items.TicketRecordsReader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Comando de clase (no de método): igual que {@link WriteItemsCsvCommand}, para poder
 * reutilizarse tal cual como subcomando de {@code ticket-digital-data} y de
 * {@code ticket-digital-launcher} sin duplicar lógica.
 */
@Command(name = "inflation-index",
         mixinStandardHelpOptions = true,
         description = "Calcula y muestra por pantalla el índice de inflación anual de la cesta de la compra "
                 + "(índices de Laspeyres y Paasche).")
public class InflationIndexCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(InflationIndexCommand.class);

    @Option(names = {"--data-dir"}, description = "Directorio donde están los ficheros pdf con los tickets a analizar.", required = true, paramLabel = "<directorio-datos>")
    private String dataDir;

    @Option(names = {"-v", "--verbose"}, description = "Activa logs de nivel DEBUG y lista los productos de la cesta.")
    private boolean verbose;

    @Override
    public Integer call() throws IOException {
        Verbosity.apply(verbose);
        logger.debug("Ejecutando operación inflation-index...");
        Path resolvedDataDir = UserPaths.resolve(dataDir);
        List<PurchasedItemRecord> records = TicketRecordsReader.readAll(resolvedDataDir);
        if (records == null) return 1;

        try {
            InflationReport report = InflationAnalyzer.analyze(records);
            System.out.print(InflationReportFormatter.format(report, verbose));
            return 0;
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
