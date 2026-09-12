package org.terra.incognita.ticketdigital.mercadona.gmail;

import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagePartAttachmentsTest {

    private static final Pattern PDF_REGEX = Pattern.compile(".*\\.pdf$");

    @Test
    void encuentraUnAdjuntoPdfAnidadoEnPartesMultipart() {
        MessagePart pdfAttachment = new MessagePart()
                .setFilename("ticket.pdf")
                .setBody(new MessagePartBody().setAttachmentId("attachment-1"));
        MessagePart textPart = new MessagePart()
                .setFilename("")
                .setBody(new MessagePartBody());
        MessagePart nestedMultipart = new MessagePart()
                .setMimeType("multipart/mixed")
                .setParts(List.of(textPart, pdfAttachment));
        MessagePart root = new MessagePart()
                .setMimeType("multipart/mixed")
                .setParts(List.of(nestedMultipart));

        List<MessagePart> found = MessagePartAttachments.find(root, PDF_REGEX);

        assertEquals(1, found.size());
        assertEquals("ticket.pdf", found.get(0).getFilename());
    }

    @Test
    void ignoraAdjuntosCuyoNombreNoMatcheaElRegex() {
        MessagePart imageAttachment = new MessagePart()
                .setFilename("logo.png")
                .setBody(new MessagePartBody().setAttachmentId("attachment-2"));
        MessagePart root = new MessagePart().setParts(List.of(imageAttachment));

        assertTrue(MessagePartAttachments.find(root, PDF_REGEX).isEmpty());
    }

    @Test
    void ignoraPartesConNombreDeFicheroPeroSinAttachmentId() {
        MessagePart inlinePart = new MessagePart()
                .setFilename("ticket.pdf")
                .setBody(new MessagePartBody());
        MessagePart root = new MessagePart().setParts(List.of(inlinePart));

        assertTrue(MessagePartAttachments.find(root, PDF_REGEX).isEmpty());
    }

    @Test
    void devuelveVacioParaRaizNula() {
        assertTrue(MessagePartAttachments.find(null, PDF_REGEX).isEmpty());
    }
}
