/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opendaylight.transportpce.test.converter.util.ConverterTestUtil;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.skyscreamer.jsonassert.JSONAssert;

@TestInstance(Lifecycle.PER_CLASS)
class JsonDataConverterTest {

    private OrgOpenroadmDevice device;
    private Context context;

    @BeforeAll
    void setup() {
        this.device = ConverterTestUtil.buildDevice();
        this.context = ConverterTestUtil.buildContext();

    }

   // @Test
    void serializeOrgOpenroadmDeviceTest() {
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            assertEquals(
                    Files.readString(Path.of("src/test/resources/device.json")),
                    converter.serialize(
                            DataObjectIdentifier
                                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                                .build(),
                            device),
                    "OrgOpenroadmDevice should be   as in the device.json file");
        } catch (IOException e1) {
            fail("Cannot load json file with expected result");
        }
    }


    // @Test
    void serializeOrgOpenroadmDeviceToFileTest() {
        final Path filePath = Path.of("testSerializeToJSONFile.json");
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            converter.serializeToFile(
                    DataObjectIdentifier.builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                        .build(),
                    device,
                    filePath);
            assertTrue(Files.exists(filePath));
        } catch (ProcessingException e) {
            fail("Cannot serialise object to json file");
        } finally {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                fail("Failed to delete the test file: " + e.getMessage());
            }
        }
    }

    @Test
    void deserializeJsonToOrgOpenroadmDeviceTest() {
        JsonDataConverter converter = new JsonDataConverter(ModelsUtils.OPENROADM_MODEL_PATHS_71);
        try {
            OrgOpenroadmDevice deserializedDevice = (OrgOpenroadmDevice) converter.deserialize(
                    Files.readString(Path.of("src/test/resources/device.json")), OrgOpenroadmDevice.QNAME);
            assertEquals(this.device, deserializedDevice);
        } catch (ProcessingException e) {
            fail("Error deserializing json to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load json file with input json data");
        }
    }

    @Test
    void deserializeJsonReaderToOrgOpenroadmDeviceTest() {
        JsonDataConverter converter = new JsonDataConverter(ModelsUtils.OPENROADM_MODEL_PATHS_71);
        try {
            OrgOpenroadmDevice deserializedDevice = (OrgOpenroadmDevice) converter.deserialize(
                    Files.newBufferedReader(Path.of("src/test/resources/device.json"), StandardCharsets.UTF_8),
                    OrgOpenroadmDevice.QNAME);
            assertEquals(this.device, deserializedDevice);
        } catch (ProcessingException e) {
            fail("Error deserializing json to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load json file with input json data");
        }
    }

    @Test
    void serializeContextTest() throws JSONException {
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            String expectedJson = Files.readString(Path.of("src/test/resources/context.json"));
            // Use of JSONAssert here because we have a list with several items whose order is not under control. So we
            // check that all items are present regardless of their order in the list.
            JSONAssert.assertEquals(
                    new ObjectMapper().readValue(expectedJson, JsonNode.class).toString(),
                    converter.serialize(DataObjectIdentifier.builder(Context.class).build(), context),
                    false);
        } catch (IOException e1) {
            fail("Cannot load json file with expected result");
        }
    }

    @Test
    void serializeContextToFileTest() {
        final Path filePath = Path.of("testSerializeContextToJSONFile.json");
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            converter.serializeToFile(DataObjectIdentifier.builder(Context.class).build(), context, filePath);
            assertTrue(Files.exists(filePath));
        } catch (ProcessingException e) {
            fail("Cannot serialise object to json file");
        } finally {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                fail("Failed to delete the test file: " + e.getMessage());
            }
        }
    }

    @Test
    void deserializeJsonToContextTest() {
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            Context deserializedContext = (Context) converter.deserialize(
                    Files.readString(Path.of("src/test/resources/context.json")), Context.QNAME);
            assertEquals(this.context, deserializedContext);
        } catch (ProcessingException e) {
            fail("Error deserializing json to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load json file with input json data");
        }
    }

    @Test
    void deserializeJsonReaderToContextTest() {
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            Context deserializedContext = (Context) converter.deserialize(
                    Files.newBufferedReader(Path.of("src/test/resources/context.json"), StandardCharsets.UTF_8),
                    Context.QNAME);
            assertEquals(this.context, deserializedContext);
        } catch (ProcessingException e) {
            fail("Error deserializing json to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load json file with input json data");
        }
    }
}
