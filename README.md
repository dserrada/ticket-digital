# ticket-digital

Procesa los tickets digitales de Mercadona (PDF recibidos por email): los descarga
desde Gmail y genera un CSV o un XLSX con el detalle de todo lo comprado.

Proyecto Gradle multi-módulo. Cada módulo se puede compilar y ejecutar por separado
(cargando solo sus propias dependencias), y hay un módulo agregador con todo junto:

| Módulo | Qué hace | README |
|---|---|---|
| [`ticket-digital-launcher`](projects/ticket-digital-launcher/README.md) | **CLI recomendada para uso normal**: un único comando `ticket-digital` con todos los subcomandos de los dos módulos siguientes. | [README](projects/ticket-digital-launcher/README.md) |
| [`ticket-digital-tickets-downloader`](projects/ticket-digital-tickets-downloader/README.md) | Descarga desde Gmail los PDF de tickets nuevos a un directorio local (`download-tickets`). Cómo obtener las credenciales de Google está aquí. | [README](projects/ticket-digital-tickets-downloader/README.md) |
| [`ticket-digital-data`](projects/ticket-digital-data/README.md) | Genera CSV/XLSX a partir de los PDF ya descargados (`write-items-csv`, `write-items-xlsx`). | [README](projects/ticket-digital-data/README.md) |
| `ticket-digital-parser` | Librería de parseo de los PDF de Mercadona. Sin CLI propia; la usa `ticket-digital-data` internamente. | — |

## Inicio rápido

Para el uso normal (descargar + generar CSV/XLSX), usa el módulo agregador
`ticket-digital-launcher`:

```bash
./gradlew :ticket-digital-launcher:run --args="--help"
```

La primera vez, sigue el [README de `ticket-digital-tickets-downloader`](projects/ticket-digital-tickets-downloader/README.md#1-obtener-credentialsjson-desde-google-cloud-console)
para obtener el fichero `credentials.json` (necesario solo para descargar; no hace falta
si ya tienes los PDF en un directorio).

Cada módulo también se puede ejecutar solo (sin cargar las dependencias de los demás):
ver su README para el detalle de opciones de cada subcomando.
