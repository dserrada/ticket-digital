package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Información de la tienda donde se ha generado el ticket digital.
 *
 * @param shopName   Nombre de la tienda, de momento "MERCADONA"
 * @param cif        CIF de la tienda (o de Mercadona?),
 * @param address    Nombre de la calle donde se encuentra la tienda
 * @param postalCode Código postal como cadena
 * @param state    Nombre de la provincia donde se encuentra la tienda
 * @param phoneNumber     Número de teléfono asociado a la tienda (como cadena)
 */
public record ShopData(String shopName, String cif, String address, String postalCode, String state, String phoneNumber) {

    public static final int EXPECTED_LINES = 4;

    private static final Logger logger = LoggerFactory.getLogger(ShopData.class);

    /**
     * Parsea el trozo de información del ticket
     *
     * @param status    Estado del parseador con el iterador de líneas
     * @return          Un objeto que representa la información contenida en el chunk
     * @throws ParseException Si ocurre un error durante el análisis del chunk
     */
    public static ShopData parse(ParserStatusInfo status) throws ParseException {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < EXPECTED_LINES; i++) {
            if (status.iterator().hasNext()) {
                lines.add(status.iterator().next());
            } else {
                for (int j = 0; j < lines.size(); j++) status.iterator().previous();
                throw new ParseException("Expected 4 lines, got " + lines.size(), -1);
            }
        }

        logger.debug("Parsing lines {}", lines);

        // TODO: Mejor validarlas una a una para poder dar información de que linea falla
        boolean missingLine = lines.stream().anyMatch(line -> line == null || line.isBlank());
        if ( missingLine ) {
            throw new ParseException("Missing or blank lines in input", -1);
        }

        // Empieza el parseado real

        // Parse Empresa
        String line = lines.get(0);
        int indexCIF = line.indexOf("S.A.");
        if (indexCIF < 0) {
            throw new IllegalArgumentException("Expected 'S.A.' in empresa line: '" + line + "'");
        }
        String nombreEmpresa = line.substring(0, indexCIF + "S.A.".length()).trim();
        String cifEmpresa = line.substring(indexCIF + "S.A.".length()).trim();

        // Parse Direccion
        line = lines.get(1);
        String calleLinea = line.trim();

        line = lines.get(2);
        String codigoLocalidadLinea = line.trim();
        String[] parts = codigoLocalidadLinea.split("\\s+");
        if (parts.length < 2) {
            throw new ParseException("Expected at least postalCode and localidad in line: '" + codigoLocalidadLinea + "'",-1);
        }
        String codigoPostal = parts[0];
        String localidad = parts[1];

        // Parse Teléfono
        line = lines.get(3);
        String[] telefonoParts = line.trim().split(":");
        if (telefonoParts.length < 2) {
            throw new ParseException("Expected ':' in phoneNumber line: '" + line + "'",-1);
        }
        String telefono = telefonoParts[1].trim();
        return new ShopData(nombreEmpresa, cifEmpresa,  calleLinea, codigoPostal, localidad, telefono);
    }
}
