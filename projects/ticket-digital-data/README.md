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
```

### Opciones de `write-items-csv`

| Opción | Descripción |
|---|---|
| `--data-dir` | Directorio donde están los PDF de tickets a analizar. |
| `--csv-file` | Fichero donde se escribe el CSV generado. |
| `-v`, `--verbose` | Activa logs de nivel DEBUG. |

### Opciones de `write-items-xlsx`

| Opción | Descripción |
|---|---|
| `--data-dir` | Directorio donde están los PDF de tickets a analizar. |
| `--output-dir` | Directorio donde se escribe el XLSX generado (`Mercadona-yyyyMMdd.xlsx`). |
| `-v`, `--verbose` | Activa logs de nivel DEBUG. |

`~` al principio de una ruta se expande al directorio del usuario aunque tu shell no lo
haga por ti (ver detalle en el README de `ticket-digital-tickets-downloader`).

Si quieres descargar y generar el CSV/XLSX con un único comando, usa
[`ticket-digital-launcher`](../ticket-digital-launcher/README.md).
