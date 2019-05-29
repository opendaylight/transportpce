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

import io.fd.honeycomb.transportpce.device.tools.DefaultPmListFactory;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;
import io.fd.honeycomb.transportpce.test.common.DataStoreContextImpl;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to create {@link CurrentPmlist} object
 * from xml file or String using Yangtools
 *
 * @author Martial COULIBALY ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public class CreatePmListFromXMLFileTest {
    private static final Logger LOG = LoggerFactory.getLogger(CreatePmListFromXMLFileTest.class);
    private static final String PM_LIST_OPER_IN = "src/test/resources/oper-ROADMA-cpm.xml";

    /**
     * This test create Device from File
     * located at {@link CreatePmListFromXMLFileTest#DEVICE_OPER_IN}.
     *
     * @throws NullPointerException
     */
    @Test
    public void createPmListFromFile() throws NullPointerException{
        LOG.info("test createPmListFromFile ...");
        CurrentPmList result = null;
        File pm_list_data = new File(PM_LIST_OPER_IN);
        DataStoreContext dataStoreContextUtil = new DataStoreContextImpl();
        DefaultPmListFactory defaultPmListFactory = new DefaultPmListFactory();
        result = defaultPmListFactory.createDefaultPmList(dataStoreContextUtil,pm_list_data);
        if (result != null) {
            LOG.info("result pm list size : {}", result.getCurrentPmEntry().size());
            Assert.assertEquals("result pm list size : {}", result.getCurrentPmEntry().size());
        }
        LOG.info("Test Succeed");
    }
}