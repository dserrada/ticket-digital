package org.terra.incognita.ticketdigital.mercadona.data.inflation;

import java.math.BigDecimal;

/**
 * Da formato de tabla de texto, para consola, a la composición de la cesta calculada
 * por {@link InflationAnalyzer#analyzeBasketComposition}.
 */
public final class BasketCompositionFormatter {

    private static final String ROW_FORMAT = "%-30s %-7s %12s %9s%n";

    private BasketCompositionFormatter() {
    }

    public static String format(BasketComposition composition) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cesta de la compra (año base ").append(composition.baseYear())
                .append(", cesta calculada sobre el año ").append(composition.basketYear())
                .append(", ").append(composition.weights().size()).append(" productos)")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        sb.append(String.format(ROW_FORMAT, "Producto", "Tipo", "Gasto año base", "Peso"));
        for (ProductWeight weight : composition.weights()) {
            sb.append(String.format(ROW_FORMAT,
                    weight.key().id(),
                    weight.key().type(),
                    formatAmount(weight.baseYearSpend()) + " €",
                    formatPercent(weight.weightPercent()) + (weight.hasBaseYearData() ? "" : "*")));
        }
        sb.append(System.lineSeparator());
        sb.append("Gasto total de la cesta en el año base: ").append(formatAmount(composition.totalBaseYearSpend())).append(" €")
                .append(System.lineSeparator());

        boolean anyMissing = composition.weights().stream().anyMatch(w -> !w.hasBaseYearData());
        if (anyMissing) {
            sb.append("* sin datos en el año base (el producto entró en la cesta por el año usado para calcularla, ")
                    .append("que es distinto del año base); peso 0% y excluido de los índices de inflación.")
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String formatAmount(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String formatPercent(BigDecimal value) {
        return value.toPlainString().replace('.', ',') + "%";
    }
}
