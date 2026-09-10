package org.terra.incognita.ticketdigital.mercadona.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadona.class);

    public static List<Path> searchInDir(Path dataDir) throws IOException {
        // Skip test if directory doesn't exist
        if (!Files.exists(dataDir)) {
            logger.warn("Data directory does not exist: {}", dataDir.toAbsolutePath());
            return null;
        }
        logger.info("Searching for PDF files in {}", dataDir.toAbsolutePath());

        List<Path> pdfFiles = new ArrayList<>();

        // Tree walk to collect all PDF files
        Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                logger.debug("Found file: {}", file);
                if (file.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    pdfFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        logger.info("Found {} PDF files in {}", pdfFiles.size(), dataDir.toAbsolutePath());
        return pdfFiles;
    }
}
