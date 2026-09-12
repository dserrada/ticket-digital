package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;

/**
 * Da formato de tabla de texto, para consola, al resultado de {@link InflationAnalyzer}.
 */
public final class InflationReportFormatter {

    private static final String ROW_FORMAT = "%-6s %-11s %10s %10s%n";

    private InflationReportFormatter() {
    }

    public static String format(InflationReport report, boolean verbose) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(ROW_FORMAT, "Año", "Estado", "Laspeyres", "Paasche"));
        for (YearInflationIndex index : report.yearIndices()) {
            sb.append(String.format(ROW_FORMAT,
                    index.year(),
                    estado(index),
                    formatAsPercent(index.laspeyres()),
                    formatAsPercent(index.paasche())));
        }
        sb.append(System.lineSeparator());
        sb.append("Año base: ").append(report.baseYear())
                .append(" | Cesta calculada sobre el año: ").append(report.basketYear())
                .append(" (").append(report.basket().size()).append(" productos)")
                .append(System.lineSeparator());

        if (verbose) {
            sb.append("Productos de la cesta:").append(System.lineSeparator());
            for (ProductKey key : report.basket()) {
                sb.append("  - ").append(key.id()).append(" (").append(key.type()).append(")")
                        .append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private static String estado(YearInflationIndex index) {
        if (index.baseYear()) return "BASE";
        if (!index.complete()) return "incompleto";
        return "";
    }

    /**
     * El índice interno está en base 100 (100 = precio del año base); se muestra como el
     * tanto por ciento de variación frente al año base, que es como se expresa
     * habitualmente la inflación (p.ej. índice 103,45 → "+3,45%").
     */
    private static String formatAsPercent(BigDecimal index) {
        if (index == null) return "n/d";
        BigDecimal change = index.subtract(BigDecimal.valueOf(100));
        String sign = change.signum() > 0 ? "+" : "";
        return sign + change.toPlainString().replace('.', ',') + "%";
    }
}
