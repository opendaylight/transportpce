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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev170708.terminal.device.top.TerminalDevice;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev170708.terminal.device.top.TerminalDeviceBuilder;
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
import io.fd.honeycomb.transportpce.device.configuration.OcTerminalDeviceConfiguration;

/**
 * @authors Gilles THOUENON and Christophe BETOULE ( gilles.thouenon@orange.com, christophe.betoule@orange.com )
 */
public class OcTerminalDeviceReaderFactory implements ReaderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OcTerminalDeviceReaderFactory.class);
    public static final InstanceIdentifier<TerminalDevice> TERMINAL_DEVICE_ID =
            InstanceIdentifier.create(TerminalDevice.class);

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Inject
    private OcTerminalDeviceConfiguration ocTerminalDeviceConfiguration;


    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        registry.add(new BindingBrokerReader<>(TERMINAL_DEVICE_ID, dataBroker,LogicalDatastoreType.OPERATIONAL,
                TerminalDeviceBuilder.class));
        if (writeXMLDataToOper()) {
        loadConfigData();
        };
    }

    /**
     * Write xml data from {@link terminal-device}
     * to operational data.
     *
     */
    public boolean writeXMLDataToOper() {
        Boolean res = false;
        LOG.info("writting xml oc-terminal-device file data to oper datastore");
        TerminalDevice terminalDevice = this.ocTerminalDeviceConfiguration.getDataTerminalDevice();
        if (terminalDevice !=null) {
            LOG.info("Getting oc-terminal-device info from xml file");
            TerminalDeviceBuilder result = new TerminalDeviceBuilder(terminalDevice);
            InstanceIdentifier<TerminalDevice> iid = InstanceIdentifier.create(TerminalDevice.class);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null ) {
                LOG.info("WriteTransaction is ok, copy oc-terminal-device to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, result.build());
                FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("oc-terminal-device writed to oper datastore");
                    res = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write oc-terminal-device to oper datastore");
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("get oc-terminal-device data from xml file is null !");
        }
        return res;
    }

    /**
     * Load data to config oc-terminal-device datastore.
     *
     */
    private boolean loadConfigData() {
        Boolean res = false;
        LOG.info("loading oc-terminal-device configuration info from config-xml file...");
        String xml = this.ocTerminalDeviceConfiguration.getConfigTerminalDevice();
        LOG.info("get terminal-device data from xml file !");
        if (xml != null) {
            TerminalDevice result = this.ocTerminalDeviceConfiguration.getTerminalDeviceFromXML(xml);
            if (result != null) {
                LOG.info("get terminal-device data : {}", result.toString());
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                if (writeTx != null) {
                    LOG.info("WriteTransaction is ok, copy terminal-device info to config datastore");
                    writeTx.put(LogicalDatastoreType.CONFIGURATION, TERMINAL_DEVICE_ID, result);
                    FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                    try {
                        Futures.getChecked(future, ExecutionException.class);
                        LOG.info("terminal-device writed to config datastore");
                    } catch (ExecutionException e) {
                        LOG.error("Failed to write terminal-device to config datastore");
                    }
                } else {
                    LOG.error("WriteTransaction object is null");
                }
            } else {
                LOG.error("terminal-device from xml is null !!");
            }
        } else {
            LOG.error("no terminal-device obtained from xml file");
        }
        return res;
    }

}
