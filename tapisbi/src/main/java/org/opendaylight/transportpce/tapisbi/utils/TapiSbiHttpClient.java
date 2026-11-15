/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client utility for making REST calls to TAPI southbound controllers.
 */
public final class TapiSbiHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiHttpClient.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final String CONTENT_TYPE_JSON = "application/json";

    private TapiSbiHttpClient() {
    }

    /**
     * Makes a GET request to the specified URL with Basic Authentication.
     *
     * @param url      The full URL to call
     * @param username Username for Basic Auth
     * @param password Password for Basic Auth
     * @return JsonObject containing the response, or null if failed
     */
    public static JsonObject executeGet(String url, String username, String password) {
        LOG.debug("Executing GET request to: {}", url);

        try {
            String basicAuth = createBasicAuthHeader(username, password);

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", basicAuth)
                .header("Accept", CONTENT_TYPE_JSON)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.debug("GET request successful, status code: {}", response.statusCode());
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                LOG.error("GET request failed with status code: {}, body: {}",
                    response.statusCode(), response.body());
                return null;
            }

        } catch (IOException e) {
            LOG.error("IOException during GET request to {}: {}", url, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            LOG.error("InterruptedException during GET request to {}: {}", url, e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Creates Basic Authentication header value.
     *
     * @param username Username
     * @param password Password
     * @return Basic Auth header value (e.g., "Basic base64EncodedCredentials")
     */
    private static String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }
}
