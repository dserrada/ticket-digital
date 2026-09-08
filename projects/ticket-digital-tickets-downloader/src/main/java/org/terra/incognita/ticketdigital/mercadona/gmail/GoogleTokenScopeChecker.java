package org.terra.incognita.ticketdigital.mercadona.gmail;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;

/**
 * Implementación real de {@link TokenScopeChecker}: consulta el endpoint público de
 * tokeninfo de Google.
 */
public class GoogleTokenScopeChecker implements TokenScopeChecker {

    private static final String TOKENINFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo";

    private final HttpTransport transport;

    public GoogleTokenScopeChecker() {
        this(new NetHttpTransport());
    }

    GoogleTokenScopeChecker(HttpTransport transport) {
        this.transport = transport;
    }

    @Override
    public String fetchGrantedScope(String accessToken) throws IOException {
        GenericUrl url = new GenericUrl(TOKENINFO_URL);
        url.set("access_token", accessToken);
        HttpRequest request = transport.createRequestFactory().buildGetRequest(url);
        request.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
        HttpResponse response = request.execute();
        try {
            GenericJson json = response.parseAs(GenericJson.class);
            Object scope = json.get("scope");
            return scope == null ? null : scope.toString();
        } finally {
            response.disconnect();
        }
    }
}
