# ticket-digital-launcher

CLI "todo en uno" del proyecto [`ticket-digital`](../../README.md): un único comando
`ticket-digital` que agrupa los subcomandos de
[`ticket-digital-data`](../ticket-digital-data/README.md) (generar CSV/XLSX) y de
[`ticket-digital-tickets-downloader`](../ticket-digital-tickets-downloader/README.md)
(descargar desde Gmail). Es el módulo recomendado para el uso normal del día a día.

No tiene código propio de negocio: solo compone, con la propia API de `picocli`, los
comandos que cada uno de esos dos módulos ya construye para sí mismo — no duplica
opciones ni lógica.

Si solo necesitas una de las dos funcionalidades y prefieres no cargar las dependencias
de la otra (por ejemplo, no quieres las librerías de Google si solo vas a generar
CSV/XLSX), ejecuta el módulo correspondiente por separado: ver su propio README.

## Construcción

Gradle se usa aquí solo para construir el artefacto, no para ejecutarlo:

```bash
./gradlew :ticket-digital-launcher:installDist
```

Esto genera en `projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/`
un script ejecutable (`ticket-digital-launcher` / `ticket-digital-launcher.bat`) que no
necesita Gradle para funcionar, con todas las dependencias incluidas.

## Uso en línea de comandos

Sin Gradle, invocando directamente el script ya construido:

```bash
LAUNCHER=./projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/ticket-digital-launcher

# Ver todos los subcomandos disponibles
$LAUNCHER --help

# Descargar los tickets nuevos desde Gmail (ver README de ticket-digital-tickets-downloader
# para las opciones y cómo obtener las credenciales)
$LAUNCHER download-tickets

# Generar el CSV a partir de los PDF ya descargados (ver README de ticket-digital-data
# para el resto de opciones)
$LAUNCHER write-items-csv --data-dir ~/.ticket-digital/data --csv-file ~/tickets.csv

# Generar el XLSX
$LAUNCHER write-items-xlsx --data-dir ~/.ticket-digital/data --output-dir ~/

# Índice de inflación de tu cesta habitual, año a año (ver README de ticket-digital-data)
$LAUNCHER inflation-index --data-dir ~/.ticket-digital/data

# Composición de esa cesta y peso de cada producto en el gasto
$LAUNCHER basket-composition --data-dir ~/.ticket-digital/data
```

Cada subcomando tiene su propia ayuda:

```bash
$LAUNCHER download-tickets --help
$LAUNCHER write-items-csv --help
$LAUNCHER write-items-xlsx --help
$LAUNCHER inflation-index --help
$LAUNCHER basket-composition --help
```

Todos los subcomandos aceptan `-v`/`--verbose` para logs de nivel DEBUG con el detalle
de qué está haciendo en cada momento.

Añadir el directorio `bin/` de la distribución al `PATH` permite invocar directamente
`ticket-digital-launcher` sin la ruta completa.
