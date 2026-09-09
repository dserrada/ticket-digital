package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Desglose de IVA (VAT) del ticket: cabecera + N tramos + fila TOTAL.
 *
 * @param rates            Lista de tramos de IVA (uno por cada tipo aplicado)
 * @param totalTaxableBase Suma de bases imponibles, tal y como aparece en la fila TOTAL
 * @param totalVatAmount   Suma de cuotas, tal y como aparece en la fila TOTAL
 */
public record VatBreakdown(List<VatRate> rates, BigDecimal totalTaxableBase, BigDecimal totalVatAmount) {

    private static final Logger logger = LoggerFactory.getLogger(VatBreakdown.class);

    // Algunos tickets abrevian "BASE IMPONIBLE" a "BASE IMP." y añaden una tercera
    // columna redundante "TOTAL (€)" (base + cuota); ambas variantes se aceptan.
    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^\\s*IVA\\s+BASE IMP(?:ONIBLE|\\.) \\(€\\)\\s+CUOTA \\(€\\)(?:\\s+TOTAL \\(€\\))?\\s*$");

    private static final Pattern TOTAL_LINE_PATTERN = Pattern.compile(
            "^\\s*TOTAL\\s+(?<base>\\d*,\\d{2})\\s+(?<vatAmount>\\d*,\\d{2})(?:\\s+\\d*,\\d{2})?\\s*$");

    public VatBreakdown {
        rates = List.copyOf(rates);
        totalTaxableBase = totalTaxableBase.setScale(2, RoundingMode.UNNECESSARY);
        totalVatAmount = totalVatAmount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Parsea la cabecera "IVA  BASE IMPONIBLE (€)  CUOTA (€)", seguida de 1 o más
     * tramos, y termina en la fila "TOTAL &lt;base&gt; &lt;vatAmount&gt;".
     *
     * Valida que la suma de bases/cuotas de los tramos coincide con la fila TOTAL.
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return El desglose de IVA parseado
     * @throws ParseException Si el formato no es el esperado
     */
    public static VatBreakdown parse(ParserStatusInfo status) throws ParseException {
        if (!status.hasNext()) {
            throw new ParseException("Expected IVA breakdown header, got EOF", -1);
        }
        String headerLine = status.next();
        if (!HEADER_PATTERN.matcher(headerLine).matches()) {
            throw new ParseException("Expected IVA header line, found: " + headerLine, status.previousIndex());
        }

        List<VatRate> rates = new ArrayList<>();
        BigDecimal totalBase = null;
        BigDecimal totalVatAmount = null;
        while (status.hasNext()) {
            String line = status.next();
            Matcher totalMatcher = TOTAL_LINE_PATTERN.matcher(line);
            if (totalMatcher.matches()) {
                totalBase = PurchasedItem.parseUnitPrice(totalMatcher.group("base"));
                totalVatAmount = PurchasedItem.parseUnitPrice(totalMatcher.group("vatAmount"));
                break;
            }
            status.rollback(1);
            VatRate rate = VatRate.parse(status);
            if (rate == null) {
                String errorLine = status.next();
                throw new ParseException("Expected IVA rate row or TOTAL row, found: " + errorLine, status.previousIndex());
            }
            rates.add(rate);
        }
        if (totalBase == null) {
            throw new ParseException("Expected IVA TOTAL row, got EOF", -1);
        }

        BigDecimal sumBase = rates.stream().map(VatRate::taxableBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumVatAmount = rates.stream().map(VatRate::vatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumBase.compareTo(totalBase) != 0 || sumVatAmount.compareTo(totalVatAmount) != 0) {
            throw new ParseException("La suma de los tramos de IVA no coincide con la fila TOTAL", -1);
        }

        logger.debug("IVA breakdown parseado: rates={}, totalBase={}, totalVatAmount={}", rates, totalBase, totalVatAmount);
        return new VatBreakdown(rates, totalBase, totalVatAmount);
    }
}
