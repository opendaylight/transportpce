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

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opendaylight.transportpce.test.converter.util.ConverterTestUtil;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

@TestInstance(Lifecycle.PER_CLASS)
class JsonDataConverterTest {

    private OrgOpenroadmDevice device;

    @BeforeAll
    void setup() {
        this.device = ConverterTestUtil.buildDevice();
    }

    @Test
    void serializeOrgOpenroadmDeviceTest() {
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            assertEquals(
                    Files.readString(Paths.get("src/test/resources/device.json")),
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

    @Test
    void deserializeJsonToOrgOpenroadmDeviceTest() {
        JsonDataConverter converter = new JsonDataConverter(ModelsUtils.OPENROADM_MODEL_PATHS_71);
        try {
            OrgOpenroadmDevice deserializedDevice = (OrgOpenroadmDevice) converter.deserialize(
                    Files.readString(Paths.get("src/test/resources/device.json")),
                    OrgOpenroadmDevice.QNAME);
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
                    new FileReader("src/test/resources/device.json", Charset.forName("UTF8")),
                    OrgOpenroadmDevice.QNAME);
            assertEquals(this.device, deserializedDevice);
        } catch (ProcessingException e) {
            fail("Error deserializing json to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load json file with input json data");
        }
    }
}
