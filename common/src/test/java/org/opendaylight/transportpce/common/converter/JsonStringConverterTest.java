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
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

@Ignore
public class JsonStringConverterTest extends AbstractTest {

    @Test
    public void createJsonStringFromDataObjectTest() {
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
                .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader("src/test/resources/gnpy_request.json", StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                    .transformIntoNormalizedNode(reader).get();
            GnpyApi gnpyRequest = (GnpyApi) getDataStoreContextUtil().getBindingDOMCodecServices()
                    .fromNormalizedNode(YangInstanceIdentifier.of(GnpyApi.QNAME), normalizedNode).getValue();
            JsonStringConverter<GnpyApi> gnpyJsonCOnverter = new JsonStringConverter<GnpyApi>(
                    getDataStoreContextUtil().getBindingDOMCodecServices());
            String jsonString = gnpyJsonCOnverter
                    .createJsonStringFromDataObject(InstanceIdentifier.builder(GnpyApi.class).build(),
                            gnpyRequest, JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
            assertEquals("Should be a valid request",
                    Files.readString(Paths.get("src/test/resources/expected_string.json")), jsonString);
        } catch (IOException e) {
            fail("Cannot load path description ");
        }
    }

    @Test
    public void createDataObjectFromJsonStringTest() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/expected_string.json"));
        JsonStringConverter<GnpyApi> gnpyJsonCOnverter = new JsonStringConverter<GnpyApi>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        GnpyApi request = gnpyJsonCOnverter
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(GnpyApi.QNAME), json,
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
        assertNotNull("Should not be null", request);
    }
}
