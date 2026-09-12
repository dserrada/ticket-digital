package org.terra.incognita.ticketdigital.mercadona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.BasketComposition;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.BasketCompositionFormatter;
import org.terra.incognita.ticketdigital.mercadona.data.inflation.InflationAnalyzer;
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
@Command(name = "basket-composition",
         mixinStandardHelpOptions = true,
         description = "Muestra por pantalla la cesta de la compra usada para el índice de inflación "
                 + "(inflation-index) y el peso relativo de cada producto en el gasto del año base.")
public class BasketCompositionCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(BasketCompositionCommand.class);

    @Option(names = {"--data-dir"},
            description = "Directorio donde están los ficheros pdf con los tickets a analizar "
                    + "(por defecto: ~/.ticket-digital/data).",
            paramLabel = "<directorio-datos>")
    private String dataDir;

    @Option(names = {"-v", "--verbose"}, description = "Activa logs de nivel DEBUG.")
    private boolean verbose;

    @Override
    public Integer call() throws IOException {
        Verbosity.apply(verbose);
        logger.debug("Ejecutando operación basket-composition...");
        Path resolvedDataDir = dataDir != null ? UserPaths.resolve(dataDir) : TicketDataDefaults.dataDir();
        List<PurchasedItemRecord> records = TicketRecordsReader.readAll(resolvedDataDir);
        if (records == null) return 1;

        try {
            BasketComposition composition = InflationAnalyzer.analyzeBasketComposition(records);
            System.out.print(BasketCompositionFormatter.format(composition));
            return 0;
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
