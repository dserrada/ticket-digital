# ticket-digital-data

Genera un CSV o un XLSX con el detalle de todo lo comprado, a partir de los PDF de
tickets de Mercadona que ya tengas en un directorio local. Es parte del proyecto
[`ticket-digital`](../../README.md).

Este módulo **no descarga nada de Gmail**: solo lee PDF de un directorio. Ese
directorio puede haberse llenado de dos formas:

- con [`ticket-digital-tickets-downloader`](../ticket-digital-tickets-downloader/README.md)
  (o el CLI "todo en uno" [`ticket-digital-launcher`](../ticket-digital-launcher/README.md)),
  o
- copiando los PDF a mano (por ejemplo, desde la herramienta anterior
  `GmailAttachmentsExtractor.jar`, o cualquier otro origen) — a este módulo le da igual
  de dónde vengan.

Es un módulo standalone: se puede compilar y ejecutar sin depender de
`ticket-digital-tickets-downloader` (ni cargar las librerías de Google, que este módulo
no necesita para nada).

## Uso en línea de comandos

```bash
# Ver los subcomandos disponibles
./gradlew :ticket-digital-data:run --args="--help"

# Generar el CSV
./gradlew :ticket-digital-data:run --args="write-items-csv --data-dir ~/.ticket-digital/data --csv-file ~/tickets.csv"

# Generar el XLSX (a partir de la plantilla Mercadona-base.xlsx)
./gradlew :ticket-digital-data:run --args="write-items-xlsx --data-dir ~/.ticket-digital/data --output-dir ~/"

# Calcular el índice de inflación de la cesta de la compra (Laspeyres y Paasche)
./gradlew :ticket-digital-data:run --args="inflation-index --data-dir ~/.ticket-digital/data"

# Ver la cesta de la compra calculada, con el peso relativo de cada producto
./gradlew :ticket-digital-data:run --args="basket-composition --data-dir ~/.ticket-digital/data"
```

### Opciones de `write-items-csv`

| Opción | Descripción |
|---|---|
| `--data-dir` | Directorio donde están los PDF de tickets a analizar (por defecto: `~/.ticket-digital/data`). |
| `--csv-file` | Fichero donde se escribe el CSV generado. |
| `-v`, `--verbose` | Activa logs de nivel DEBUG. |

### Opciones de `write-items-xlsx`

| Opción | Descripción |
|---|---|
| `--data-dir` | Directorio donde están los PDF de tickets a analizar (por defecto: `~/.ticket-digital/data`). |
| `--output-dir` | Directorio donde se escribe el XLSX generado (`Mercadona-yyyyMMdd.xlsx`). |
| `-v`, `--verbose` | Activa logs de nivel DEBUG. |

Además de la hoja "Datos", si hay histórico suficiente para fijar un año base (ver
[`docs/inflation-index.md`](../../docs/inflation-index.md)) también se rellena la hoja
"MiInflación" con el mismo índice de inflación (Laspeyres/Paasche por año) que muestra por
consola `inflation-index`. Si no hay histórico suficiente, esa hoja se deja tal cual está en la
plantilla.

### Opciones de `inflation-index`

| Opción | Descripción |
|---|---|
| `--data-dir` | Directorio donde están los PDF de tickets a analizar (por defecto: `~/.ticket-digital/data`). |
| `-v`, `--verbose` | Activa logs de nivel DEBUG y lista los productos de la cesta calculada. |

El programa calcula automáticamente el año base y la cesta de la compra a partir del
propio histórico (no se pueden indicar a mano) y muestra por pantalla una tabla con el
índice de Laspeyres y el de Paasche de cada año (como % de variación frente al año
base), marcando el año base y los años con cobertura incompleta (menos de 10 meses con
compras). De momento el resultado solo se imprime por consola, no genera ningún fichero.

Los criterios usados (qué año se elige como base, qué productos entran en la cesta,
cómo se tratan los precios que faltan un año concreto, etc.) están documentados con su
razonamiento en [`docs/inflation-index.md`](../../docs/inflation-index.md).

### Opciones de `basket-composition`

| Opción | Descripción |
|---|---|
| `--data-dir` | Directorio donde están los PDF de tickets a analizar (por defecto: `~/.ticket-digital/data`). |
| `-v`, `--verbose` | Activa logs de nivel DEBUG. |

Muestra la misma cesta que calcula `inflation-index`, con el gasto en el año base y el
peso relativo (%) de cada producto sobre el gasto total de la cesta, ordenados de mayor
a menor peso. Útil para ver qué productos "pesan" más en el índice de inflación.

`~` al principio de una ruta se expande al directorio del usuario aunque tu shell no lo
haga por ti (ver detalle en el README de `ticket-digital-tickets-downloader`).

Si quieres descargar y generar el CSV/XLSX con un único comando, usa
[`ticket-digital-launcher`](../ticket-digital-launcher/README.md).
