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
import io.fd.honeycomb.transportpce.device.tools.DefaultPmListFactory;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;
import io.fd.honeycomb.transportpce.test.common.DataStoreContextImpl;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts XML Data operation file  to Configuration file.
 *
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
public class DeviceOperToConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceOperToConfig.class);
    private static final String DEVICE_OPER = "src/test/resources/oper-ROADMA.xml";
    private static final String PM_LIST_OPER = "src/test/resources/oper-ROADMA-cpm.xml";
    private static final String CONFIG_XSL = "src/main/resources/honeycomb-minimal-resources/config/device/config.xsl";
    private static final String DEVICE_XSL = "src/main/resources/honeycomb-minimal-resources/config/device/OperToConfig.xsl";



    /**
     * Convert data xml to config xml device.
     *
     * @return String result
     */
    public static String operToConfig() {
        String result =null;
        LOG.info("process to transform xml file {}",DEVICE_OPER);
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(DEVICE_XSL));
        try {
           StringWriter tmpwriter = new StringWriter();
            LOG.info("transforming xml data to config device ...");
            Transformer transformer = factory.newTransformer(xslt);
            Source text = new StreamSource(new File(DEVICE_OPER));
            transformer.transform(text, new StreamResult(tmpwriter));
            result = tmpwriter.toString();
        } catch (TransformerException e) {
            LOG.error("Transformer failed ", e);
        }
        return result;
    }

    public static String getDeviceFromXML(String xml) {
        String config_result =null;
        LOG.info("process to transform xml file to config data");
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(CONFIG_XSL));
        Transformer transformer;
        Source text;
        StringWriter device_config = new StringWriter();
        try {
            LOG.info("transforming xml string to config device ...");
            transformer = factory.newTransformer(xslt);
            text = new StreamSource(new StringReader(xml));
            transformer.transform(text, new StreamResult(device_config));
            config_result = device_config.toString();
            LOG.info(config_result);
        } catch (TransformerException e) {
            LOG.error("Transformer failed ");
        }
        return config_result;
    }

    public static void createDeviceFromString(String xml) throws NullPointerException {
        OrgOpenroadmDevice result = null;
        LOG.info("Parameterized string is : {}", xml);
        DataStoreContext dataStoreContextUtil = new DataStoreContextImpl();
        DefaultDeviceFactory defaultDeviceFactory = new DefaultDeviceFactory();
        result = defaultDeviceFactory.createDefaultDevice(dataStoreContextUtil,xml);
        if (result != null) {
            LOG.info("result info : {}", result.getInfo().getNodeId());
        } else {
            LOG.error("failed !");
        }

    }

    public static void createPmListFromFile() throws NullPointerException {
        CurrentPmList result = null;
        DataStoreContext dataStoreContextUtil = new DataStoreContextImpl();
        DefaultPmListFactory defaultDeviceFactory = new DefaultPmListFactory();
        result = defaultDeviceFactory.createDefaultPmList(dataStoreContextUtil,new File(PM_LIST_OPER));
        if (result != null) {
            LOG.info("result pm list size : {}", result.getCurrentPmEntry().size());
        } else {
            LOG.error("failed !");
        }

    }

    public static void main(String[] args) {
        DeviceOperToConfig.createPmListFromFile();

    }
}
