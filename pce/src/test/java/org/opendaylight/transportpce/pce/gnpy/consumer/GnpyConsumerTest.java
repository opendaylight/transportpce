/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.Result;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class GnpyConsumerTest extends JerseyTest {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyConsumerTest.class);
    private JsonStringConverter<GnpyApi> gnpyApiConverter;

    @Override
    protected Application configure() {
        gnpyApiConverter = new JsonStringConverter<>(
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        return new ResourceConfig(GnpyStub.class);
    }

    @Test
    public void isAvailableTest() {
        GnpyConsumer gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998",
                "mylogin",
                "mypassword",
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        assertTrue("Gnpy should be available", gnpyConsumer.isAvailable());
    }

    @Test
    public void computePathsTest() throws IOException {
        GnpyConsumer gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998",
                "mylogin",
                "mypassword",
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        QName pathQname = QName.create("gnpy:gnpy-api", "2019-01-03", "gnpy-api");
        YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
        GnpyApi request = gnpyApiConverter
                .createDataObjectFromJsonString(yangId,
                        Files.readString(Paths.get("src/test/resources/gnpy/gnpy_request.json")),
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
        Result result = gnpyConsumer.computePaths(request);
        LOG.info("Response received {}", result);
        assertNotNull("Result should not be null", result);
    }
}
