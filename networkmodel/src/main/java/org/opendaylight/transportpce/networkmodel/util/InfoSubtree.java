/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.Optional;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfoSubtree {

    private static final Logger LOG = LoggerFactory.getLogger(InfoSubtree.class);

    String clli;
    String vendor;
    String model;
    IpAddress ipAddress;
    int nodeType;
    String openRoadmVersion;

    public InfoSubtree(String openRoadmVersion) {

        this.clli = new String();
        this.vendor = new String();
        this.model = new String();
        this.ipAddress = null;
        this.nodeType = 0;
        this.openRoadmVersion = openRoadmVersion;


    }

    public boolean getDeviceInfo(String nodeId, DeviceTransactionManager deviceTransactionManager) {
        switch (this.openRoadmVersion) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return getDeviceInfo121(nodeId, deviceTransactionManager);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return getDeviceInfo221(nodeId, deviceTransactionManager);
            default:
                LOG.info("Device version {} not supported",this.openRoadmVersion);
                return false;
        }

    }

    private boolean getDeviceInfo121(String nodeId, DeviceTransactionManager deviceTransactionManager) {

        //Read clli from the device
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOpt =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, infoIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        Info deviceInfo = null;

        if (deviceInfoOpt.isPresent()) {
            deviceInfo = deviceInfoOpt.get();
        } else {
            LOG.error("Unable to get device info from device {}!", nodeId);
            return false;

        }

        this.clli = deviceInfo.getClli();
        this.vendor = deviceInfo.getVendor();
        this.model = deviceInfo.getModel();
        this.ipAddress = deviceInfo.getIpAddress();
        this.nodeType = deviceInfo.getNodeType().getIntValue();

        return true;

    }

    private boolean getDeviceInfo221(String nodeId, DeviceTransactionManager deviceTransactionManager) {

        //TODO : change back to operational when testing on real device
        //Read clli from the device
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm
                .device.container.org.openroadm.device.Info> infoIID = InstanceIdentifier.create(org
                .opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                .container.OrgOpenroadmDevice.class).child(org.opendaylight.yang.gen.v1.http.org
                .openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Info.class);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                .container.org.openroadm.device.Info> deviceInfoOpt =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, infoIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
                .org.openroadm.device.Info deviceInfo = null;

        if (deviceInfoOpt.isPresent()) {
            deviceInfo = deviceInfoOpt.get();
        } else {
            LOG.error("Unable to get device info from device {}!", nodeId);
            return false;

        }

        this.clli = deviceInfo.getClli();
        //this.vendor = deviceInfo.getVendor();
        //this.model = deviceInfo.getModel();
        this.ipAddress = deviceInfo.getIpAddress();
        this.nodeType = deviceInfo.getNodeType().getIntValue();
        return true;
    }

    public String getClli() {
        return clli;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public int getNodeType() {
        return nodeType;
    }
}


