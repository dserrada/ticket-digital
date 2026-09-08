package org.terra.incognita.ticketdigital.mercadona.launcher;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * CLI "todo en uno": compone en un único comando los subcomandos que
 * {@code ticket-digital-data} y {@code ticket-digital-tickets-downloader} ya exponen
 * cada uno por separado en su propio {@code Main}. No duplica lógica ni opciones: reutiliza
 * tal cual los {@link CommandLine} que cada módulo construye para sí mismo.
 * <p>
 * Quien solo necesite una de las dos funcionalidades puede seguir ejecutando el módulo
 * correspondiente de forma independiente (cargando solo sus propias dependencias):
 * {@code ticket-digital-data} (lectura/generación de CSV/XLSX) o
 * {@code ticket-digital-tickets-downloader} (descarga desde Gmail).
 */
@Command(name = "ticket-digital",
         mixinStandardHelpOptions = true,
         version = "1.0",
         description = "Interfaz de línea de comandos completa de ticket-digital: descarga de tickets desde "
                 + "Gmail y generación de CSV/XLSX a partir de ellos.")
public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        CommandLine root = new CommandLine(new Main());

        CommandLine dataCommandLine =
                new CommandLine(new org.terra.incognita.ticketdigital.mercadona.Main());
        for (Map.Entry<String, CommandLine> subcommand : dataCommandLine.getSubcommands().entrySet()) {
            root.getCommandSpec().addSubcommand(subcommand.getKey(), subcommand.getValue());
        }

        root.addSubcommand(new org.terra.incognita.ticketdigital.mercadona.gmail.Main());

        int exitCode = root.execute(args);
        System.exit(exitCode);
    }
}
