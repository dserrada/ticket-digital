package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Representación de un ticket digital de una compra realizada en una tienda de Mercadona.
 *
 * Contiene la información fiscal, detalles de la tienda, fecha de compra, lista de artículos adquiridos,
 * importe pagado, información de la tarjeta bancaria y otros identificadores relacionados con el ticket.
 *
 * Esta clase es un registro inmutable que facilita la gestión de los datos del ticket generado.
 */
public record TicketMercadona(ShopData shopData, TicketHeader header, List<PurchasedItem> items,
                              BigDecimal pagadoEnEuros, String tarjetaBancaria, String nc, String aut, String aid, String arc) {

    private static final Logger logger = LoggerFactory.getLogger(TicketMercadona.class);

    /**
     * Calcula el precio pagado en este ticket
     *
     * @return Total en euros de los precios de los artículos.
     */
    public BigDecimal precioTotalEnEuros() {
        return items.stream()
                .map(PurchasedItem::precioTotal) // Obtenemos el precio de cada artículo.
                .reduce(BigDecimal.ZERO, BigDecimal::add)  // Sumamos todos los precios de los artículos comprados
                .setScale(2, RoundingMode.UNNECESSARY);
    }


    public static TicketMercadona parse(String ticketData) throws IOException, ParseException {
        Objects.requireNonNull(ticketData, "ticketData");

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(ticketData))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // Parseo parte a parte
        int nCurrentLine = 0;  // TODO: esto lo tendrían que mantener los parseadores (o en el arrayList)
        ShopData shopData = ShopData.parse(0, lines);
        nCurrentLine += ShopData.EXPECTED_LINES;

        TicketHeader header = TicketHeader.parse(nCurrentLine,lines);
        nCurrentLine += TicketHeader.EXPECTED_LINES;

        // Salto lineas en blanco hasta que llego a la cabecera de los items (TODO: esto debería estar embebido en alguno de los parseadores)
        while (nCurrentLine < lines.size() && lines.get(nCurrentLine).isBlank()) {
            nCurrentLine++;
        }
        // Linea de cabecera de los items
        String line = lines.get(nCurrentLine);
        if ( !line.trim().equals("Descripción                       P. Unit    Importe") ) {
            throw new ParseException("Invalid ticket format: expected 'Descripción                       P. Unit    Importe' header", nCurrentLine);
        }
        nCurrentLine++;

        List<PurchasedItem> items = new ArrayList<>();
        // Y ahora compruebo la lista de items
        boolean byUnit = false;
        boolean byWeight = false;
        while( !lines.get(nCurrentLine).trim().startsWith("TOTAL (€)")) {
            byUnit = ItemByUnit.isItemByUnit(nCurrentLine, lines);
            logger.debug("NLinea: {}, linea: {},  byUnit: {}", nCurrentLine,lines.get(nCurrentLine), byUnit);
            if (byUnit) {
                items.add(ItemByUnit.parse(nCurrentLine, lines));
                nCurrentLine++;  // Solo una vez que se alla añadido correctamente el objeto
            } else {
                // Probamos a ver si es byWeight
                byWeight = ItemByWeight.isItemByWeight(nCurrentLine, lines);
                logger.debug("NLinea: {}, linea: {},  byUnit: {}", nCurrentLine,lines.get(nCurrentLine), byWeight);
                items.add(ItemByWeight.parse(nCurrentLine, lines));
                nCurrentLine+=2;
            }
        }
        // Estamos en la linea del total
        logger.info("Total: {}", lines.get(nCurrentLine));


        return new TicketMercadona(shopData, header,items,
                null,null,null,null,null,null);
    }


}

