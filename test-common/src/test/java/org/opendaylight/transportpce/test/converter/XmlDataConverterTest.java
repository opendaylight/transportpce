/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opendaylight.transportpce.test.converter.util.ConverterTestUtil;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(Lifecycle.PER_CLASS)
class XmlDataConverterTest {
    private static final Logger LOG = LoggerFactory.getLogger(XmlDataConverterTest.class);
    private OrgOpenroadmDevice device;
    private Context context;

    @BeforeAll
    void setup() {
        this.device = ConverterTestUtil.buildDevice();
        this.context = ConverterTestUtil.buildContext();
    }

   // @Test
    void serializeOrgOpenroadmDeviceTest() {
        XmlDataConverter converter = new XmlDataConverter(null);
        try {
            assertEquals(
                    Files.readString(Path.of("src/test/resources/device.xml")),
                    converter.serialize(
                            DataObjectIdentifier
                                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                                .build(),
                            device),
                    "OrgOpenroadmDevice should be as in the device.json file");
        } catch (IOException e1) {
            fail("Cannot load xml file with expected result");
        }
    }

   // @Test
    void serializeOrgOpenroadmDeviceToFileTest() {
        final Path filePath = Path.of("testSerializeDeviceToXmlFile.xml");
        XmlDataConverter converter = new XmlDataConverter(null);
        try {
            converter.serializeToFile(
                    DataObjectIdentifier
                        .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
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
    void deserializeXmlToOrgOpenroadmDeviceTest() {
        XmlDataConverter converter = new XmlDataConverter(ModelsUtils.OPENROADM_MODEL_PATHS_71);
        try {
            OrgOpenroadmDevice deserializedDevice = (OrgOpenroadmDevice) converter.deserialize(
                    Files.readString(Path.of("src/test/resources/device.xml")),
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
                    Files.newBufferedReader(Path.of("src/test/resources/device.xml"), StandardCharsets.UTF_8),
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
    void serializeContextTest() {
        XmlDataConverter converter = new XmlDataConverter(null);
        try {
            assertThat(converter.serialize(DataObjectIdentifier.builder(Context.class).build(), context))
                    .isEqualToIgnoringWhitespace(Files.readString(Path.of("src/test/resources/context.xml")));
        } catch (IOException e1) {
            fail("Cannot load xml file with expected result");
        }
    }

    @Test
    void serializeContextToFileTest() {
        final Path filePath = Path.of("testSerializeContextToXmlFile.xml");
        XmlDataConverter converter = new XmlDataConverter(null);
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
    void deserializeXmlToContextTest() {
        XmlDataConverter converter = new XmlDataConverter(null);
        try {
            Context deserializedContext = (Context) converter.deserialize(
                    Files.readString(Path.of("src/test/resources/context.xml")), Context.QNAME);
            LOG.info("deserializedContext = {}", deserializedContext);
            assertEquals(this.context, deserializedContext);
        } catch (ProcessingException e) {
            fail("Error deserializing xml to TAPI Context object");
        } catch (IOException e) {
            fail("Cannot load xml file with input xml data");
        }
    }

    @Test
    void deserializeXmlReaderToContextTest() {
        XmlDataConverter converter = new XmlDataConverter(null);
        try {
            Context deserializedContext = (Context) converter.deserialize(
                    Files.newBufferedReader(Path.of("src/test/resources/context.xml"), StandardCharsets.UTF_8),
                    Context.QNAME);
            LOG.info("deserializedContext = {}", deserializedContext);
            assertEquals(this.context, deserializedContext);
        } catch (ProcessingException e) {
            fail("Error deserializing xml to TAPI Context object");
        } catch (IOException e) {
            fail("Cannot load xml file with input xml data");
        }
    }
}
