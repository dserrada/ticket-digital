package org.terra.incognita.ticketdigital.mercadona;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "ticket-digital-data",
         mixinStandardHelpOptions = true,
         version = "1.0",
         description = "Interfaz de línea de comandos para procesar tickets digitales de Mercadona ya descargados. "
                 + "Para descargarlos desde Gmail, usa ticket-digital-tickets-downloader "
                 + "(o ticket-digital-launcher, que incluye ambos).",
         subcommands = {WriteItemsCsvCommand.class, WriteItemsXlsxCommand.class, InflationIndexCommand.class, BasketCompositionCommand.class})
public class Main implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
