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

import io.fd.honeycomb.transportpce.device.tools.DefaultPmListFactory;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;
import io.fd.honeycomb.transportpce.test.common.DataStoreContextImpl;

import java.io.File;

import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
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
public final class PmConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PmConfiguration.class);

    private DataStoreContext dataStoreContextUtil;
    private DefaultPmListFactory defaultPmListFactory;
    private ClassLoader classLoader;

    @InjectConfig("netconf-initial-config-xml")
    public String PM_DATA_SAMPLE_OPER_XML;

    public PmConfiguration() {
        classLoader = Thread.currentThread().getContextClassLoader();
        dataStoreContextUtil = new DataStoreContextImpl();
        defaultPmListFactory = new DefaultPmListFactory();
    }

    public String getNetconfInitialPmXml() {
        return PM_DATA_SAMPLE_OPER_XML;
    }

    public CurrentPmlist getDataPm() {
        CurrentPmlist result = null;
        File pm_list_data = new File(classLoader.getResource(PM_DATA_SAMPLE_OPER_XML).getFile());
        result = defaultPmListFactory.createDefaultPmList(dataStoreContextUtil,pm_list_data);
        if (result != null) {
            LOG.info("result pm list size : {}", result.getCurrentPm().size());
        }
        return result;
    }
}
