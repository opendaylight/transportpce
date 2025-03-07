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
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.config.Config;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.time.Timeout;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfacesImpl221 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfacesImpl221.class);

    private final DeviceTransactionManager deviceTransactionManager;
    private final PortMapping portMapping;
    private final PortMappingVersion221 portMapping221;
    private final Config configuration;

    public OpenRoadmInterfacesImpl221(DeviceTransactionManager deviceTransactionManager, PortMapping portMapping,
            Config configuration) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.portMapping = portMapping;
        this.portMapping221 = portMapping.getPortMappingVersion221();
        this.configuration = configuration;
    }

    @SuppressWarnings({"checkstyle:EmptyBlock", "checkstyle:VariableDeclarationUsageDistance"})
    public void postInterface(String nodeId, InterfaceBuilder ifBuilder) throws OpenRoadmInterfaceException {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(nodeId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.orElseThrow();
            } else {
                throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                    nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                nodeId), e);
        }

        String interfaceName = ifBuilder.getName();
        DataObjectIdentifier<Interface> interfacesIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey(interfaceName))
            .build();
        LOG.info("POST INTERF for {} : InterfaceBuilder : name = {} \t type = {}", nodeId, interfaceName,
            ifBuilder.getType().toString());
        deviceTx.merge(LogicalDatastoreType.CONFIGURATION, interfacesIID, ifBuilder.build());
        Timeout timeout = configuration.deviceReadTimeout();
        FluentFuture<? extends @NonNull CommitInfo> txSubmitFuture =
                deviceTx.commit(timeout.time(), timeout.unit());
        try {
            txSubmitFuture.get();
            long timeoutMilliseconds = timeout.unit().toMillis(
                    configuration.deviceReadTimeout().time());

            Thread timer = new Thread() {
                public void run() {
                    try {
                        LOG.info("Waiting {} ms for the node {} to respond with the newly created interface {}...",
                                timeoutMilliseconds, nodeId, interfaceName);
                        Thread.sleep(timeoutMilliseconds);
                        this.interrupt();
                    } catch (InterruptedException e) {
                        LOG.error("Confirmation interrupted while reading from node {} searching for interface {}",
                                nodeId, interfaceName, e);
                    }
                }
            };
            String supportingCircuitPackName = ifBuilder.getSupportingCircuitPackName();
            String supportingPort = ifBuilder.getSupportingPort();
            // this check is not needed during the delete operation
            // during the delete operation, ifBuilder does not contain supporting-cp and supporting-port
            if (supportingCircuitPackName != null && supportingPort != null) {
                LOG.info("Successfully posted interface {} on node {}, awaiting confirmation from node...",
                        interfaceName, nodeId);

                timer.start();
                LOG.info("Reading from node {}...", nodeId);
                boolean devicePortIsUptodated = checkIfDevicePortIsUpdatedWithInterface(nodeId, ifBuilder);

                Timeout periodicTimeout = getPeriodicPollTimeout(timeout);
                while (!devicePortIsUptodated && !timer.isInterrupted()) {
                    try {
                        LOG.debug("Interface {} not found on node {}, waiting {} {} before retrying, "
                                        + "unless aborted since the overall timeout {} {} has passed...",
                                interfaceName, nodeId, periodicTimeout.time(), periodicTimeout.unit(),
                                timeout.time(), timeout.unit());
                        // TODO: Replace this timer with a notification from the node
                        Thread.sleep(periodicTimeout.unit().toMillis(periodicTimeout.time()));
                        LOG.debug("Reading from node {}...", nodeId);
                        devicePortIsUptodated = checkIfDevicePortIsUpdatedWithInterface(nodeId, ifBuilder);
                    } catch (InterruptedException ignored) {
                    }
                }

                if (devicePortIsUptodated) {
                    LOG.info("Confirmed, {} - {} - interface {} updated on port {}", nodeId,
                            supportingCircuitPackName, interfaceName, supportingPort);
                } else {
                    LOG.warn("Unable to confirm, {} - {} - interface {} updated on port {}", nodeId,
                            supportingCircuitPackName, interfaceName, supportingPort);
                }
            } else {
                LOG.info("Deleted interface {} on node {}, not waiting for confirmation from node", interfaceName,
                        nodeId);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to post interface %s on node %s!",
                    interfaceName, nodeId), e);
        }
    }

    /**
     * The goal is to poll the node periodically to check if the interface has been updated.
     * This method also makes sure that the timeout is not too long or too short.
     */
    public Timeout getPeriodicPollTimeout(Timeout timeout) {
        if (timeout.time() <= 0) {
            return new Timeout(3, TimeUnit.SECONDS);
        }

        long millis = timeout.unit().toMillis(timeout.time());
        long t1 = (millis - millis % 2) / 2;

        if (t1 < 1000) {
            return new Timeout(1, TimeUnit.SECONDS);
        }

        //Let's make sure we don't wait forever retrying
        if (t1 > 3000) {
            return new Timeout(3, TimeUnit.SECONDS);
        }

        return new Timeout(t1, TimeUnit.MILLISECONDS);
    }


    public Optional<Interface> getInterface(String nodeId, String interfaceName) {
        DataObjectIdentifier<Interface> interfacesIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey(interfaceName))
            .build();
        return deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION,
            interfacesIID, configuration.deviceReadTimeout().time(), configuration.deviceReadTimeout().unit());
    }


    public synchronized void deleteInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        LOG.info("deleting interface {} on device221 {}", interfaceName, nodeId);
        Optional<Interface> intf2DeleteOpt;
        intf2DeleteOpt = getInterface(nodeId, interfaceName);
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
                throw new OpenRoadmInterfaceException(String.format("Failed to set state of interface %s to %s while"
                    + " deleting it!", interfaceName, AdminStates.OutOfService), ex);
            }

            DataObjectIdentifier<Interface> interfacesIID = DataObjectIdentifier
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
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                    nodeId), e);
            }

            deviceTx.delete(LogicalDatastoreType.CONFIGURATION, interfacesIID);
            FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(configuration.deviceWriteTimeout().time(), configuration.deviceWriteTimeout().unit());

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
        DataObjectIdentifier<CircuitPacks> circuitPackIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
            .build();
        Optional<CircuitPacks> cpOpt = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, circuitPackIID, configuration.deviceReadTimeout().time(),
            configuration.deviceReadTimeout().unit());
        CircuitPacks cp = null;
        if (cpOpt.isPresent()) {
            cp = cpOpt.orElseThrow();
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
                    deviceTx = deviceTxOpt.orElseThrow();
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
                deviceTx.commit(configuration.deviceWriteTimeout().time(), configuration.deviceWriteTimeout().unit());
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
        supInterfOpt = getInterface(nodeId, interf);
        return supInterfOpt.map(s -> s.getSupportingInterface()).orElse(null);
    }

    private boolean checkIfDevicePortIsUpdatedWithInterface(String nodeId, InterfaceBuilder ifBuilder) {
        DataObjectIdentifier<Ports> portIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(ifBuilder.getSupportingCircuitPackName()))
            .child(Ports.class, new PortsKey(ifBuilder.getSupportingPort()))
            .build();
        Ports port = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
            portIID, configuration.deviceReadTimeout().time(), configuration.deviceReadTimeout().unit()).orElseThrow();
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
