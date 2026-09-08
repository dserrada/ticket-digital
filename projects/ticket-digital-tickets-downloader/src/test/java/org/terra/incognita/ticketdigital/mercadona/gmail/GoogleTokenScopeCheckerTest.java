package org.terra.incognita.ticketdigital.mercadona.gmail;

import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleTokenScopeCheckerTest {

    @Test
    void parseaElScopeDevueltoPorElEndpointDeTokeninfo() throws IOException {
        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse()
                .setContentType("application/json")
                .setContent("{\"scope\": \"https://www.googleapis.com/auth/gmail.readonly\"}");
        MockHttpTransport transport = new MockHttpTransport.Builder()
                .setLowLevelHttpRequest(new MockLowLevelHttpRequest() {
                    @Override
                    public com.google.api.client.testing.http.MockLowLevelHttpResponse execute() {
                        return response;
                    }
                })
                .build();
        GoogleTokenScopeChecker checker = new GoogleTokenScopeChecker(transport);

        String scope = checker.fetchGrantedScope("fake-access-token");

        assertEquals("https://www.googleapis.com/auth/gmail.readonly", scope);
    }
}
