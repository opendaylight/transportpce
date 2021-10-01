/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev171215.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev171215.States;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.loopback.rev171215.maint.loopback.MaintLoopbackBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.testsignal.rev171215.maint.testsignal.MaintTestsignalBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.otu.container.OtuBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfacesImpl221 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfacesImpl221.class);

    private final DeviceTransactionManager deviceTransactionManager;
    private final PortMapping portMapping;
    private final PortMappingVersion221 portMapping221;

    public OpenRoadmInterfacesImpl221(DeviceTransactionManager deviceTransactionManager,
            PortMapping portMapping, PortMappingVersion221 portMapping221) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.portMapping = portMapping;
        this.portMapping221 = portMapping221;
    }

    public void postInterface(String nodeId, InterfaceBuilder ifBuilder) throws OpenRoadmInterfaceException {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(nodeId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                    nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                nodeId), e);
        }

        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            Interface.class, new InterfaceKey(ifBuilder.getName()));
        LOG.info("POST INTERF for {} : InterfaceBuilder : name = {} \t type = {}", nodeId, ifBuilder.getName(),
            ifBuilder.getType().toString());
        deviceTx.merge(LogicalDatastoreType.CONFIGURATION, interfacesIID, ifBuilder.build());
        FluentFuture<? extends @NonNull CommitInfo> txSubmitFuture =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        // TODO: instead of using this infinite loop coupled with this timeout,
        // it would be better to use a notification mechanism from the device to be advertised
        // that the new created interface is present in the device circuit-pack/port
        final Thread current = Thread.currentThread();
        Thread timer = new Thread() {
            public void run() {
                try {
                    Thread.sleep(3000);
                    current.interrupt();
                } catch (InterruptedException e) {
                    LOG.error("Timeout before the new created interface appears on the deivce circuit-pack port", e);
                }
            }
        };
        try {
            txSubmitFuture.get();
            LOG.info("Successfully posted interface {} on node {}", ifBuilder.getName(), nodeId);
            boolean devicePortIsUptodated = false;
            while (!devicePortIsUptodated) {
                devicePortIsUptodated = checkIfDevicePortIsUpdatedWithInterface(nodeId, ifBuilder);
            }
            LOG.info("{} - {} - interface {} updated on port {}", nodeId, ifBuilder.getSupportingCircuitPackName(),
                ifBuilder.getName(), ifBuilder.getSupportingPort());
            timer.interrupt();
        } catch (InterruptedException | ExecutionException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to post interface %s on node %s!", ifBuilder
                .getName(), nodeId), e);
        }
    }


    public Optional<Interface> getInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey(interfaceName));
        return deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION,
            interfacesIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }


    public synchronized void deleteInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        LOG.info("deleting interface {} on device221 {}", interfaceName, nodeId);
        Optional<Interface> intf2DeleteOpt;
        try {
            intf2DeleteOpt = getInterface(nodeId, interfaceName);
        } catch (OpenRoadmInterfaceException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to check if interface %s exists on node %s!",
                interfaceName, nodeId), e);
        }
        if (intf2DeleteOpt.isPresent()) {
            Interface intf2Delete = intf2DeleteOpt.get();
            // State admin state to out of service
            InterfaceBuilder ifBuilder = new InterfaceBuilder(intf2Delete);
            if (ifBuilder.getType() == OtnOdu.class) {
                Interface1Builder oduBuilder = new Interface1Builder(intf2Delete.augmentation(Interface1.class));
                OduBuilder odu = new OduBuilder(oduBuilder.getOdu());
                if (odu.getMaintTestsignal() != null) {
                    MaintTestsignalBuilder maintSignalBuilder = new MaintTestsignalBuilder();
                    maintSignalBuilder.setEnabled(false);
                    odu.setMaintTestsignal(maintSignalBuilder.build());
                }
                oduBuilder.setOdu(odu.build());
                ifBuilder.addAugmentation(oduBuilder.build());
            } else if (ifBuilder.getType() == OtnOtu.class) {
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder
                    otuBuilder =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder(
                        intf2Delete.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1
                            .class));
                OtuBuilder otu = new OtuBuilder(otuBuilder.getOtu());
                if (otu.getMaintLoopback() != null) {
                    MaintLoopbackBuilder maintLoopBackBuilder = new MaintLoopbackBuilder();
                    maintLoopBackBuilder.setEnabled(false);
                    otu.setMaintLoopback(maintLoopBackBuilder.build());
                }
                otuBuilder.setOtu(otu.build());
                ifBuilder.addAugmentation(otuBuilder.build());
            }
            ifBuilder.setAdministrativeState(AdminStates.OutOfService);
            // post interface with updated admin state
            try {
                postInterface(nodeId, ifBuilder);
            } catch (OpenRoadmInterfaceException ex) {
                throw new OpenRoadmInterfaceException(String.format("Failed to set state of interface %s to %s while"
                    + " deleting it!", interfaceName, AdminStates.OutOfService), ex);
            }

            InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                Interface.class, new InterfaceKey(interfaceName));
            Future<Optional<DeviceTransaction>> deviceTxFuture = this.deviceTransactionManager.getDeviceTransaction(
                nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.get();
                } else {
                    throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                        nodeId));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                    nodeId), e);
            }

            deviceTx.delete(LogicalDatastoreType.CONFIGURATION, interfacesIID);
            FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);

            try {
                commit.get();
                LOG.info("Successfully deleted {} on node {}", interfaceName, nodeId);
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to delete interface %s on " + "node %s",
                    interfaceName, nodeId), e);
            }
            // change the equipment state on circuit pack if xpdr node
            if (intf2Delete.getName().contains(StringConstants.CLIENT_TOKEN) || intf2Delete.getName().contains(
                StringConstants.NETWORK_TOKEN)) {
                postEquipmentState(nodeId, intf2Delete.getSupportingCircuitPackName(), false);
                Mapping oldmapping = this.portMapping.getMapping(nodeId, intf2Delete.getSupportingCircuitPackName(),
                    intf2Delete.getSupportingPort());
                this.portMapping.deleteMapping(nodeId, oldmapping.getLogicalConnectionPoint());
                this.portMapping221.updateMapping(nodeId, oldmapping);
            }

        } else {
            LOG.info("Interface does not exist, cannot delete on node {}", nodeId);
        }
    }

    public void postEquipmentState(String nodeId, String circuitPackName, boolean activate)
        throws OpenRoadmInterfaceException {
        InstanceIdentifier<CircuitPacks> circuitPackIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            CircuitPacks.class, new CircuitPacksKey(circuitPackName));
        Optional<CircuitPacks> cpOpt = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, circuitPackIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        CircuitPacks cp = null;
        if (cpOpt.isPresent()) {
            cp = cpOpt.get();
        } else {
            throw new OpenRoadmInterfaceException(String.format(
                "Could not find CircuitPack %s in equipment config datastore for node %s", circuitPackName, nodeId));
        }
        CircuitPacksBuilder cpBldr = new CircuitPacksBuilder(cp);
        boolean change = false;
        if (activate) {
            if (cpBldr.getEquipmentState() != null
                    && !States.NotReservedInuse.equals(cpBldr.getEquipmentState())) {
                cpBldr.setEquipmentState(States.NotReservedInuse);
                change = true;
            }
        } else if ((cpBldr.getEquipmentState() != null
                && !States.NotReservedAvailable.equals(cpBldr.getEquipmentState()))) {
            cpBldr.setEquipmentState(States.NotReservedAvailable);
            change = true;
        }
        if (change) {
            Future<Optional<DeviceTransaction>> deviceTxFuture = this.deviceTransactionManager.getDeviceTransaction(
                nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.get();
                } else {
                    throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                        nodeId));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                    nodeId), e);
            }
            deviceTx.merge(LogicalDatastoreType.CONFIGURATION, circuitPackIID, cpBldr.build());
            FluentFuture<? extends @NonNull CommitInfo> txSubmitFuture =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            try {
                txSubmitFuture.get();
                LOG.info("Successfully posted equipment state change on node {}", nodeId);
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to post equipment state on node %s!",
                    nodeId), e);
            }
        }
    }

    public String getSupportedInterface(String nodeId, String interf) {
        Optional<Interface> supInterfOpt;
        try {
            supInterfOpt = getInterface(nodeId, interf);
            if (supInterfOpt.isPresent()) {
                return supInterfOpt.get().getSupportingInterface();
            } else {
                return null;
            }
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("error getting Supported Interface of {} - {}", interf, nodeId, e);
            return null;
        }
    }

    private boolean checkIfDevicePortIsUpdatedWithInterface(String nodeId, InterfaceBuilder ifBuilder) {
        KeyedInstanceIdentifier<Ports, PortsKey> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(ifBuilder.getSupportingCircuitPackName()))
            .child(Ports.class, new PortsKey(ifBuilder.getSupportingPort()));
        Ports port = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
            portIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT).get();
        if (port.getInterfaces() == null) {
            return false;
        }
        for (Interfaces interf : port.getInterfaces()) {
            if (interf.getInterfaceName().equals(ifBuilder.getName())) {
                return true;
            }
        }
        return false;
    }
}
