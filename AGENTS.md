# ticket-digital — Agent Guide

## Propósito del proyecto

Aplicación Java modular que extrae y analiza datos de **tickets digitales de Mercadona** (PDF o TXT) y los exporta a CSV. Es una herramienta de uso personal para análisis local de histórico de compras.

---

## Estructura del proyecto

```
ticket-digital/
├── projects/
│   ├── ticket-digital-parser/   # Librería: modelos de dominio y parsers
│   └── ticket-digital-data/     # Aplicación: CLI, generación de CSV
└── docs/tasks.md                # Backlog de tareas pendientes
```

Build system: **Gradle 8.x**, Java **24**, módulos multi-project.

---

## Módulos

### `ticket-digital-parser` (java-library)

Modelos de dominio y lógica de parsing.

**Dependencias:** `pdfbox:3.0.3`, `slf4j-api`

**Paquete raíz:** `org.terra.incognita.ticketdigital.mercadona.model`

**Modelos principales:**

| Clase | Descripción |
|---|---|
| `PurchasedItem` | Sealed interface base para ítems comprados. Define constantes regex, `SPANISH_LOCALE`, helpers de parsing. |
| `OneItemByUnit` | 1 unidad a precio fijo. |
| `NItemsByUnit` | N unidades × precio unitario. |
| `ItemByWeight` | Producto vendido por kg (peso × precio/kg). |
| `FreshItemByWeight` | Producto fresco por peso (pescadería), agrupado bajo una cabecera de categoría (p.ej. "PESCADO"). |
| `ShopData` | Datos del establecimiento (`shopName`, `cif`, `address`, `postalCode`, `state`, `phoneNumber`). |
| `TicketHeader` | Cabecera del ticket (`purchaseDate`, `operationCode`, `simplifiedInvoiceNumber`). |
| `Parking` | Horario de parking (`start`/`end`). |
| `VatRate` | Un tramo del desglose de IVA (`rate`, `taxableBase`, `vatAmount`). |
| `VatBreakdown` | Desglose de IVA completo (`rates`, `totalTaxableBase`, `totalVatAmount`), valida que la suma de tramos cuadre con la fila TOTAL. |
| `CardPayment` | Datos del pago con tarjeta al final del ticket (`lastFourDigits`, `nc`, `aut`, `aid`, `arc`, `brand`, `amount`). Tolera dos formatos distintos de N.C/AUT/AID/ARC vistos en tickets reales. |
| `TicketMercadona` | Ticket completo. Punto de entrada del parsing: `TicketMercadona.parse(Path)`. |
| `ParserStatusInfo` | Iterador de líneas con rollback para el parser chain. |

Todos los modelos son **records inmutables**. Los parsers son métodos estáticos en cada record que devuelven `null` si la línea no coincide (patrón parser chain); las excepciones (`ParseException`) solo se lanzan una vez el bloque se considera inequívoco (p.ej. tras casar la línea de tarjeta enmascarada en `CardPayment`).

Nomenclatura: los campos de los records están en inglés, salvo los conceptos que solo tienen sentido en español o que son códigos literales impresos en el ticket sin traducción clara (`cif`, `nc`, `aut`, `aid`, `arc`).

---

### `ticket-digital-data` (application)

CLI y generación de CSV.

**Dependencias:** `picocli:4.7.6`, `:ticket-digital-parser`

**Paquete raíz:** `org.terra.incognita.ticketdigital.mercadona`

| Clase | Descripción |
|---|---|
| `Main` | CLI picocli. Subcomandos `write-items-csv`, `write-items-xlsx`, `inflation-index`, `basket-composition`. |
| `CSVGenerator` | Orquesta el proceso: `TicketRecordsReader.readAll()` → escribe CSV. |
| `XlsxDatosWriter` | Igual que `CSVGenerator` pero generando el XLSX a partir de `Mercadona-base.xlsx`. |
| `TicketRecordsReader` | Punto único de lectura: busca PDFs (`FileUtils`) → `TicketMercadona.parse()` → `PurchasedItemRecord.fromTicket()`. Lo usan `CSVGenerator`, `XlsxDatosWriter` e `InflationIndexCommand`. |
| `PurchasedItemRecord` | Representación tabular de una compra. Método `fromTicket()` para convertir, `toCSV()` para serializar. |
| `ProductNameNormalizer` | Corrige erratas de nombre de producto (acentos, puntuación) según `nombres-normalizacion.csv`. Se aplica dentro de `PurchasedItemRecord.fromTicket()`; el CSV se carga una única vez en un mapa estático. |
| `FileUtils` | Búsqueda recursiva de ficheros PDF en un directorio. |
| `InflationIndexCommand` + paquete `data.inflation` | Subcomando `inflation-index`: calcula y muestra por consola los índices de Laspeyres y Paasche de la cesta de la compra. Ver detalle abajo y `docs/inflation-index.md`. |
| `BasketCompositionCommand` | Subcomando `basket-composition`: muestra la misma cesta que `inflation-index`, listando cada producto con su peso relativo (%) sobre el gasto del año base. |

---

## Flujo de datos

```
CLI (Main) → CSVGenerator → FileUtils.searchInDir()
                                    ↓
                         TicketMercadona.parse(Path)
                         [ShopData → TicketHeader → Items (+ Parking) →
                          total → VatBreakdown → CardPayment → disclaimer]
                         (ver docs/parser-phases.mmd)
                                    ↓
                         PurchasedItemRecord.fromTicket()
                         [normaliza el id vía ProductNameNormalizer.normalize()]
                                    ↓
                         PurchasedItemRecord.toCSV()
                                    ↓
                         BufferedWriter (UTF-8, separador ";")
```

El CSV de salida usa **separador `;`** y **decimales con coma** (locale español).

Los nombres de producto ya salen **normalizados** (`ProductNameNormalizer`, tabla en
`nombres-normalizacion.csv`): corrige erratas de tipografía/acentos/puntuación que
Mercadona imprime de forma inconsistente para el mismo producto (p.ej.
`ACEITE OLIVA 0'4` / `ACEITE OLIVA 0`4` → `ACEITE OLIVA 0.4`). Cualquier comprobación
posterior sobre nombres de producto (agrupaciones, totales por producto, etc.) puede
asumir que esta corrección **ya se ha aplicado** y no necesita repetirla ni tenerla en
cuenta como fuente de inconsistencias.

---

## Índice de inflación (`inflation-index`, `basket-composition`)

Calcula, a partir de los `PurchasedItemRecord` (mismo pipeline que CSV/XLSX), un índice
de inflación anual de la cesta de la compra con **Laspeyres** (cesta y precios del año
base) y **Paasche** (cesta y precios de cada año), impreso por consola (como % de
variación frente al año base). Diseño completo, con el porqué de cada criterio
metodológico (año base, umbral de la cesta, tratamiento de precios faltantes, etc.), en
**`docs/inflation-index.md`** — léelo antes de tocar cualquier umbral o fórmula. Clases
en `data/inflation/` (sin I/O):

| Clase | Rol |
|---|---|
| `ProductKey` | `(id normalizado, ProductType)` — UNIDAD o PESO; mismo nombre comprado de las dos formas cuenta como dos productos distintos. |
| `YearBasketDataBuilder` | Agrupa las compras por año y por `ProductKey`: cantidad/gasto total (`ProductYearStats`) y meses distintos con compra. |
| `BasketSelector` | Año base = primer año **completo** (≥`DEFAULT_MIN_COMPLETE_MONTHS` meses distintos con compra, evita tomar como base un año a medias si la serie empieza a mitad de año). Cesta = productos del año completo con más variedad, filtrando los comprados menos de `DEFAULT_MIN_TOTAL_PURCHASE_COUNT` veces en toda la serie. |
| `InflationIndexCalculator` | Laspeyres/Paasche por año, método *matched-model*: un producto de la cesta sin precio en el año base o en el año en curso se excluye de ambas sumas ese año (no se imputa precio). |
| `InflationAnalyzer` | Fachada; `analyze(records)` (índices) y `analyzeBasketComposition(records)` (pesos), ambos con los umbrales por defecto y comparten la selección de año base/cesta. |
| `InflationReportFormatter` | Tabla de `inflation-index`; año base marcado `BASE`, años con menos meses de los exigidos marcados `incompleto` (se muestran igualmente, no se ocultan); índices mostrados como % de variación, no en base 100. |
| `BasketCompositionFormatter` | Tabla de `basket-composition`; cada producto con su gasto en el año base y su peso (%) sobre el total de la cesta, ordenados de mayor a menor peso. |

Umbrales por defecto (`InflationAnalyzer`): 10 de 12 meses para año "completo", mínimo 3
compras históricas para entrar en la cesta — decididos con el usuario, no cambiar sin
confirmarlo.

---

## Comandos de uso

```bash
# Compilar y ejecutar tests
./gradlew test

# Compilar todo
./gradlew build

# Ejecutar CLI
./gradlew :ticket-digital-data:run --args="write-items-csv --data-dir /ruta/pdfs --csv-file salida.csv"
./gradlew :ticket-digital-data:run --args="inflation-index --data-dir /ruta/pdfs"
./gradlew :ticket-digital-data:run --args="basket-composition --data-dir /ruta/pdfs"
```

---

## Estado de implementación

| Feature | Estado |
|---|---|
| Parsing OneItemByUnit, NItemsByUnit, ItemByWeight | Completo |
| Parsing ShopData, TicketHeader, Parking | Completo |
| Parsing FreshItemByWeight (productos frescos) | Completo |
| Parsing total, importe pagado con tarjeta, desglose de IVA | Completo |
| Parsing datos de pago con tarjeta (tarjeta enmascarada, N.C/AUT/AID/ARC, marca, importe) | Completo |
| Extracción de PDF (PDFBox) y TXT | Completo |
| CLI picocli | Completo |
| Generación CSV/XLSX | Completo |
| Índice de inflación (Laspeyres/Paasche, `inflation-index`) | Completo (salida solo por consola, sin fichero) |
| Composición de la cesta con pesos relativos (`basket-composition`) | Completo (salida solo por consola, sin fichero) |

---

## Convenciones del código

- Todos los modelos de dominio son **Java records** (inmutables).
- Los parsers devuelven **`null`** si no coinciden (nunca lanzan excepción en ese caso).
- Las constantes regex están en `PurchasedItem` como `static final Pattern`.
- Se usan **named groups** en regex: `(?<id>...)`, `(?<price>...)`.
- Logging vía SLF4J; Logback solo en test scope.
- Los tests de integración asumen PDFs en rutas externas al repositorio.

---

## Áreas de trabajo pendiente

Ver `docs/tasks.md` para el backlog general (parcialmente desactualizado/genérico). Gaps conocidos y concretos en el código actual:

1. **`FreshItemByWeight.FRESH_TYPE_PATTERN`** — solo reconoce la categoría "PESCADO" como cabecera de sección fresca; otras categorías (carnicería, frutería, etc., si existen en algún ticket) no se detectarían.
2. **`TicketMercadonaTest.pruebaFichero()`** — referencia `src/test/resources/20230101090000 Ticket Digital Mercadona.txt`, que no existe en el repo; el test falla con `NoSuchFileException` (fallo preexistente, no introducido por el parseo de total/IVA/tarjeta).
3. **`OneItemByUnitTest.testParseValidSingleItem()`** — falla en una aserción (fallo preexistente, no relacionado con el parseo de total/IVA/tarjeta ni con el renombrado de campos).
4. **`CardPayment`** — el bloque final del ticket (relleno entre AID/ARC y la línea `Importe:`) se parsea de forma tolerante en base a los formatos vistos hasta ahora en ~150 tickets reales; nuevas plantillas de recibo de Mercadona podrían requerir ajustes.
