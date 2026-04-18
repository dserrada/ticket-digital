package org.terra.incognita.ticketdigital.mercadona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terra.incognita.ticketdigital.mercadona.model.PurchasedItem;
import org.terra.incognita.ticketdigital.mercadona.model.ShopData;
import org.terra.incognita.ticketdigital.mercadona.model.TicketMercadona;
import org.terra.incognita.ticketdigital.mercadona.utils.FileUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Clase que maneja agregados de información sobre productos comprados
 */
public class ItemsAggregator {

    private static final Logger logger = LoggerFactory.getLogger(ItemsAggregator.class);


    public static List<PurchasedItem> extractFromPDF(Path basePath) throws IOException {
        List<Path> files = FileUtils.searchInDir(basePath);
        if ( files == null ) return List.of();
        List<PurchasedItem> items = files.stream()
                .map( file -> {
                    try {
                        return TicketMercadona.parse(file).items();
                    } catch (UnsupportedOperationException e) {
                        logger.error("Todavía no podemos procesar el archivo " + file);
                        return null;
                    } catch  ( Exception e ) {
                        logger.error("Error parsing " + file, e);
                        throw new RuntimeException("Error al procesar el archivo: " + file );
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(List::stream).toList();
        logger.debug("Extracted {} items from {} files", items.size(), files.size());
        return items;

    }

}
