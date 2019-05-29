/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.test;

import io.fd.honeycomb.transportpce.device.tools.DefaultDeviceFactory;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;
import io.fd.honeycomb.transportpce.test.common.DataStoreContextImpl;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to create {@link OrgOpenroadmDevice} object
 * from xml file or String using Yangtools
 *
 * @author Martial COULIBALY ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
@RunWith(Parameterized.class)
public class CreateDeviceFromXMLFileTest {
    private static final Logger LOG = LoggerFactory.getLogger(CreateDeviceFromXMLFileTest.class);
    private static final String DEVICE_OPER_IN = "src/main/resources/honeycomb-minimal-resources/config/device/sample-config-ROADM.xml";
    private String xml;


    public CreateDeviceFromXMLFileTest(String xmlData) {
        this.xml = xmlData;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> stringXML() {
        return Arrays.asList(new Object[][]{
            {DeviceOperToConfig.getDeviceFromXML(DeviceOperToConfig.operToConfig())}
//                {DeviceOperToConfig.operToConfig()}

        });
    }

    /**
     * This test  create instance of
     * {@link OrgOpenroadmDevice} with String xml
     * from {@link DeviceOperToConfig#operToConfig()}
     * function as parameters.
     *
     * @throws NullPointerException
     */
    @Test
    public void createDeviceFromString() throws NullPointerException {
        OrgOpenroadmDevice result = null;
        LOG.info("Parameterized string is : {}", xml);
        DataStoreContext dataStoreContextUtil = new DataStoreContextImpl();
        DefaultDeviceFactory defaultDeviceFactory = new DefaultDeviceFactory();
        result = defaultDeviceFactory.createDefaultDevice(dataStoreContextUtil,xml);
        if (result != null) {
            LOG.info("result info : {}", result.getInfo().getNodeId());
            Assert.assertEquals("ROADMA", result.getInfo().getNodeId());
        }
        LOG.info("Test Succeed");
    }


    /**
     * This test create Device from File
     * located at {@link CreateDeviceFromXMLFileTest#DEVICE_OPER_IN}.
     *
     * @throws NullPointerException
     */
    @Test
    public void createDeviceFromFile() throws NullPointerException{
        LOG.info("test createDeviceFromFile ...");
        OrgOpenroadmDevice result = null;
        result = null;
        File device_data = new File(DEVICE_OPER_IN);
        DataStoreContext dataStoreContextUtil = new DataStoreContextImpl();
        DefaultDeviceFactory defaultDeviceFactory = new DefaultDeviceFactory();
        result = defaultDeviceFactory.createDefaultDevice(dataStoreContextUtil,device_data);
        if (result != null) {
            LOG.info("result info : {}", result.getInfo().getNodeId());
            Assert.assertEquals("ROADMA", result.getInfo().getNodeId());
        }
        LOG.info("Test Succeed");
    }
}