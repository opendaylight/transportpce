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
import org.opendaylight.transportpce.common.device.observer.EventSubscriber;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.openroadminterfaces.message.ErrorMessage;
import org.opendaylight.transportpce.common.openroadminterfaces.message.Message;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfacesImpl221 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfacesImpl221.class);

    private final DeviceTransactionManager deviceTransactionManager;
    private final PortMapping portMapping;
    private final PortMappingVersion221 portMapping221;
    private final Message error = new ErrorMessage("2.2.1");

    public OpenRoadmInterfacesImpl221(DeviceTransactionManager deviceTransactionManager, PortMapping portMapping) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.portMapping = portMapping;
        this.portMapping221 = portMapping.getPortMappingVersion221();
    }

    public void postInterface(String nodeId, InterfaceBuilder ifBuilder) throws OpenRoadmInterfaceException {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(nodeId);
        String ifName = ifBuilder.getName();
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.orElseThrow();
            } else {
                throw new OpenRoadmInterfaceException(error.failedCreatingInterfaceNoComNoTxTrans(nodeId, ifName));
            }
        } catch (InterruptedException e) {
            throw new OpenRoadmInterfaceException(
                    error.failedCreatingInterfaceComInterruptedNoTrans(nodeId, ifName), e);
        } catch (ExecutionException e) {
            throw new OpenRoadmInterfaceException(error.failedCreatingInterfaceNoComNoTrans(nodeId, ifName), e);
        }

        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey(ifBuilder.getName()))
            .build();
        LOG.info("POST INTERF for {} : InterfaceBuilder : name = {} \t type = {}", nodeId, ifName,
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
            LOG.info("Successfully posted/deleted interface {} on node {}", ifName, nodeId);
            // this check is not needed during the delete operation
            // during the delete operation, ifBuilder does not contain supporting-cp and supporting-port
            if (ifBuilder.getSupportingCircuitPackName() != null && ifBuilder.getSupportingPort() != null) {
                boolean devicePortIsUptodated = false;
                while (!devicePortIsUptodated) {
                    devicePortIsUptodated = checkIfDevicePortIsUpdatedWithInterface(nodeId, ifBuilder);
                }
                LOG.info("{} - {} - interface {} updated on port {}", nodeId, ifBuilder.getSupportingCircuitPackName(),
                    ifBuilder.getName(), ifBuilder.getSupportingPort());
            }
            timer.interrupt();
        } catch (InterruptedException e) {
            throw new OpenRoadmInterfaceException(error.failedCreatingInterfaceComInterrupted(nodeId, ifName), e);
        } catch (ExecutionException e) {
            throw new OpenRoadmInterfaceException(error.failedCreatingInterfaceNoCom(nodeId, ifName), e);
        }
    }


    public Optional<Interface> getInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey(interfaceName))
            .build();
        return deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION,
            interfacesIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }


    public synchronized void deleteInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        LOG.info("deleting interface {} on device221 {}", interfaceName, nodeId);
        Optional<Interface> intf2DeleteOpt;
        try {
            intf2DeleteOpt = getInterface(nodeId, interfaceName);
        } catch (OpenRoadmInterfaceException e) {
            throw new OpenRoadmInterfaceException(error.failedDeleteInterfaceNotFound(nodeId, interfaceName), e);
        }
        if (intf2DeleteOpt.isPresent()) {
            Interface intf2Delete = intf2DeleteOpt.orElseThrow();
            // State admin state to out of service
            InterfaceBuilder ifBuilder = new InterfaceBuilder()
                .setAdministrativeState(AdminStates.OutOfService)
                .setName(intf2Delete.getName())
                .setType(intf2Delete.getType());
            // post interface with updated admin state
            try {
                postInterface(nodeId, ifBuilder);
            } catch (OpenRoadmInterfaceException ex) {
                throw new OpenRoadmInterfaceException(
                        error.failedDeleteInterfaceNoComNoTXTrans(nodeId, interfaceName), ex
                );
            }

            InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName))
                .build();
            Future<Optional<DeviceTransaction>> deviceTxFuture = this.deviceTransactionManager.getDeviceTransaction(
                nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.orElseThrow();
                } else {
                    throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                        nodeId));
                }
            } catch (InterruptedException e) {
                throw new OpenRoadmInterfaceException(
                        error.failedDeleteInterfaceComInterruptedNoTrans(nodeId, interfaceName), e);
            } catch (ExecutionException e) {
                throw new OpenRoadmInterfaceException(
                        error.failedDeleteInterfaceNoComNoTrans(nodeId, interfaceName), e);
            }

            deviceTx.delete(LogicalDatastoreType.CONFIGURATION, interfacesIID);
            FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);

            try {
                commit.get();
                LOG.info("Successfully deleted {} on node {}", interfaceName, nodeId);
            } catch (InterruptedException e) {
                throw new OpenRoadmInterfaceException(
                        error.failedDeleteInterfaceInterruptedCom(nodeId, interfaceName), e);
            } catch (ExecutionException e) {
                throw new OpenRoadmInterfaceException(error.failedDeleteInterfaceNoCom(nodeId, interfaceName), e);
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
        InstanceIdentifier<CircuitPacks> circuitPackIID = InstanceIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
            .build();
        Subscriber eventSubscriber = new EventSubscriber();
        Optional<CircuitPacks> cpOpt = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, circuitPackIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT, eventSubscriber, error);

        CircuitPacks cp = null;
        if (cpOpt.isPresent()) {
            cp = cpOpt.orElseThrow();
        } else {
            throw new OpenRoadmInterfaceException(error.circuitPackNotFound(nodeId, circuitPackName));
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
                    deviceTx = deviceTxOpt.orElseThrow();
                } else {
                    throw new OpenRoadmInterfaceException(error.failedWritingEquipmentStateNoComNoTxTrans(nodeId));
                }
            } catch (InterruptedException e) {
                throw new OpenRoadmInterfaceException(
                        error.failedWritingEquipmentStateComInterruptedNoTrans(nodeId), e);
            } catch (ExecutionException e) {
                throw new OpenRoadmInterfaceException(error.failedWritingEquipmentStateNoComNoTrans(nodeId), e);
            }
            deviceTx.merge(LogicalDatastoreType.CONFIGURATION, circuitPackIID, cpBldr.build());
            FluentFuture<? extends @NonNull CommitInfo> txSubmitFuture =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            try {
                txSubmitFuture.get();
                LOG.info("Successfully posted equipment state change on node {}", nodeId);
            } catch (InterruptedException e) {
                throw new OpenRoadmInterfaceException(error.failedWritingEquipmentStateComInterrupted(nodeId), e);
            } catch (ExecutionException e) {
                throw new OpenRoadmInterfaceException(error.failedWritingEquipmentStateNoCom(nodeId), e);
            }
        }
    }

    public String getSupportedInterface(String nodeId, String interf) {
        Optional<Interface> supInterfOpt;
        try {
            supInterfOpt = getInterface(nodeId, interf);
            if (supInterfOpt.isPresent()) {
                return supInterfOpt.orElseThrow().getSupportingInterface();
            } else {
                return null;
            }
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("error getting Supported Interface of {} - {}", interf, nodeId, e);
            return null;
        }
    }

    private boolean checkIfDevicePortIsUpdatedWithInterface(String nodeId, InterfaceBuilder ifBuilder) {
        InstanceIdentifier<Ports> portIID = InstanceIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(ifBuilder.getSupportingCircuitPackName()))
            .child(Ports.class, new PortsKey(ifBuilder.getSupportingPort()))
            .build();
        Ports port = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
            portIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT).orElseThrow();
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
