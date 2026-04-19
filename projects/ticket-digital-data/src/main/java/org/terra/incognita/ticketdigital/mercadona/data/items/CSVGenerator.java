package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CSVGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CSVGenerator.class);

    public static void writeCSVToFile(Path basePath, File outputFile) throws IOException {
        logger.debug("Iniciando la escritura del fichero csv, basePath: {}, outputFile: {}", basePath, outputFile.getAbsolutePath());
        List<Path> files = FileUtils.searchInDir(basePath);
        if ( files == null ) return;
        try (BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            // La cabecera
            bos.write(PurchasedItemRecord.headerCSV());
            bos.newLine();
            Stream<String> lines = files.stream()
                .map(file -> {
                    try {
                        return TicketMercadona.parse(file);
                    } catch (UnsupportedOperationException e) {
                        logger.error("Todavía no podemos procesar el archivo " + file);
                        return null;
                    } catch (Exception e) {
                        logger.error("Error parsing " + file, e);
                        throw new RuntimeException("Error al procesar el archivo: " + file);
                    }
                })
                .filter(Objects::nonNull)
                .map(PurchasedItemRecord::fromTicket)
                .flatMap(List::stream)
                .map(PurchasedItemRecord::toCSV);
            for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
                bos.write(it.next());
                bos.newLine();
            }
        }
        logger.debug("Finalizada la escritura del fichero csv");
    }
}
