package org.terra.incognita.ticketdigital.mercadona.launcher;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * CLI "todo en uno": compone en un único comando los subcomandos que
 * {@code ticket-digital-data} y {@code ticket-digital-tickets-downloader} ya exponen
 * cada uno por separado. No duplica lógica ni opciones: reutiliza tal cual las clases de
 * comando de cada módulo (todas ellas comandos de clase, no de método — un subcomando de
 * método picocli resuelve su receptor a través del comando padre, así que reparentarlo
 * bajo otra raíz rompe la ejecución; un comando de clase no tiene ese problema).
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
                 + "Gmail y generación de CSV/XLSX a partir de ellos.",
         subcommands = {
                 org.terra.incognita.ticketdigital.mercadona.WriteItemsCsvCommand.class,
                 org.terra.incognita.ticketdigital.mercadona.WriteItemsXlsxCommand.class,
                 org.terra.incognita.ticketdigital.mercadona.gmail.Main.class
         })
public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
