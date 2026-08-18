package com.coltrip.backend.auth.google;

import com.coltrip.backend.auth.exception.InvalidGoogleTokenException;
import com.coltrip.backend.config.GoogleOAuthProperties;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleTokenVerifier {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final Set<String> VALID_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final RestClient restClient = RestClient.create();
    private final GoogleOAuthProperties properties;

    public GoogleTokenVerifier(GoogleOAuthProperties properties) {
        this.properties = properties;
    }

    public GoogleUserInfo verify(String idToken) {
        GoogleTokenInfoResponse response = fetchTokenInfo(idToken);

        if (response == null
                || !properties.clientId().equals(response.aud())
                || !VALID_ISSUERS.contains(response.iss())) {
            throw new InvalidGoogleTokenException();
        }

        return new GoogleUserInfo(response.sub(), response.email());
    }

    private GoogleTokenInfoResponse fetchTokenInfo(String idToken) {
        try {
            return restClient.get()
                    .uri(TOKEN_INFO_URL + "?id_token={idToken}", idToken)
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);
        } catch (RestClientException e) {
            throw new InvalidGoogleTokenException();
        }
    }
}
