package org.terra.incognita.ticketdigital.mercadona.gmail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GmailScopeValidatorTest {

    @Test
    void aceptaExactamenteElScopeDeSoloLectura() {
        GmailScopeValidator.assertReadOnly("https://www.googleapis.com/auth/gmail.readonly");
    }

    @Test
    void abortaSiElScopeIncluyeModificacionAdemasDeLectura() {
        assertThrows(IllegalStateException.class, () -> GmailScopeValidator.assertReadOnly(
                "https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/gmail.modify"));
    }

    @Test
    void abortaSiElScopeEsSoloModificacion() {
        assertThrows(IllegalStateException.class, () ->
                GmailScopeValidator.assertReadOnly("https://www.googleapis.com/auth/gmail.modify"));
    }

    @Test
    void abortaSiElScopeEsNulo() {
        assertThrows(IllegalStateException.class, () -> GmailScopeValidator.assertReadOnly(null));
    }

    @Test
    void abortaSiElScopeEstaVacio() {
        assertThrows(IllegalStateException.class, () -> GmailScopeValidator.assertReadOnly("  "));
    }
}
