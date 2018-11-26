/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.util.List;
import java.util.Optional;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceConfigImpl implements DeviceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceConfigImpl.class);

    private final DeviceTransactionManager deviceTransactionManager;

    public DeviceConfigImpl(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    @Override
    public Info getDeviceInfo(String nodeId) {
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOptional = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, infoIID,
            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (deviceInfoOptional.isPresent()) {
            return deviceInfoOptional.get();
        } else {
            LOG.warn("Device info subtree is absent for {}", nodeId);
            return null;
        }

    }

    @Override
    public Ports getDevicePorts(String nodeId, String circuitPackName, String portName) {
        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
            .child(Ports.class, new PortsKey(portName));
        LOG.info("{}: Fetching Port {} at circuit pack {}", nodeId, portName, circuitPackName);
        Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, portIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (portObject.isPresent()) {
            return portObject.get();
        } else {
            return null;
        }
    }

    @Override
    public SharedRiskGroup getDeviceSrg(String nodeId, int srgNb) {
        InstanceIdentifier<SharedRiskGroup> srgIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            SharedRiskGroup.class, new SharedRiskGroupKey(srgNb));
        Optional<SharedRiskGroup> srgObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, srgIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (srgObject.isPresent()) {
            return srgObject.get();
        } else {
            return null;
        }
    }

    @Override
    public CircuitPack getDeviceCp(String nodeId, String cpName) {
        InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(cpName));
        Optional<CircuitPacks> cpObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, cpIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (cpObject.isPresent()) {
            return cpObject.get();
        } else {
            return null;
        }
    }

    @Override
    public List<ConnectionMap> getConnectionMap(String NodeId) {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = this.deviceTransactionManager.getDataFromDevice(NodeId,
            LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (deviceObject.isPresent()) {
            return deviceObject.get().getConnectionMap();
        } else {
            return null;
        }
    }

    @Override
    public Degree getDeviceDegree(String nodeId, int degreeNb) {
        InstanceIdentifier<Degree> degreeIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            Degree.class, new DegreeKey(degreeNb));
        Optional<Degree> degreeObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, degreeIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (degreeObject.isPresent()) {
            return degreeObject.get();
        } else {
            return null;
        }
    }
}
