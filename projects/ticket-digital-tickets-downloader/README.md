# ticket-digital-tickets-downloader

Descarga desde Gmail los PDF de tickets de Mercadona a un directorio local, usando la
API oficial de Gmail (no la herramienta de terceros `GmailAttachmentsExtractor.jar`
usada anteriormente). Es parte del proyecto [`ticket-digital`](../../README.md).

Es un módulo standalone: se puede compilar y ejecutar sin depender de
[`ticket-digital-data`](../ticket-digital-data/README.md) (ni cargar sus dependencias).
Si quieres un único CLI con todo (descarga + generación de CSV/XLSX), usa
[`ticket-digital-launcher`](../ticket-digital-launcher/README.md), que compone este
comando y los de `ticket-digital-data` sin duplicar código.

- **Solo lectura**: se solicita únicamente el permiso (scope) OAuth
  `https://www.googleapis.com/auth/gmail.readonly`. Además, antes de descargar nada, el
  programa verifica en tiempo de ejecución que el token obtenido tiene exactamente ese
  scope y ningún otro; si detecta un permiso mayor (por ejemplo `gmail.modify`), **aborta
  sin tocar la API de mensajes**.
- **Incremental**: no vuelve a descargar todo el histórico en cada ejecución. Recuerda
  la fecha del último mensaje procesado y solo pide a Gmail los mensajes posteriores
  (con un pequeño margen de solape). Los ficheros que ya existen en el directorio de
  destino nunca se vuelven a descargar.
- **Sin secretos en el repositorio**: el fichero de credenciales, el token OAuth y el
  estado de la descarga incremental viven, por defecto, fuera del repositorio, bajo
  `~/.ticket-digital/`.

## 1. Obtener `credentials.json` desde Google Cloud Console

Este paso solo hay que hacerlo una vez (o si se pierde el fichero).

1. Ve a [Google Cloud Console](https://console.cloud.google.com/) y crea un proyecto
   nuevo, o selecciona uno existente que quieras usar para esto.
2. En el menú, ve a **APIs & Services → Library**, busca **Gmail API** y pulsa
   **Enable**.
3. Ve a **APIs & Services → OAuth consent screen**:
   - Tipo de usuario: **External** (o **Internal** si tu cuenta es de una organización
     Google Workspace y quieres restringirlo a ella).
   - Rellena los campos obligatorios (nombre de la app, correo de soporte, correo de
     contacto del desarrollador). No hace falta publicarla ni pedir verificación a
     Google para uso personal: basta con añadir tu propia cuenta de Gmail como
     **Test user** en la sección correspondiente.
   - En la sección **Data Access** (o **Scopes**, según la versión de la consola),
     pulsa **Add or Remove Scopes** y añade únicamente
     `https://www.googleapis.com/auth/gmail.readonly` (búscalo o pégalo directamente).
     No añadas ningún otro scope de Gmail. Esto declara ante Google qué va a pedir esta
     app y es lo que se muestra al usuario en la pantalla de consentimiento ("Ver tus
     mensajes de correo electrónico y su configuración").
4. Ve a **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Tipo de aplicación: **Desktop app**.
   - Ponle un nombre (p. ej. "ticket-digital-tickets-downloader").
   - Pulsa **Create**.
5. Descarga el JSON generado (botón de descarga junto al client ID recién creado, o
   desde **Credentials** más tarde) y guárdalo como:
   ```
   ~/.ticket-digital/credentials.json
   ```
   (crea el directorio `~/.ticket-digital/` si no existe).

> **Importante**: `credentials.json` es un secreto. No lo subas nunca al repositorio, ni
> lo compartas. El `.gitignore` del proyecto ya lo excluye como refuerzo, pero la
> ubicación por defecto (`~/.ticket-digital/`) está fuera del propio repositorio
> precisamente para evitar el riesgo.
>
> Si ya tenías un `credentials.json` generado para la herramienta anterior
> (`GmailAttachmentsExtractor.jar`), es el mismo tipo de cliente OAuth ("Desktop app")
> y se puede reutilizar tal cual — no hace falta crear un proyecto nuevo en Google Cloud
> Console ni repetir el consentimiento.

La primera vez que se ejecute el comando, se abrirá el navegador para completar el
consentimiento OAuth (login con la cuenta de Gmail y aceptar el permiso de solo
lectura). El token resultante se guarda en `~/.ticket-digital/tokens/`, así que las
siguientes ejecuciones no vuelven a pedir login mientras el token siga siendo válido.

### ¿Esto garantiza que la app nunca podrá pedir más que lectura?

Configurar el scope en la pantalla de consentimiento (paso 3 de arriba) **declara** el
permiso y es lo que ve el usuario al autorizar, pero no es una restricción criptográfica
del propio `credentials.json`: un cliente OAuth "Desktop app" no lleva codificado dentro
qué scopes puede o no pedir — eso lo decide, en cada petición, el código que construye la
URL de autorización.

Por eso la garantía real, la que sí depende de este proyecto y no de la configuración de
Google Cloud, está en el código (`GmailTicketDownloader`), en dos capas:

1. Solo se solicita un scope: `GmailScopes.GMAIL_READONLY`. En ningún punto del código se
   pide `gmail.modify`, `gmail.settings.basic` ni `https://mail.google.com/`.
2. Antes de descargar nada, se verifica en tiempo de ejecución (contra el endpoint de
   tokeninfo de Google) que el token obtenido tiene **exactamente** ese scope y ningún
   otro; si no es así —por ejemplo, por reutilizar sin querer un token guardado
   previamente con más permisos—, el programa aborta sin llamar a la API de mensajes
   (ver `GmailScopeValidator`).

En resumen: declara el scope de solo lectura en la consola (buena práctica, deja rastro
de qué pide la app), pero confía en la validación en tiempo de ejecución del propio
programa como mecanismo de seguridad real.

## 2. Construcción y uso en línea de comandos

Gradle se usa aquí solo para construir el artefacto, no para ejecutarlo:

```bash
./gradlew :ticket-digital-tickets-downloader:installDist
```

Esto genera en
`projects/ticket-digital-tickets-downloader/build/install/ticket-digital-tickets-downloader/bin/`
un script ejecutable con todas las dependencias incluidas, que no necesita Gradle para
funcionar. Ejecutado como módulo standalone (sin `ticket-digital-data`), sin Gradle:

```bash
DOWNLOADER=./projects/ticket-digital-tickets-downloader/build/install/ticket-digital-tickets-downloader/bin/ticket-digital-tickets-downloader

$DOWNLOADER --data-dir ~/.ticket-digital/data --query 'label:mercadona from: ticket_digital@mail.mercadona.com' --filename-regex '.*\.pdf$'
```

Todas las opciones tienen valores por defecto pensados para el caso de uso actual, así
que en el día a día basta con:

```bash
$DOWNLOADER
```

O, integrado en el CLI "todo en uno" (módulo `ticket-digital-launcher`, que también
incluye `write-items-csv`/`write-items-xlsx` de `ticket-digital-data`; ver su
[README](../ticket-digital-launcher/README.md) para construirlo y ejecutarlo):

```bash
./projects/ticket-digital-launcher/build/install/ticket-digital-launcher/bin/ticket-digital-launcher download-tickets
```

### Opciones disponibles

| Opción | Descripción | Valor por defecto |
|---|---|---|
| `--data-dir` | Directorio donde se guardan los PDF descargados | `~/.ticket-digital/data` |
| `--credentials-file` | Fichero `credentials.json` (ver paso 1) | `~/.ticket-digital/credentials.json` |
| `--token-dir` | Directorio donde se guarda el token OAuth | `~/.ticket-digital/tokens` |
| `--state-file` | Fichero de estado de la descarga incremental | `~/.ticket-digital/state/gmail-download-state.properties` |
| `--query` | Query de búsqueda de Gmail | `label:mercadona from: ticket_digital@mail.mercadona.com` |
| `--filename-regex` | Regex que debe matchear el nombre del adjunto | `.*\.pdf$` |
| `-v`, `--verbose` | Activa logs de nivel DEBUG (progreso detallado: página de resultados, mensaje que se está procesando, adjunto que se está descargando, etc). Sin esta opción solo se ven los logs de nivel INFO (inicio, cada fichero descargado, resumen final). | (desactivado) |

> Nota sobre `~`: el propio programa expande un `~` inicial en `--data-dir`,
> `--credentials-file`, `--token-dir` y `--state-file` al directorio del usuario,
> igual que hace una shell Unix — no depende de que la shell lo haga por ti. Esto es
> importante porque **no todas las shells expanden `~` automáticamente** (cmd.exe y
> PowerShell en Windows no lo hacen, y tampoco se expande si el argumento va entre
> comillas simples); si tu shell no lo expande y el programa tampoco lo hiciera, acabarías
> con una carpeta creada literalmente llamada `~` en el directorio desde el que
> ejecutaste el comando, en vez de en tu home real.

### Equivalencia con el uso anterior de `GmailAttachmentsExtractor.jar`

El uso habitual hasta ahora era:

```bash
java -jar GmailAttachmentsExtractor.jar \
  "label:mercadona from: ticket_digital@mail.mercadona.com" \
  --no-modify-gmail --filename '.*\.pdf$' .\mails\
```

Su equivalente con este módulo (con los valores por defecto, no hace falta escribirlos):

```bash
$DOWNLOADER --data-dir .\mails\ --query "label:mercadona from: ticket_digital@mail.mercadona.com" --filename-regex '.*\.pdf$'
```

La diferencia principal es que aquí `--no-modify-gmail` no es una opción: el permiso de
solo lectura es la única opción posible (ver más arriba), y además la descarga es
incremental por defecto, no hace falta gestionar manualmente qué ya se había descargado.
