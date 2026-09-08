package org.terra.incognita.ticketdigital.mercadona.gmail;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reproduce el caso de uso que hoy cubre la herramienta externa
 * {@code GmailAttachmentsExtractor.jar}: busca en Gmail los mensajes que matchean una
 * query, y descarga los adjuntos cuyo nombre matchea un regex a un directorio local.
 * <p>
 * A diferencia de la herramienta externa: solo pide el scope de solo lectura
 * {@link GmailScopes#GMAIL_READONLY} y verifica en tiempo de ejecución que el token
 * obtenido no tenga más permisos (ver {@link GmailScopeValidator}); y la descarga es
 * incremental (ver {@link DownloadState}), no vuelve a traer todo el histórico cada vez.
 */
public class GmailTicketDownloader {

    private static final Logger logger = LoggerFactory.getLogger(GmailTicketDownloader.class);

    private static final String APPLICATION_NAME = "ticket-digital-tickets-downloader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_READONLY);
    private static final String USER_ID = "me";

    private final TokenScopeChecker scopeChecker;

    public GmailTicketDownloader() {
        this(new GoogleTokenScopeChecker());
    }

    GmailTicketDownloader(TokenScopeChecker scopeChecker) {
        this.scopeChecker = scopeChecker;
    }

    /**
     * @return número de ficheros nuevos descargados.
     */
    public int download(DownloadRequest request) throws IOException, GeneralSecurityException {
        logger.info("Iniciando descarga de tickets desde Gmail en {}", request.dataDir());
        if (!Files.exists(request.credentialsFile())) {
            throw new IOException("No se encuentra el fichero de credenciales: " + request.credentialsFile()
                    + ". Consulta el README.md de ticket-digital-tickets-downloader para generarlo.");
        }
        Files.createDirectories(request.dataDir());
        Files.createDirectories(request.tokenDirectory());

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        logger.info("Autenticando con Gmail (credenciales: {})...", request.credentialsFile());
        Credential credential = authorize(httpTransport, request.credentialsFile(), request.tokenDirectory());
        logger.debug("Autenticación completada.");

        logger.debug("Verificando que el token OAuth es de solo lectura...");
        String grantedScope = scopeChecker.fetchGrantedScope(credential.getAccessToken());
        GmailScopeValidator.assertReadOnly(grantedScope);
        logger.debug("Alcance del token OAuth verificado como solo lectura ({}).", GmailScopes.GMAIL_READONLY);

        Gmail service = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        DownloadState state = DownloadState.load(request.stateFile());
        String effectiveQuery = state.appendIncrementalClause(request.query());
        logger.info("Buscando mensajes en Gmail con query: {}", effectiveQuery);

        Pattern filenamePattern = Pattern.compile(request.filenameRegex());
        int downloaded = 0;
        int messagesProcessed = 0;
        int pageNumber = 0;
        Instant latestMessageDate = state.lastMessageDate();
        String pageToken = null;
        do {
            pageNumber++;
            logger.debug("Pidiendo página {} de resultados a Gmail...", pageNumber);
            ListMessagesResponse listResponse = service.users().messages().list(USER_ID)
                    .setQ(effectiveQuery)
                    .setPageToken(pageToken)
                    .execute();
            List<Message> summaries = listResponse.getMessages();
            logger.debug("Página {}: {} mensaje(s) (estimado total: {}).", pageNumber,
                    summaries == null ? 0 : summaries.size(), listResponse.getResultSizeEstimate());
            if (summaries != null) {
                for (Message summary : summaries) {
                    messagesProcessed++;
                    logger.debug("Procesando mensaje #{} (id={})...", messagesProcessed, summary.getId());
                    Message message = service.users().messages().get(USER_ID, summary.getId()).execute();
                    Instant messageDate = Instant.ofEpochMilli(message.getInternalDate());
                    downloaded += downloadMatchingAttachments(service, message, filenamePattern, request.dataDir());
                    if (messageDate.isAfter(latestMessageDate)) {
                        latestMessageDate = messageDate;
                    }
                }
            }
            pageToken = listResponse.getNextPageToken();
        } while (pageToken != null);

        state.withLastMessageDate(latestMessageDate).save(request.stateFile());
        logger.info("Descarga completada: {} fichero(s) nuevo(s) de {} mensaje(s) revisados.",
                downloaded, messagesProcessed);
        return downloaded;
    }

    private int downloadMatchingAttachments(Gmail service, Message message, Pattern filenamePattern, Path dataDir)
            throws IOException {
        int downloaded = 0;
        List<MessagePart> attachments = MessagePartAttachments.find(message.getPayload(), filenamePattern);
        if (attachments.isEmpty()) {
            logger.debug("Mensaje {}: sin adjuntos que matcheen el regex.", message.getId());
        }
        for (MessagePart attachmentPart : attachments) {
            Path target = dataDir.resolve(attachmentPart.getFilename());
            if (Files.exists(target)) {
                logger.debug("Ya existe, se omite: {}", target);
                continue;
            }
            logger.debug("Descargando adjunto {} del mensaje {}...", attachmentPart.getFilename(), message.getId());
            MessagePartBody body = service.users().messages().attachments()
                    .get(USER_ID, message.getId(), attachmentPart.getBody().getAttachmentId())
                    .execute();
            byte[] bytes = Base64.getUrlDecoder().decode(body.getData());
            Files.write(target, bytes);
            logger.info("Descargado {}", target);
            downloaded++;
        }
        return downloaded;
    }

    private Credential authorize(NetHttpTransport httpTransport, Path credentialsFile, Path tokenDirectory)
            throws IOException {
        try (InputStream in = Files.newInputStream(credentialsFile)) {
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in, StandardCharsets.UTF_8));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(tokenDirectory.toFile()))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            logger.debug("Token no válido en caché (o inexistente): puede que se abra el navegador para "
                    + "completar el consentimiento OAuth.");
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }
}
