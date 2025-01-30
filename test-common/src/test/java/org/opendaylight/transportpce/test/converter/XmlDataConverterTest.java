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

import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(Lifecycle.PER_CLASS)
class XmlDataConverterTest {
    private static final Logger LOG = LoggerFactory.getLogger(XmlDataConverterTest.class);
    private OrgOpenroadmDevice device;

    @BeforeAll
    void setup() {
        this.device = ConverterTestUtil.buildDevice();
    }

    @Test
    void serializeOrgOpenroadmDeviceTest() {
        XmlDataConverter converter = new XmlDataConverter(null);
        try {
            assertEquals(
                    Files.readString(Paths.get("src/test/resources/device.xml")),
                    converter.serialize(
                            DataObjectIdentifier
                                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                                .build(),
                            device),
                    "OrgOpenroadmDevice should be as in the device.json file");
        } catch (IOException e1) {
            fail("Cannot load json file with expected result");
        }
    }

    @Test
    void serializeToFileTest() {
        final var fileName = "testSerializeToXmlFile.xml";
        XmlDataConverter converter = new XmlDataConverter(null);
        File file = new File(fileName);
        try {
            converter.serializeToFile(
                    DataObjectIdentifier
                        .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                        .build(),
                    device,
                    fileName);
            assertTrue(file.exists());
        } catch (ProcessingException e) {
            fail("Cannot serialise object to json file");
        } finally {
            file.delete();
        }
    }

    @Test
    void deserializeXmlToOrgOpenroadmDeviceTest() {
        XmlDataConverter converter = new XmlDataConverter(ModelsUtils.OPENROADM_MODEL_PATHS_71);
        try {
            OrgOpenroadmDevice deserializedDevice = (OrgOpenroadmDevice) converter.deserialize(
                    Files.readString(Paths.get("src/test/resources/device.xml")),
                    OrgOpenroadmDevice.QNAME);
            LOG.info("deserializedDevice = {}", deserializedDevice);
            assertEquals(this.device, deserializedDevice);
        } catch (ProcessingException e) {
            fail("Error deserializing xml to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load xml file with input xml data");
        }
    }

    @Test
    void deserializeXmlReaderToOrgOpenroadmDeviceTest() {
        XmlDataConverter converter = new XmlDataConverter(ModelsUtils.OPENROADM_MODEL_PATHS_71);
        try {
            OrgOpenroadmDevice deserializedDevice = (OrgOpenroadmDevice) converter.deserialize(
                    new FileReader("src/test/resources/device.xml", Charset.forName("UTF8")),
                    OrgOpenroadmDevice.QNAME);
            LOG.info("deserializedDevice = {}", deserializedDevice);
            assertEquals(this.device, deserializedDevice);
        } catch (ProcessingException e) {
            fail("Error deserializing xml to OrgOpenroadmDevice object");
        } catch (IOException e) {
            fail("Cannot load xml file with input xml data");
        }
    }

}
