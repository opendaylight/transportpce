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

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev180130.platform.component.top.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fd.honeycomb.transportpce.device.tools.DefaultOcPlatformFactory;
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
public final class OcPlatformConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(OcPlatformConfiguration.class);
    private static final String OC_PLATFORM_XSL = "device/ocplatformOperToConfig.xsl";

    private DataStoreContext dataStoreContextUtil;
    private DefaultOcPlatformFactory defaultOcPlatformFactory;
    private ClassLoader classLoader;
    private Boolean register;
    public String config_components;
    public Components oper_components;

    @InjectConfig("netconf-initial-config-xml")
    public String OC_PLATFORM_DATA_SAMPLE_OPER_XML;

    public OcPlatformConfiguration() {
        LOG.info("initializing OcPlatformConfiguration");
        classLoader = Thread.currentThread().getContextClassLoader();
        dataStoreContextUtil = new DataStoreContextImpl();
        defaultOcPlatformFactory = new DefaultOcPlatformFactory();
        register = false;
    }

    public String getNetconfInitialOcPlatformXml() {
        return OC_PLATFORM_DATA_SAMPLE_OPER_XML;
    }

    public String getConfigComponents() {
        return operToConfig();
    }

    public Boolean getRegister() {
        return register;
    }

    public void setRegister(Boolean reg) {
        this.register = reg;
    }

    public Components getDataComponents() {
        Components result = null;
        File components_data = new File(classLoader.getResource(OC_PLATFORM_DATA_SAMPLE_OPER_XML).getFile());
        result = defaultOcPlatformFactory.createDefaultComponents(dataStoreContextUtil, components_data);
        if (result != null) {
            LOG.info("reading initial data.");
        } else {
            LOG.warn("failed to get OC Platform Data");
        }
        return result;
    }

    public Components getComponentsFromXML(String xml) {
        String config_result = null;
        LOG.info("process to transform xml file to config data");
        LOG.info("xml={}", xml);
        Components result = null;
        LOG.info("transforming xml string to config components ...");
        config_result = xml.toString();
        // LOG.info("config_result: {}",config_result);
        result = defaultOcPlatformFactory.createDefaultComponents(dataStoreContextUtil, config_result);
        if (result != null) {
            LOG.info("result : {}", result);
        }
        return result;
    }

    /**
     * Convert data xml to config xml device.
     *
     * @return String result
     */
    public String operToConfig() {
        String result = null;
        LOG.info("process to transform oper xml file to config xml file ");
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(new File(classLoader.getResource(OC_PLATFORM_XSL).getFile()));
        Transformer transformer;
        Source text;
        StringWriter tmpwriter = new StringWriter();
        try {
            LOG.info("transforming xml data to config components ...");
            transformer = factory.newTransformer(xslt);
            String extract_data = ExtractXMLTag.extractTagElement(OC_PLATFORM_DATA_SAMPLE_OPER_XML, "components",
                    "http://openconfig.net/yang/platform");
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
