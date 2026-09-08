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

## Uso en línea de comandos

```bash
# Ver todos los subcomandos disponibles
./gradlew :ticket-digital-launcher:run --args="--help"

# Descargar los tickets nuevos desde Gmail (ver README de ticket-digital-tickets-downloader
# para las opciones y cómo obtener las credenciales)
./gradlew :ticket-digital-launcher:run --args="download-tickets"

# Generar el CSV a partir de los PDF ya descargados (ver README de ticket-digital-data
# para el resto de opciones)
./gradlew :ticket-digital-launcher:run --args="write-items-csv --data-dir ~/.ticket-digital/data --csv-file ~/tickets.csv"

# Generar el XLSX
./gradlew :ticket-digital-launcher:run --args="write-items-xlsx --data-dir ~/.ticket-digital/data --output-dir ~/"
```

Cada subcomando tiene su propia ayuda:

```bash
./gradlew :ticket-digital-launcher:run --args="download-tickets --help"
./gradlew :ticket-digital-launcher:run --args="write-items-csv --help"
./gradlew :ticket-digital-launcher:run --args="write-items-xlsx --help"
```

Todos los subcomandos aceptan `-v`/`--verbose` para logs de nivel DEBUG con el detalle
de qué está haciendo en cada momento.

### Distribución

`./gradlew :ticket-digital-launcher:installDist` genera en
`projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/` un script
ejecutable (`ticket-digital-launcher` / `ticket-digital-launcher.bat`) que no necesita
Gradle para funcionar, con todas las dependencias incluidas.
