/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.transportpce.device.configuration;

import io.fd.honeycomb.transportpce.device.tools.DefaultDeviceFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

/**
 * Class containing static configuration for honeynode-plugin module<br>
 * <p/>
 * Further documentation for the configuration injection can be found at:
 * https://github.com/yyvess/gconf
 */
@BindConfig(value = "honeycomb", syntax = Syntax.JSON)
public final class DeviceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceConfiguration.class);
    private static final String DEVICE_XSL = "device/OperToConfig.xsl";
    private static final String CONFIG_XSL = "device/config.xsl";
    private static final String NAMESPACE_TRIMER_XSL = "device/NamespaceTrimmer.xslt";
    //private static final String DEVICE_DATA_SAMPLE_OPER_XML = "device/sample-config-ROADM.xml";

    public String config_device;
    public OrgOpenroadmDevice oper_device;
    private DataStoreContext dataStoreContextUtil;
    private DefaultDeviceFactory defaultDeviceFactory;
    private ClassLoader classLoader;
    private Boolean register;

    @InjectConfig("persisted-config-path")
    public String peristConfigPath;

    @InjectConfig("netconf-initial-config-xml")
    public String DEVICE_DATA_SAMPLE_OPER_XML;

    public DeviceConfiguration() {
        classLoader = Thread.currentThread().getContextClassLoader();
        dataStoreContextUtil = new DataStoreContextImpl();
        defaultDeviceFactory = new DefaultDeviceFactory();
        register = false;
    }


    public String getConfigDevice() {
        return operToConfig();
    }

    public String getPeristConfigPath() {
        return peristConfigPath;
    }

    public String getNetconfInitialConfigXml() {
        return DEVICE_DATA_SAMPLE_OPER_XML;
    }

    public Boolean getRegister() {
        return register;
    }

    public void setRegister(Boolean reg) {
        this.register = reg;
    }

    public OrgOpenroadmDevice getDataDevice() {
        OrgOpenroadmDevice result = null;
        File device_data = new File(classLoader.getResource(DEVICE_DATA_SAMPLE_OPER_XML).getFile());
        result = defaultDeviceFactory.createDefaultDevice(dataStoreContextUtil,device_data);
        if (result != null) {
            LOG.info("result info : {}", result.getInfo().getNodeId());
        }
        return result;
    }

    public OrgOpenroadmDevice getDeviceFromXML(String xml) {
        String config_result =null;
        LOG.info("process to transform xml file to config data");
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(classLoader.getResource(CONFIG_XSL).getFile()));
        Transformer transformer;
        Source text;
        StringWriter device_config = new StringWriter();
        OrgOpenroadmDevice result = null;
        try {
            LOG.info("transforming xml string to config device ...");
            transformer = factory.newTransformer(xslt);
            text = new StreamSource(new StringReader(xml));
            transformer.transform(text, new StreamResult(device_config));
            config_result = device_config.toString();
            //LOG.info("config_result: {}",config_result);
            result  = defaultDeviceFactory.createDefaultDevice(dataStoreContextUtil,config_result);
            if (result != null) {
                LOG.info("result info : {}", result.getInfo().getNodeId());
            }
        } catch (TransformerException e) {
            LOG.error("Transformer failed ");
        }

        return result;
    }


   /**
    * Convert data xml to config xml device.
    * @return String result
    */
    public String operToConfig() {
        String result =null;
        LOG.info("process to transform xml file ");
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(classLoader.getResource(DEVICE_XSL).getFile()));
        Transformer transformer;
        Source text;
        StringWriter tmpwriter = new StringWriter();
        StringReader reader;
        try {
            LOG.info("transforming xml data to config device ...");
            transformer = factory.newTransformer(xslt);
            text = new StreamSource(new File(classLoader.getResource(DEVICE_DATA_SAMPLE_OPER_XML).getFile()));
            transformer.transform(text, new StreamResult(tmpwriter));
            LOG.info("removing namespace ...");
            xslt = new StreamSource(new File(classLoader.getResource(NAMESPACE_TRIMER_XSL).getFile()));
            transformer = factory.newTransformer(xslt);
            reader = new StringReader(tmpwriter.toString());
            StringWriter device_config = new StringWriter();
            text = new StreamSource(reader);
            transformer.transform(text, new StreamResult(device_config));
            result = device_config.toString();
//            LOG.info("DeviceConfiguration - operToCconfig - result = {}", result);
        } catch (TransformerException e) {
            LOG.error("Transformer failed ");
        }
        return result;
    }
}
