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

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import io.fd.honeycomb.transportpce.device.DeviceConfiguration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDeviceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martial COULIBALY ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public final class DeviceReaderFactory implements ReaderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceReaderFactory.class);
    public static final InstanceIdentifier<OrgOpenroadmDevice> DEVICE_CONTAINER_ID =
            InstanceIdentifier.create(OrgOpenroadmDevice.class);

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Inject
    private DeviceConfiguration deviceConfiguration;


    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        registry.add(new BindingBrokerReader<>(DEVICE_CONTAINER_ID, dataBroker,LogicalDatastoreType.OPERATIONAL,
                OrgOpenroadmDeviceBuilder.class));
        if(writeXMLDataToOper()) {
            loadConfigData();
        }
    }

    /**
     * Write xml data from {@link DeviceConfiguration}
     * to operational data.
     *
     */
    public boolean writeXMLDataToOper() {
        Boolean res = false;
        LOG.info("writting xml file data to oper datastore");
        OrgOpenroadmDevice device = this.deviceConfiguration.getDataDevice();
        if (device !=null) {
            String deviceId = device.getInfo().getNodeId();
            LOG.info("Getting device info from xml file for device '{}'", deviceId);
            OrgOpenroadmDeviceBuilder result  = new OrgOpenroadmDeviceBuilder(device);
            InstanceIdentifier<OrgOpenroadmDevice> iid = InstanceIdentifier.create(OrgOpenroadmDevice.class);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null ) {
                LOG.info("WriteTransaction is ok, copy device info to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, result.build());
                Future<Void> future = writeTx.submit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("device '{}' writed to oper datastore", deviceId);
                    res = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write Element '{}' to oper datastore", deviceId);
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("device data operation gets from xml file is null !");
        }
        return res;
    }

    /**
     * Load data to config
     * device datastore.
     *
     */
    public boolean loadConfigData() {
        Boolean res = false;
        LOG.info("loading device configuration info from xml file...");
        String xml = this.deviceConfiguration.getConfigDevice();
        LOG.info("device info gets from xml file !");
        if (xml != null) {
            OrgOpenroadmDevice result  = this.deviceConfiguration.getDeviceFromXML(xml);
            if (result != null) {
                LOG.info("OrgOpenroadmDevice info gets : {}", result.getInfo().getNodeId());
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                if (writeTx != null ) {
                    LOG.info("WriteTransaction is ok, copy device info to config datastore");
                    writeTx.put(LogicalDatastoreType.CONFIGURATION, DEVICE_CONTAINER_ID, result);
                    Future<Void> future = writeTx.submit();
                    try {
                        Futures.getChecked(future, ExecutionException.class);
                        LOG.info("device writed to config datastore");
                      } catch (ExecutionException e) {
                          LOG.error("Failed to write device to config datastore");
                      }
                } else {
                    LOG.error("WriteTransaction object is null");
                }
            } else {
                LOG.error("device gets from xml is null !!");
            }
        } else {
            LOG.error("device ID from input is not the same from xml file");
        }
        return res;
    }
}
