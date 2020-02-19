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

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fd.honeycomb.transportpce.device.tools.DefaultNetconfFactory;
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
public final class NetconfConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfConfiguration.class);

    private DataStoreContext dataStoreContextUtil;
    private DefaultNetconfFactory defaultNetconf;
    private ClassLoader classLoader;

    @InjectConfig("netconf-initial-config-xml")
    public String NETCONF_DATA_SAMPLE_OPER_XML;

    public NetconfConfiguration() {
        classLoader = Thread.currentThread().getContextClassLoader();
        dataStoreContextUtil = new DataStoreContextImpl();
        defaultNetconf = new DefaultNetconfFactory();
    }

    public String getNetconfInitialXml() {
        return NETCONF_DATA_SAMPLE_OPER_XML;
    }

    public Streams getNetconfStreamsData() {
        Streams result = null;
        File netconf_data = new File(classLoader.getResource(NETCONF_DATA_SAMPLE_OPER_XML).getFile());
        Netconf netconf = defaultNetconf.createDefaultNetconf(dataStoreContextUtil, netconf_data);
        if (netconf != null) {
            try {
                result = netconf.getStreams();
                LOG.info("netconf streams result : {}", netconf.getStreams());
            } catch (NullPointerException e) {
                LOG.error("failed to get Netconf Streams");
            }
        } else {
            LOG.warn("failed to get Netconf Streams");
        }
        return result;
    }
}
