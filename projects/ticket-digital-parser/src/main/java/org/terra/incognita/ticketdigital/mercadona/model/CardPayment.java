package org.terra.incognita.ticketdigital.mercadona.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Información del pago con tarjeta al final del ticket: tarjeta enmascarada,
 * N.C./AUT, AID/ARC, y el importe verificado junto con la marca de la tarjeta.
 *
 * @param lastFourDigits Últimos 4 dígitos de la tarjeta enmascarada
 * @param nc                    Código N.C.
 * @param aut                   Código de autorización (AUT)
 * @param aid                   Application Identifier
 * @param arc                   Authorization Response Code
 * @param brand                 Marca de la tarjeta (p.ej. "MASTERCARD", "Visa Debit")
 * @param amount               Importe verificado en euros
 */
public record CardPayment(String lastFourDigits, String nc, String aut, String aid, String arc,
                           String brand, BigDecimal amount) {

    private static final Logger logger = LoggerFactory.getLogger(CardPayment.class);

    public static final int MAX_FILLER_LINES = 10;

    private static final Pattern MASKED_CARD_PATTERN = Pattern.compile(
            "^\\s*TARJ\\.?\\s*BANCARIA:\\s*\\*{4}\\s+\\*{4}\\s+\\*{4}\\s+(?<lastFourDigits>\\d{4})\\s*$");

    private static final Pattern NC_AUT_PATTERN = Pattern.compile(
            "^\\s*N\\.C:\\s*(?<nc>\\d+)\\s+AUT:\\s*(?<aut>\\S+)\\s*$");

    private static final Pattern AID_ARC_PATTERN = Pattern.compile(
            "^\\s*AID:\\s*(?<aid>\\S+)\\s+ARC:\\s*(?<arc>\\S+)\\s*$");

    // Formato alternativo visto en tickets más recientes: sin N.C, con AUT/ARC/AID en
    // una sola línea y el valor de AID en la línea siguiente (en vez de en la misma línea)
    private static final Pattern AUT_ARC_AID_PATTERN = Pattern.compile(
            "^\\s*AUT:\\s*(?<aut>\\S+)\\s+ARC:\\s*(?<arc>\\S+)\\s+AID:\\s*$");

    // Otro formato alternativo: sin N.C, con AUT/ARC/AID los tres en la misma línea
    // (en vez de AID en la línea siguiente).
    private static final Pattern AUT_ARC_AID_INLINE_PATTERN = Pattern.compile(
            "^\\s*AUT:\\s*(?<aut>\\S+)\\s+ARC:\\s*(?<arc>\\S+)\\s+AID:\\s*(?<aid>\\S+)\\s*$");

    private static final Pattern BARE_VALUE_PATTERN = Pattern.compile("^\\s*(?<value>\\S+)\\s*$");

    private static final Pattern DEVICE_VERIFIED_PATTERN = Pattern.compile(
            "^\\s*Verificado por dispositivo\\s*$");

    private static final Pattern BRAND_ONLY_PATTERN = Pattern.compile("^\\s*[A-Z]+\\s*$");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "^\\s*Importe:\\s*(?<amount>\\d*,\\d{2})\\s*Â?€\\s+(?<brand>.+?)\\s*$");

    public CardPayment {
        Objects.requireNonNull(lastFourDigits, "lastFourDigits must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Parsea el bloque de pago con tarjeta: línea de tarjeta enmascarada, línea
     * N.C./AUT, línea AID/ARC, y - de forma tolerante - líneas de relleno (en
     * blanco, "Verificado por dispositivo" o una marca suelta) hasta encontrar
     * la línea "Importe: X,XX € &lt;marca&gt;".
     *
     * Las tres primeras líneas siguen el contrato habitual "coincide o rollback+null"
     * de esta clase de parsers. Una vez esas tres líneas coinciden, el bloque se
     * considera obligatorio y cualquier fallo posterior lanza ParseException.
     *
     * @param status Estado del parseador con el iterador de líneas
     * @return El pago con tarjeta parseado, o null si las 3 primeras líneas no coinciden
     */
    public static CardPayment parse(ParserStatusInfo status) throws ParseException {
        if (!status.hasNext()) return null;
        String maskedLine = status.next();
        Matcher maskedMatcher = MASKED_CARD_PATTERN.matcher(maskedLine);
        if (!maskedMatcher.matches()) {
            status.rollback(1);
            return null;
        }
        String lastFourDigits = maskedMatcher.group("lastFourDigits");

        if (!status.hasNext()) {
            status.rollback(1);
            return null;
        }
        String secondLine = status.next();
        String nc;
        String aut;
        String aid;
        String arc;
        Matcher ncAutMatcher = NC_AUT_PATTERN.matcher(secondLine);
        if (ncAutMatcher.matches()) {
            // Formato clásico: "N.C: ... AUT: ..." seguido de "AID: ... ARC: ..."
            nc = ncAutMatcher.group("nc");
            aut = ncAutMatcher.group("aut");

            if (!status.hasNext()) {
                status.rollback(2);
                return null;
            }
            String thirdLine = status.next();
            Matcher aidArcMatcher = AID_ARC_PATTERN.matcher(thirdLine);
            if (!aidArcMatcher.matches()) {
                status.rollback(3);
                return null;
            }
            aid = aidArcMatcher.group("aid");
            arc = aidArcMatcher.group("arc");
        } else {
            // Formato alternativo: sin N.C, "AUT: ... ARC: ... AID: " seguido del valor de AID solo
            Matcher autArcAidMatcher = AUT_ARC_AID_PATTERN.matcher(secondLine);
            if (autArcAidMatcher.matches()) {
                nc = null;
                aut = autArcAidMatcher.group("aut");
                arc = autArcAidMatcher.group("arc");

                if (!status.hasNext()) {
                    status.rollback(2);
                    return null;
                }
                String thirdLine = status.next();
                Matcher bareValueMatcher = BARE_VALUE_PATTERN.matcher(thirdLine);
                if (!bareValueMatcher.matches()) {
                    status.rollback(3);
                    return null;
                }
                aid = bareValueMatcher.group("value");
            } else {
                // Formato más reciente: sin N.C, "AUT: ... ARC: ... AID: ..." los tres en
                // una sola línea.
                Matcher autArcAidInlineMatcher = AUT_ARC_AID_INLINE_PATTERN.matcher(secondLine);
                if (!autArcAidInlineMatcher.matches()) {
                    status.rollback(2);
                    return null;
                }
                nc = null;
                aut = autArcAidInlineMatcher.group("aut");
                arc = autArcAidInlineMatcher.group("arc");
                aid = autArcAidInlineMatcher.group("aid");
            }
        }

        // A partir de aquí el bloque es inequívocamente un CardPayment: cualquier
        // problema se traduce en ParseException en lugar de null/rollback.
        String brand = null;
        BigDecimal amount = null;
        int fillerLinesRead = 0;
        while (status.hasNext()) {
            String line = status.next();
            if (amount == null) {
                Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
                if (amountMatcher.matches()) {
                    amount = PurchasedItem.parseUnitPrice(amountMatcher.group("amount"));
                    brand = amountMatcher.group("brand").trim();
                    continue;
                }
            }
            if (line.isBlank() || DEVICE_VERIFIED_PATTERN.matcher(line).matches() || BRAND_ONLY_PATTERN.matcher(line).matches()) {
                fillerLinesRead++;
                if (fillerLinesRead > MAX_FILLER_LINES) {
                    throw new ParseException("Too many filler lines while looking for Importe line", status.previousIndex());
                }
                continue;
            }
            if (amount != null) {
                // Ya encontramos la línea Importe: cualquier línea posterior que no sea de
                // relleno se deja para que la procese quien llame a este parser (p.ej. el
                // disclaimer final).
                status.rollback(1);
                break;
            }
            throw new ParseException("Expected filler line or Importe line, found: " + line, status.previousIndex());
        }
        if (amount == null) {
            throw new ParseException("Expected Importe line, got EOF", -1);
        }

        logger.debug("CardPayment parseado: lastFourDigits={}, nc={}, aut={}, aid={}, arc={}, brand={}, amount={}",
                lastFourDigits, nc, aut, aid, arc, brand, amount);
        return new CardPayment(lastFourDigits, nc, aut, aid, arc, brand, amount);
    }
}
