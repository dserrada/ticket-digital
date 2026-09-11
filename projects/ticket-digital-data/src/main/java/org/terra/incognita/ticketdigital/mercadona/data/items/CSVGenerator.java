package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class CSVGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CSVGenerator.class);

    public static void writeCSVToFile(Path basePath, File outputFile) throws IOException {
        logger.debug("Iniciando la escritura del fichero csv, basePath: {}, outputFile: {}", basePath, outputFile.getAbsolutePath());
        List<PurchasedItemRecord> records = TicketRecordsReader.readAll(basePath);
        if (records == null) {
            throw new IOException("El directorio de datos no existe: " + basePath.toAbsolutePath());
        }
        try (BufferedWriter bos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            // La cabecera
            bos.write(PurchasedItemRecord.headerCSV());
            bos.newLine();
            for (PurchasedItemRecord record : records) {
                bos.write(record.toCSV());
                bos.newLine();
            }
        }
        logger.debug("Finalizada la escritura del fichero csv");
    }
}
