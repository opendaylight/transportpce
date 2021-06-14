/*
 * Copyright © 2020 AT&T and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.States;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfacesImpl710 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfacesImpl710.class);

    private final DeviceTransactionManager deviceTransactionManager;

    public OpenRoadmInterfacesImpl710(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
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
        try {
            txSubmitFuture.get();
            LOG.info("Successfully posted interface {} on node {}", ifBuilder.getName(), nodeId);
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


    public void deleteInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        LOG.info("deleting interface {} on device71 {}", interfaceName, nodeId);
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
            InterfaceBuilder ifBuilder = new InterfaceBuilder();
            ifBuilder.setName(intf2Delete.getName());
            ifBuilder.setType(intf2Delete.getType());
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
                return supInterfOpt.get().getSupportingInterfaceList().get(0);
            } else {
                return null;
            }
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("error getting Supported Interface of {} - {}", interf, nodeId, e);
            return null;
        }
    }

}
