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

## Cómo invocarlo

Hay dos formas de ejecutar cualquiera de los módulos con CLI
(`ticket-digital-launcher`, `ticket-digital-data`,
`ticket-digital-tickets-downloader`): con Gradle (para desarrollo, no requiere
instalar nada más) o con el artefacto ya construido (para uso normal del
día a día, sin depender de Gradle en cada ejecución).

### 1. Con Gradle: `./gradlew :<módulo>:run`

```bash
# CLI unificada (recomendado): ver todos los subcomandos
./gradlew :ticket-digital-launcher:run --args="--help"

# Descargar los tickets nuevos desde Gmail
./gradlew :ticket-digital-launcher:run --args="download-tickets"

# Generar el CSV a partir de los PDF ya descargados
./gradlew :ticket-digital-launcher:run --args="write-items-csv --data-dir=~/.ticket-digital/data --csv-file=~/tickets.csv"

# Generar el XLSX
./gradlew :ticket-digital-launcher:run --args="write-items-xlsx --data-dir=~/.ticket-digital/data --output-dir=~/"

# Cualquier subcomando con -v/--verbose para ver el detalle en DEBUG
./gradlew :ticket-digital-launcher:run --args="write-items-csv --data-dir=~/.ticket-digital/data --csv-file=~/tickets.csv -v"
```

Los otros dos módulos se invocan igual, sustituyendo `ticket-digital-launcher`
por el módulo que quieras ejecutar solo (sin cargar las dependencias del
otro): `./gradlew :ticket-digital-data:run --args="write-items-csv ..."` o
`./gradlew :ticket-digital-tickets-downloader:run --args="download-tickets"`.

Nota: todo lo que va después de `--args=` es una única cadena con el
subcomando y sus opciones tal como se pasarían en la línea de comandos.

### 2. Con el artefacto ya construido (sin Gradle en cada ejecución)

Gradle, vía el plugin `application`, genera para cada módulo con CLI un
script de arranque con todas sus dependencias (no un único jar "fat jar": el
`.jar` suelto en `build/libs/` no lleva las dependencias y **no** es
ejecutable directamente con `java -jar`).

```bash
# Genera el script + las dependencias en build/install/<módulo>/
./gradlew :ticket-digital-launcher:installDist

# Ejecutarlo (Linux/macOS)
./projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/ticket-digital-launcher --help
./projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/ticket-digital-launcher download-tickets
./projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/ticket-digital-launcher write-items-csv --data-dir=~/.ticket-digital/data --csv-file=~/tickets.csv

# En Windows: usa el .bat generado en la misma carpeta
projects\ticket-digital-launcher\build\install\ticket-digital-launcher\bin\ticket-digital-launcher.bat --help
```

Ese directorio (`build/install/ticket-digital-launcher/`) es autocontenido:
se puede copiar a otra máquina con Java (misma versión o superior) y
ejecutarse sin Gradle. Para empaquetarlo y llevarlo a otro sitio, usa en su
lugar:

```bash
./gradlew :ticket-digital-launcher:distZip   # o :distTar
# genera projects/ticket-digital-launcher/build/distributions/ticket-digital-launcher-1.0-SNAPSHOT.zip
```

Igual con los otros dos módulos, sustituyendo el nombre
(`:ticket-digital-data:installDist`,
`:ticket-digital-tickets-downloader:installDist`, y el script correspondiente
dentro de `build/install/<módulo>/bin/`).

## Calidad y seguridad

`./gradlew build` (o `./gradlew check`) ejecuta además, en todos los módulos:

- **[OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)**: escanea
  las dependencias de los 4 módulos en busca de vulnerabilidades (CVE) conocidas.
  Informe en `build/reports/dependency-check/dependency-check-report.html` (raíz del
  repo). Por defecto
  el build falla si aparece algún hallazgo de severidad alta/crítica (CVSS ≥ 7).

  Usa como fuente de datos la NVD (National Vulnerability Database) de NIST, vía su API
  2.0. **Necesita sí o sí una API key** — la NVD retiró los feeds JSON antiguos y su API
  actual rechaza toda petición sin clave (falla con `Invalid API Key, length of 0`); ya
  no es "más lento sin ella", es que no funciona. **Mientras no la configures,
  `./gradlew build`/`check` fallará** en la tarea `dependencyCheckAggregate` — es
  intencional, para no poder olvidarse del escaneo.

  Pide la API key gratuita (autoservicio, instantánea, activación por email) en
  https://nvd.nist.gov/developers/request-an-api-key y guárdala **fuera del repo**, en
  `~/.gradle/gradle.properties` (créalo si no existe):

  ```properties
  nvdApiKey=tu-api-key-aquí
  ```

  Nunca la pongas en un fichero del proyecto ni la subas al repo.

  Si algún día aparece un falso positivo, se puede suprimir con
  [`dependencyCheck.suppressionFile`](https://dependency-check.github.io/DependencyCheck/dependency-check-gradle/configuration.html)
  apuntando a un XML de supresiones — no hay ninguno todavía porque no ha hecho falta.

- **[PMD](https://pmd.github.io/)** (`toolVersion 7.27.0`, incluido en Gradle sin plugin
  externo): analiza el código en busca de bugs y malas prácticas reales (ruleset propio
  en [`config/pmd/ruleset.xml`](config/pmd/ruleset.xml), centrado en
  `errorprone`/`bestpractices`, no en estilo/formato). Informe por módulo en
  `<módulo>/build/reports/pmd/main.html` y `test.html`. Bloquea el build ante cualquier
  hallazgo (`ignoreFailures = false`); las exclusiones ya investigadas y justificadas
  están documentadas con su motivo en el propio ruleset.

  Las reglas de concurrencia (`category/java/multithreading.xml`) se configuran aparte,
  por módulo, según si está pensado para ser thread-safe:
  [`config/pmd/multithreading-strict.xml`](config/pmd/multithreading-strict.xml) (sin
  ninguna exclusión) para `ticket-digital-parser` — pensado como base de un futuro
  servicio REST, donde varias peticiones concurrentes reutilizarán las mismas clases — y
  [`config/pmd/multithreading-relaxed.xml`](config/pmd/multithreading-relaxed.xml) para
  el resto de módulos (CLIs de un solo hilo sin planes de dejar de serlo).

- **[SpotBugs](https://spotbugs.github.io/)** (`toolVersion 4.10.4`, plugin
  `com.github.spotbugs`): segunda capa de análisis, complementaria a PMD — en vez de
  patrones sobre el código fuente, analiza el bytecode compilado con dataflow analysis,
  lo que detecta cosas que PMD no ve (p.ej. un posible NullPointerException por el valor
  de retorno de un método, o un `record` que expone una lista/mapa mutable interno sin
  copia defensiva, rompiendo su propia inmutabilidad). Soporta Java 25 desde la 4.9.7
  (subieron BCEL a la 6.11.0; antes de eso no se podía usar en este proyecto —
  [spotbugs/spotbugs#3564](https://github.com/spotbugs/spotbugs/issues/3564)). Informe
  por módulo en `<módulo>/build/reports/spotbugs/main.html` y `test.html`. Bloquea el
  build ante cualquier hallazgo (`ignoreFailures = false`).
