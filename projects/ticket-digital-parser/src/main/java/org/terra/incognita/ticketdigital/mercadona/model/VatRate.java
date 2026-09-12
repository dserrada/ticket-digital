package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un tramo de la tabla de desglose de IVA (VAT) del ticket.
 *
 * @param rate         Tipo de IVA aplicado (0, 2, 4, 5, 7.5, 10, 21, ...)
 * @param taxableBase  Base imponible en euros para este tramo
 * @param vatAmount    Cuota de IVA en euros para este tramo
 */
public record VatRate(BigDecimal rate, BigDecimal taxableBase, BigDecimal vatAmount) {

    private static final Logger logger = LoggerFactory.getLogger(VatRate.class);

    // El tramo original solo trae base y cuota; algunos tickets añaden una tercera
    // columna redundante con el total (base + cuota) del tramo, que se ignora.
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s*(?<rate>\\d{1,2}(?:,\\d{1,2})?)%\\s+(?<base>\\d*,\\d{2})\\s+(?<vatAmount>\\d*,\\d{2})"
                    + "(?:\\s+\\d*,\\d{2})?\\s*$");

    public VatRate {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal(100)) > 0) {
            throw new IllegalArgumentException("rate must be between 0 and 100");
        }
        taxableBase = taxableBase.setScale(2, RoundingMode.UNNECESSARY);
        vatAmount = vatAmount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Parsea una línea de tramo de IVA (p.ej. "10%    8,54    0,85").
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return El tramo parseado, o null si la línea no es un tramo (p.ej. es la fila TOTAL)
     */
    public static VatRate parse(ParserStatusInfo status) throws ParseException {
        if (!status.hasNext()) return null;
        String line = status.next();
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            logger.debug("Line [{}] no es del tipo {}", line, VatRate.class.getSimpleName());
            status.rollback(1);
            return null;
        }
        BigDecimal rate = new BigDecimal(matcher.group("rate").replace(',', '.'));
        BigDecimal base = PurchasedItem.parseUnitPrice(matcher.group("base"));
        BigDecimal vatAmount = PurchasedItem.parseUnitPrice(matcher.group("vatAmount"));
        logger.debug("Tramo IVA parseado: rate={}, base={}, vatAmount={}", rate, base, vatAmount);
        return new VatRate(rate, base, vatAmount);
    }
}
