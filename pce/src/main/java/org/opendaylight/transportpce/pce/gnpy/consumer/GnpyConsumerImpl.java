/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Base64;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.Result;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "org.opendaylight.transportpce.pce")
public class GnpyConsumerImpl implements GnpyConsumer {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition
        String url() default "http://127.0.0.1:8008";
        @AttributeDefinition
        String username() default "gnpy";
        @AttributeDefinition
        String password() default "gnpy";
    }

    private static final Logger LOG = LoggerFactory.getLogger(GnpyConsumerImpl.class);
    private final HttpClient client;
    private final String baseUrl;
    private final String username;
    private final String password;
    private ObjectMapper objectMapper;
    private JsonStringConverter<Request> requestConverter;
    JsonStringConverter<Result> resultConverter;

    @Activate
    public GnpyConsumerImpl(final Configuration configuration,
            @Reference BindingDOMCodecServices bindingDOMCodecServices) {
        this(configuration.url(), configuration.username(), configuration.password(), bindingDOMCodecServices);
    }

    public GnpyConsumerImpl(String baseUrl, String username, String password,
            BindingDOMCodecServices bindingDOMCodecServices) {
        LOG.info("baseUrl: {}, username: {}, password: {}", baseUrl, username, password);
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.client = HttpClient.newBuilder()
                .version(Version.HTTP_2)
                .build();
        requestConverter = new JsonStringConverter<>(bindingDOMCodecServices);
        resultConverter = new JsonStringConverter<>(bindingDOMCodecServices);
        this.objectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature(), true)
                .registerModule(new GnpyModule(requestConverter, resultConverter));
    }

    @Deactivate
    public void close() {
        LOG.info("Closing client {}", this.client);
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/status"))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password)
                            .getBytes(Charset.forName("UTF-8"))))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("GNPy is available");
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            LOG.info("GNPy is not available ", e);
            return false;
        }
    }

    @Override
    public Result computePaths(Request request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            LOG.info("requestbody = {}", requestBody);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/path-computation"))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password)
                            .getBytes(Charset.forName("UTF-8"))))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            LOG.info("response = {}", response);

            if (response.statusCode() == 201) {
                LOG.info("response body = {}", response.body());
                return objectMapper.readValue(response.body(), Result.class);
            } else {
                LOG.info("Error response: {}", response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.info("Something went wrong while requesting GNPy ", e);
            return null;
        }
    }
}
