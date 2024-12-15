/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.Result;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GnpyConsumerTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyConsumerTest.class);
    private final JsonStringConverter<Request> gnpyApiConverter = new JsonStringConverter<>(
            AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
    private final WireMockServer wireMockServer = new WireMockServer(9998);

    @BeforeEach
    void setUp() {
        wireMockServer.start();
        wireMockServer.resetAll();
        configureFor("localhost", 9998);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void isAvailableTest() throws IOException {
        // GIVEN
        stubFor(get(urlEqualTo("/api/v1/status"))
                .willReturn(okJson(Files
                        .readString(Paths
                                .get("src", "test", "resources", "gnpy", "gnpy_status.json")))));

        // WHEN
        GnpyConsumer gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998",
                "mylogin",
                "mypassword",
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        boolean gnpyConsumerAvailable = gnpyConsumer.isAvailable();

        // THEN
        assertTrue(gnpyConsumerAvailable, "Gnpy should be available");
    }

    @Test
    void computePathsTest() throws IOException {
        // GIVEN
        stubFor(post(urlEqualTo("/api/v1/path-computation"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Files
                        .readString(Paths
                                .get("src", "test", "resources", "gnpy", "gnpy_result_with_path.json")))));

        // WHEN
        GnpyConsumer gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998",
                "mylogin",
                "mypassword",
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        QName pathQname = Request.QNAME;
        YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
        Request request = gnpyApiConverter
                .createDataObjectFromJsonString(yangId,
                        Files.readString(Paths.get("src/test/resources/gnpy/gnpy_request.json")),
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
        Result result = gnpyConsumer.computePaths(request);

        // THEN
        LOG.info("Response received {}", result);
        assertNotNull(result, "Result should not be null");
    }
}
