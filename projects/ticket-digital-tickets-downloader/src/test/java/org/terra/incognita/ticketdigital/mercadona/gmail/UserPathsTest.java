package org.terra.incognita.ticketdigital.mercadona.gmail;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPathsTest {

    @Test
    void expandeSoloVirgulillaAlHomeDelUsuario() {
        assertEquals(Path.of(System.getProperty("user.home")), UserPaths.resolve("~"));
    }

    @Test
    void expandeVirgulillaSeguidaDeBarraAlHomeDelUsuario() {
        Path expected = Path.of(System.getProperty("user.home")).resolve(".ticket-digital/data");

        assertEquals(expected, UserPaths.resolve("~/.ticket-digital/data"));
    }

    @Test
    void noTocaRutasQueNoEmpiezanPorVirgulilla() {
        assertEquals(Path.of("/tmp/data"), UserPaths.resolve("/tmp/data"));
    }

    @Test
    void noExpandeUnaVirgulillaQueNoEstaAlPrincipio() {
        assertEquals(Path.of("foo~bar"), UserPaths.resolve("foo~bar"));
    }
}
