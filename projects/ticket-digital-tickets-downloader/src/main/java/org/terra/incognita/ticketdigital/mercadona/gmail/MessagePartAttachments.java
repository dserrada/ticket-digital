package org.terra.incognita.ticketdigital.mercadona.gmail;

import com.google.api.services.gmail.model.MessagePart;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Recorrido recursivo del árbol MIME ({@code payload.parts}) de un mensaje de Gmail
 * en busca de las partes que son adjuntos descargables (tienen {@code attachmentId})
 * y cuyo nombre de fichero matchea el regex dado.
 */
public final class MessagePartAttachments {

    private MessagePartAttachments() {
    }

    public static List<MessagePart> find(MessagePart root, Pattern filenameRegex) {
        List<MessagePart> matches = new ArrayList<>();
        collect(root, filenameRegex, matches);
        return matches;
    }

    private static void collect(MessagePart part, Pattern filenameRegex, List<MessagePart> matches) {
        if (part == null) {
            return;
        }
        String filename = part.getFilename();
        if (filename != null && !filename.isEmpty()
                && part.getBody() != null && part.getBody().getAttachmentId() != null
                && filenameRegex.matcher(filename).matches()) {
            matches.add(part);
        }
        List<MessagePart> children = part.getParts();
        if (children != null) {
            for (MessagePart child : children) {
                collect(child, filenameRegex, matches);
            }
        }
    }
}
