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
package io.fd.honeycomb.transportpce.device.read;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev180130.platform.component.top.Components;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev180130.platform.component.top.ComponentsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import io.fd.honeycomb.transportpce.device.configuration.OcPlatformConfiguration;
import io.fd.honeycomb.transportpce.device.configuration.PmConfiguration;

/**
 * @authors Gilles THOUENON and Christophe BETOULE ( gilles.thouenon@orange.com, christophe.betoule@orange.com )
 */
public class OcPlatformReaderFactory implements ReaderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OcPlatformReaderFactory.class);
    public static final InstanceIdentifier<Components> COMPONENTS_ID =
            InstanceIdentifier.create(Components.class);

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Inject
    private OcPlatformConfiguration ocPlatformConfiguration;


    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        registry.add(new BindingBrokerReader<>(COMPONENTS_ID, dataBroker,LogicalDatastoreType.OPERATIONAL,
                ComponentsBuilder.class));
        if (writeXMLDataToOper()) {
            loadConfigData();
        };
    }

    /**
     * Write xml data from {@link PmConfiguration}
     * to operational data.
     *
     */
    public boolean writeXMLDataToOper() {
        Boolean res = false;
        LOG.info("writting xml oc-platform file data to oper datastore");
        Components components = this.ocPlatformConfiguration.getDataComponents();
        if (components !=null && components.getComponent().size()> 0) {
            LOG.info("Getting oc-platform info from xml file");
            ComponentsBuilder result = new ComponentsBuilder(components);
            InstanceIdentifier<Components> iid = InstanceIdentifier.create(Components.class);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null ) {
                LOG.info("WriteTransaction is ok, copy oc-platform to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, result.build());
                FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("oc-platform writed to oper datastore");
                    res = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write oc-platform to oper datastore");
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("get oc-platform data from xml file is null !");
        }
        return res;
    }

    /**
     * Load data to config oc-platform datastore.
     *
     */
    private boolean loadConfigData() {
        Boolean res = false;
        LOG.info("loading oc-platform configuration info from config-xml file...");
        String xml = this.ocPlatformConfiguration.getConfigComponents();
        LOG.info("xml de loadConfigData = {}", xml);
        LOG.info("get components data from xml file !");
        if (xml != null) {
            Components result = this.ocPlatformConfiguration.getComponentsFromXML(xml);
            if (result != null) {
                LOG.info("get components data : {}", result.getComponent().toString());
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                if (writeTx != null) {
                    LOG.info("WriteTransaction is ok, copy device info to config datastore");
                    writeTx.put(LogicalDatastoreType.CONFIGURATION, COMPONENTS_ID, result);
                    FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                    try {
                        Futures.getChecked(future, ExecutionException.class);
                        LOG.info("components writed to config datastore");
                    } catch (ExecutionException e) {
                        LOG.error("Failed to write components to config datastore");
                    }
                } else {
                    LOG.error("WriteTransaction object is null");
                }
            } else {
                LOG.error("components from xml is null !!");
            }
        } else {
            LOG.error("no components obtained from xml file");
        }
        return res;
    }

}
