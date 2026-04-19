package org.terra.incognita.ticketdigital.mercadona;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "ticket-digital", 
         mixinStandardHelpOptions = true, 
         version = "1.0",
         description = "Interfaz de línea de comandos para procesar tickets digitales de Mercadona.")
public class Main implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "write-items-csv", 
             mixinStandardHelpOptions = true,
             description = "Genera un fichero csv con la información de todos los items comprados.")
    public Integer writeItemsCsv(
            @Option(names = {"--csv-file"}, description = "Nombre del fichero donde se escribe la información en csv.", required = true, paramLabel = "<fichero-csv>") File csvFile ,
            @Option(names = {"--data-dir"}, description = "Directorio donde están los ficheros pdf con los tickets a analizar.", required = true, paramLabel = "<directorio-datos>") File dataDir) {
        // De momento solo el interfaz de linea de comandos, no enganchar con la lógica
        System.out.println("Ejecutando operación write-items-csv...");
        System.out.println("Directorio de datos: " + dataDir);
        System.out.println("Fichero CSV: " + csvFile);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}