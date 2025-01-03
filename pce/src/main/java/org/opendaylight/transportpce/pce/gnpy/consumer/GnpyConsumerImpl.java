/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.Result;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
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
    private static final String STATUS_ENDPOINT = "/api/v1/status";
    private static final String PATH_COMPUTATION_ENDPOINT = "/api/v1/path-computation";
    private final HttpClient client;
    private final URI statusUri;
    private final URI pathComputationUri;
    private final JsonStringConverter<Request> requestConverter;
    private final JsonStringConverter<Result> resultConverter;

    @Activate
    public GnpyConsumerImpl(final Configuration configuration,
            @Reference BindingDOMCodecServices bindingDOMCodecServices) {
        this(configuration.url(), configuration.username(), configuration.password(), bindingDOMCodecServices);
    }

    public GnpyConsumerImpl(String baseUrl, String username, String password,
            BindingDOMCodecServices bindingDOMCodecServices) {
        LOG.info("baseUrl: {}, username: {}, password: {}", baseUrl, username, password);
        this.client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                username,
                                password.toCharArray());
                    }
                }).build();
        this.statusUri = URI.create(baseUrl + STATUS_ENDPOINT);
        this.pathComputationUri = URI.create(baseUrl + PATH_COMPUTATION_ENDPOINT);
        requestConverter = new JsonStringConverter<>(bindingDOMCodecServices);
        resultConverter = new JsonStringConverter<>(bindingDOMCodecServices);
    }

    @Deactivate
    public void close() {
        LOG.info("Closing client {}", this.client);
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(statusUri)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOG.info("GNPy available {}", response.statusCode() == 200);
            return response.statusCode() == 200;
        } catch (IOException e) {
            LOG.info("GNPy is not available ", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public Result computePaths(final Request request) {
        try {
            String requestBody = requestConverter.createJsonStringFromDataObject(
                            DataObjectIdentifier.builder(Request.class).build(),
                            request,
                            JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02)
                    .replace("gnpy-network-topology:", "");
            LOG.info("requestbody = {}", requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(pathComputationUri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            httpRequest.headers().map().forEach((k, v) -> LOG.info("header {}: {}", k, v));
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            LOG.info("response = {}", response);

            if (response.statusCode() == 201) {
                LOG.info("response body = {}", response.body());
                return resultConverter.createDataObjectFromJsonString(YangInstanceIdentifier.of(Result.QNAME),
                        response.body(),
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
            } else {
                LOG.info("Error response: {}", response.body());
                return null;
            }
        } catch (IOException e) {
            LOG.info("Something went wrong while requesting GNPy ", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
