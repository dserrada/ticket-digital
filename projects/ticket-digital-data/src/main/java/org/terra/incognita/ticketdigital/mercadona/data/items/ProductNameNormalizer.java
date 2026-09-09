package org.terra.incognita.ticketdigital.mercadona.data.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Corrige nombres de producto que el ticket imprime con variantes (erratas de
 * tipografía, acentos, puntuación) según la tabla del recurso {@value #RESOURCE_PATH}.
 *
 * El recurso se lee una única vez, en un mapa estático, la primera vez que se usa esta clase.
 */
public final class ProductNameNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(ProductNameNormalizer.class);

    private static final String RESOURCE_PATH = "/nombres-normalizacion.csv";

    private static final Map<String, String> NORMALIZATIONS = load();

    private ProductNameNormalizer() {
    }

    /**
     * Devuelve el nombre normalizado si {@code nombre} tiene una corrección conocida,
     * o el propio {@code nombre} sin modificar en caso contrario.
     */
    public static String normalize(String nombre) {
        return NORMALIZATIONS.getOrDefault(nombre, nombre);
    }

    private static Map<String, String> load() {
        Map<String, String> normalizations = new HashMap<>();
        try (InputStream in = ProductNameNormalizer.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("No se encuentra el recurso " + RESOURCE_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                reader.readLine(); // cabecera: nombre_original;nombre_normalizado
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(";", 2);
                    if (parts.length != 2) {
                        logger.warn("Línea inválida en {}: [{}]", RESOURCE_PATH, line);
                        continue;
                    }
                    normalizations.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error cargando " + RESOURCE_PATH, e);
        }
        logger.info("Cargadas {} normalizaciones de nombres de producto desde {}", normalizations.size(), RESOURCE_PATH);
        return Collections.unmodifiableMap(normalizations);
    }
}
