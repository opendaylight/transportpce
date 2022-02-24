/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class JsonStringConverterTest extends AbstractTest {

    @Test
    public void createJsonStringFromDataObjectTest() {
        try (Reader reader = new FileReader("src/test/resources/gnpy_request.json", StandardCharsets.UTF_8)) {
            assertEquals(
                "Should be a valid request",
                Files.readString(Paths.get("src/test/resources/expected_string.json")),
                new JsonStringConverter<Request>(getDataStoreContextUtil().getBindingDOMCodecServices())
                    .createJsonStringFromDataObject(
                        InstanceIdentifier.builder(Request.class).build(),
                        //gnpyRequest
                        (Request) getDataStoreContextUtil()
                            .getBindingDOMCodecServices()
                            .fromNormalizedNode(
                                YangInstanceIdentifier.of(Request.QNAME),
                                JSONDataObjectConverter
                                    .createWithDataStoreUtil(getDataStoreContextUtil())
                                    .transformIntoNormalizedNode(reader)
                                    .get())
                            .getValue(),
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02));
        } catch (IOException e) {
            fail("Cannot load path description ");
        }
    }

    @Test
    public void createDataObjectFromJsonStringTest() throws IOException {
        assertNotNull(
            "Should not be null",
            new JsonStringConverter<Request>(getDataStoreContextUtil().getBindingDOMCodecServices())
                .createDataObjectFromJsonString(
                    YangInstanceIdentifier.of(Request.QNAME),
                    Files.readString(Paths.get("src/test/resources/expected_string.json")),
                    JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02));
    }
}
