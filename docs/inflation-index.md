# Índice de inflación de la cesta de la compra

Documento de diseño de los subcomandos `inflation-index` y `basket-composition` de
`ticket-digital-data`. Recoge el **por qué** de cada decisión metodológica, no solo el
qué — para que cualquier cambio futuro (o cualquier duda sobre "¿por qué sale este
número?") pueda contrastarse con el criterio que se siguió.

## Objetivo

A partir del histórico de tickets, calcular cuánto ha subido el precio de la compra
habitual año a año, usando los dos índices de precios clásicos:

- **Índice de Laspeyres**: compara, año a año, el coste de comprar siempre la **misma
  cesta** (cantidades fijas del año base) a los precios de cada año. Responde a "¿cuánto
  me costaría hoy lo que compraba en el año base?".
- **Índice de Paasche**: usa la cesta (cantidades) **de cada año**, no la del año base.
  Responde a "¿cuánto me costaría en el año base lo que compro hoy?", leído al revés.

Ambos se expresan en base 100 en el año base internamente, pero se **muestran como
variación porcentual** frente a ese año (p.ej. índice 103,45 → `+3,45%`), que es como se
expresa habitualmente la inflación.

La cesta de la compra **no la da el usuario**: la calcula el programa a partir del
propio histórico de tickets, y debe funcionar igual de bien si la serie de tickets
empieza en enero o a mitad de año (no se puede asumir un calendario "limpio").

## Decisiones metodológicas y su porqué

### 1. Año base: el primer año "completo" de la serie

**Problema**: si la serie de tickets empieza, por ejemplo, en julio de 2023, usar 2023
como año base sería engañoso — solo se conocen los precios de la segunda mitad del año
(sesgo estacional: verano/Navidad tienen cestas distintas al resto del año), y **ningún
otro año se podría comparar limpiamente contra un año base a medias**.

**Criterio adoptado**: un año es "completo" si tiene compras en al menos **10 de los 12
meses** distintos. El año base es el **primer año completo** de la serie (no
necesariamente el primer año con datos). Umbral confirmado con el usuario tras
plantear las alternativas (12/12, 10/12, 6/12); se eligió 10/12 porque tolera hasta 2
meses sin compras (viajes, olvidos puntuales) sin dejar de considerar el año
representativo de un año completo de precios.

Implementación: `YearBasketDataBuilder` cuenta, por año, los meses distintos con alguna
compra; `BasketSelector.selectBaseYear` toma el primero (ascendente) marcado como
completo. Si ningún año lo es, el comando falla con un mensaje explícito en vez de dar
un resultado silenciosamente poco fiable.

### 2. La cesta: año con más variedad de productos, filtrando compras puntuales

**Problema**: la cesta no puede ser "todos los productos comprados alguna vez" —
incluiría compras puntuales (un regalo, un producto probado una vez) que no son
representativas de la compra habitual y que además raramente tendrán precio en más de
un año, lo que arruinaría la comparación.

**Criterio adoptado**, en dos pasos:

1. **Año de referencia de la cesta**: entre los años completos, el que tiene mayor
   número de productos **distintos** comprados — es el que mejor retrata la variedad de
   la compra habitual (empate → el más antiguo).
2. **Filtro de frecuencia**: de esos productos, se descartan los comprados **menos de 3
   veces** en toda la serie histórica (no solo el caso obvio de una única vez). Umbral
   confirmado con el usuario, algo más exigente que el mínimo aceptable ("comprado una
   sola vez"), para asegurar que lo que entra en la cesta es una compra recurrente y no
   puntual.

El año de referencia de la cesta **puede no coincidir** con el año base cronológico (p.ej.
el año base es el primero completo, pero otro año posterior tiene más variedad de
productos). Esto es intencional y se documenta como consecuencia del punto siguiente.

### 3. Precio faltante de un producto: excluirlo ese año (matched-model)

**Problema**: un producto de la cesta puede no comprarse en un año concreto (dejó de
gustar, cambió de marca, etc.) o —si el año de la cesta es distinto del año base— puede
no tener ningún precio en el año base.

**Alternativas consideradas**: excluir el producto ese año (método "matched-model",
estándar en cálculo de índices de precios cuando falta una cotización) frente a
arrastrar el último precio conocido (imputación). Se eligió **excluir**, confirmado con
el usuario: es más simple, no introduce un precio inventado, y es el estándar habitual.

**Consecuencia documentada** (no es un error): si el año de la cesta y el año base son
distintos y un producto de la cesta no se compró en el año base, ese producto queda
excluido de **todos** los años del índice (nunca hay precio/cantidad base con los que
compararlo). El comando `basket-composition` marca estos productos con un `*` y peso
0% — ver más abajo.

### 4. Años mostrados: todos, marcando los incompletos

Se muestran **todos** los años con datos (no solo los completos), marcando los
incompletos como tales (`incompleto` en la tabla) en vez de ocultarlos. Preferido a
excluirlos porque el usuario ve toda la serie y entiende por qué un año concreto puede
no ser fiable (típicamente el primer año, si la serie empieza a mitad de año, y el
último, si es el año en curso).

### 5. Clave de producto: nombre + forma de compra (unidad o peso)

Un mismo nombre de producto comprado a veces por unidad y a veces por peso (`ProductKey`
con `ProductType.UNIDAD` / `PESO`) cuenta como **dos entradas distintas** de la cesta,
porque su precio y cantidad no son comparables entre sí (€/ud frente a €/kg). Edge case
documentado, no resuelto de otra forma (no se intenta "fundir" ambas formas en una).

### 6. Precio de un producto en un año: media ponderada por gasto

`precio = gasto total del producto ese año / cantidad total comprada ese año` (no la
media simple de los precios de cada línea de ticket), para que una compra grande no
pese lo mismo que una pequeña.

## Fórmulas

Para cada año *t* frente al año base *0*, sobre los productos de la cesta con precio
conocido en ambos años (`matchedProductCount`):

```
Laspeyres_t = 100 · Σ(p_t · q_0) / Σ(p_0 · q_0)
Paasche_t   = 100 · Σ(p_t · q_t) / Σ(p_0 · q_t)
```

Redondeo a 2 decimales, `HALF_UP`. El año base da siempre 100,00 (→ se muestra como
`0,00%`) en ambos índices.

## Peso de cada producto en la cesta (`basket-composition`)

Además de los índices, se puede consultar la composición de la cesta: para cada
producto, su **peso relativo** es su gasto en el año base dividido entre el gasto total
de la cesta en el año base, en tanto por ciento (`Σ pesos = 100%`). Es el mismo concepto
de "peso" que usa cualquier índice de precios (p.ej. el IPC) para explicar cuánto influye
cada partida en el resultado global. Los productos sin datos en el año base (ver punto 3)
se listan con peso 0% y un `*` explicando por qué.

## Clases (paquete `data/inflation`, sin I/O)

| Clase | Rol |
|---|---|
| `ProductKey` / `ProductType` | Identidad de un producto de la cesta (id normalizado + unidad/peso). |
| `ProductYearStats` | Cantidad y gasto total de un producto en un año; `averagePrice()`. |
| `YearBasketData` / `YearBasketDataBuilder` | Agregación por año: meses con compra, flag "completo", stats por producto. |
| `BasketSelector` | Año base, año de referencia de la cesta, filtro de frecuencia. |
| `InflationIndexCalculator` | Laspeyres/Paasche por año (matched-model). |
| `InflationAnalyzer` | Fachada: `analyze()` (índices) y `analyzeBasketComposition()` (pesos), comparten la selección de año base/cesta (`Selection`). |
| `InflationReportFormatter` / `BasketCompositionFormatter` | Tablas de texto para consola. |

Comandos CLI (`ticket-digital-data`, registrados también en `ticket-digital-launcher`):
`InflationIndexCommand` (`inflation-index`) y `BasketCompositionCommand`
(`basket-composition`). Ambos reutilizan `TicketRecordsReader.readAll()` (extraído de
`CSVGenerator`/`XlsxDatosWriter` para no triplicar la lectura de tickets).

Umbrales configurables como constantes en `InflationAnalyzer`
(`DEFAULT_MIN_COMPLETE_MONTHS = 10`, `DEFAULT_MIN_TOTAL_PURCHASE_COUNT = 3`) — no
cambiar sin volver a confirmar el criterio, son decisiones tomadas con el usuario, no
valores arbitrarios de implementación.

## Limitaciones conocidas (aceptadas, no bugs)

- Un producto de la cesta sin precio en el año base queda fuera de **todos** los años
  del índice (§3).
- Productos "raros" que se cuelan en la cesta por casualidad (p.ej. una donación
  puntual repetida ≥3 veces) no se filtran de forma semántica, solo por frecuencia —
  no hay lista de exclusión de conceptos no-producto.
- Solo se imprime por consola; no hay export a fichero (CSV/XLSX) de estos dos comandos
  todavía.
