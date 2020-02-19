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

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev170708.terminal.device.top.TerminalDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fd.honeycomb.transportpce.device.tools.DefaultOcTerminalDeviceFactory;
import io.fd.honeycomb.transportpce.device.tools.ExtractXMLTag;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;
import io.fd.honeycomb.transportpce.test.common.DataStoreContextImpl;
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
public final class OcTerminalDeviceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(OcTerminalDeviceConfiguration.class);
    private static final String OC_TERMINAL_DEVICE_XSL = "device/octerminaldeviceOperToConfig.xsl";

    private DataStoreContext dataStoreContextUtil;
    private DefaultOcTerminalDeviceFactory defaultOcTermminalDeviceFactory;
    private ClassLoader classLoader;
    private Boolean register;
    public String config_terminalDevice;
    public TerminalDevice oper_terminalDevice;

    @InjectConfig("netconf-initial-config-xml")
    public String OC_TERMINAL_DEVICE_DATA_SAMPLE_OPER_XML;

    public OcTerminalDeviceConfiguration() {
        LOG.info("initializing OcTerminalDeviceConfiguration");
        classLoader = Thread.currentThread().getContextClassLoader();
        dataStoreContextUtil = new DataStoreContextImpl();
        defaultOcTermminalDeviceFactory = new DefaultOcTerminalDeviceFactory();
        register = false;
    }

    public String getNetconfInitialOcTerminalDeviceXml() {
        return OC_TERMINAL_DEVICE_DATA_SAMPLE_OPER_XML;
    }

    public String getConfigTerminalDevice() {
        return operToConfig();
    }

    public Boolean getRegister() {
        return register;
    }

    public void setRegister(Boolean reg) {
        this.register = reg;
    }

    public TerminalDevice getDataTerminalDevice() {
        TerminalDevice result = null;
        File terminalDevice_data = new File(classLoader.getResource(OC_TERMINAL_DEVICE_DATA_SAMPLE_OPER_XML).getFile());
        result = defaultOcTermminalDeviceFactory.createDefaultTerminalDevice(dataStoreContextUtil, terminalDevice_data);
        if (result != null) {
            LOG.info("reading initial data.");
        } else {
            LOG.warn("failed to get OC Platform Data");
        }
        return result;
    }

    public TerminalDevice getTerminalDeviceFromXML(String xml) {
        String config_result = null;
        LOG.info("process to transform xml file to config data");
        TerminalDevice result = null;
        LOG.info("transforming xml string to config components ...");
        config_result = xml.toString();
        // LOG.info("config_result: {}",config_result);
        result = defaultOcTermminalDeviceFactory.createDefaultTerminalDevice(dataStoreContextUtil, config_result);
        if (result != null) {
            LOG.info("result : {}", result);
        }
        return result;
    }

    /**
     * Convert data xml to config xml terminal-device.
     *
     * @return String result
     */
    public String operToConfig() {
        String result = null;
        LOG.info("process to transform oper xml file to config xml file for terminal-device");
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(classLoader.getResource(OC_TERMINAL_DEVICE_XSL).getFile()));
        Transformer transformer;
        Source text;
        StringWriter tmpwriter = new StringWriter();
        try {
            LOG.info("transforming xml data to config terminal-device ...");
            transformer = factory.newTransformer(xslt);
            String extract_data = ExtractXMLTag.extractTagElement(OC_TERMINAL_DEVICE_DATA_SAMPLE_OPER_XML,
                    "terminal-device", "http://openconfig.net/yang/terminal-device");
            text = new StreamSource(new StringReader(extract_data));
            LOG.info("text avant transform = {}", text.toString());
            transformer.transform(text, new StreamResult(tmpwriter));
            result = tmpwriter.toString();
            LOG.info(result);
        } catch (TransformerException e) {
            LOG.error("Transformer failed ");
        }
        return result;
    }

}
