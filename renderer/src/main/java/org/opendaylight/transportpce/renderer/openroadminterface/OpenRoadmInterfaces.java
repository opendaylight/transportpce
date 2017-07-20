/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.renderer.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfaces {

    protected final DataBroker db;
    protected final DataBroker netconfNodeDataBroker;
    protected final String nodeId;
    protected final MountPointService mps;
    protected final Mapping portMap;
    protected final String logicalConnPoint;
    private final String serviceName;
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfaces.class);

    public OpenRoadmInterfaces(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint) {
        this.db = db;
        this.mps = mps;
        this.logicalConnPoint = logicalConnPoint;
        this.nodeId = nodeId;
        if (logicalConnPoint != null) {
            this.portMap = PortMapping.getMapping(nodeId, logicalConnPoint, db);
        } else {
            this.portMap = null;
        }
        this.serviceName = null;
        netconfNodeDataBroker = PortMapping.getDeviceDataBroker(nodeId, mps);
    }

    public OpenRoadmInterfaces(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint,
            String serviceName) {
        this.db = db;
        this.mps = mps;
        this.logicalConnPoint = logicalConnPoint;
        this.nodeId = nodeId;
        if (logicalConnPoint != null) {
            this.portMap = PortMapping.getMapping(nodeId, logicalConnPoint, db);
        } else {
            this.portMap = null;
        }
        this.serviceName = serviceName;
        netconfNodeDataBroker = PortMapping.getDeviceDataBroker(nodeId, mps);
    }

    /**
     * This methods creates a generic interface builder object to set the value that
     * are common irrespective of the interface type.
     *
     * @param portMap
     *            Mapping object containing attributes required to create interface
     *            on the device.
     *
     * @return InterfaceBuilder object with the data.
     */
    public InterfaceBuilder getIntfBuilder(Mapping portMap) {

        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        ifBuilder.setDescription("  TBD   ");
        ifBuilder.setCircuitId("   TBD    ");
        ifBuilder.setSupportingCircuitPackName(portMap.getSupportingCircuitPackName());
        ifBuilder.setSupportingPort(portMap.getSupportingPort());
        ifBuilder.setAdministrativeState(AdminStates.InService);
        return ifBuilder;
    }

    /**
     * This methods does an edit-config operation on the openROADM device in order
     * to create the given interface.
     *
     * <p>
     * Before posting the interface it checks if:
     *
     * <p>
     * 1. Interface with same name does not exist
     *
     * <p>
     * 2. If exists then admin state of interface is outOfState/Maintenance
     *
     * @param ifBuilder
     *            Builder object containing the data to post.
     *
     * @return Result of operation true/false based on success/failure.
     */
    public boolean postInterface(InterfaceBuilder ifBuilder) {
        String intf2Post = ifBuilder.getName();
        Interface intf2PostCheck = getInterface(intf2Post);
        if (intf2PostCheck != null) {
            if (intf2PostCheck.getAdministrativeState() == AdminStates.InService) {
                LOG.info("Interface with same name in service already exists on node " + nodeId);
                return true;
            }
        }
        // Post interface with its specific augmentation to the device
        if (netconfNodeDataBroker != null) {
            InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(Interface.class, new InterfaceKey(ifBuilder.getName()));
            final WriteTransaction writeTransaction = netconfNodeDataBroker.newWriteOnlyTransaction();
            writeTransaction.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, ifBuilder.build());
            final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
            try {
                submit.checkedGet();
                LOG.info("Successfully posted interface " + ifBuilder.getName() + " on node " + nodeId);
                return true;
            } catch (TransactionCommitFailedException ex) {
                LOG.warn("Failed to post {} ", ifBuilder.getName() + " on node " + nodeId, ex);
                return false;
            }

        } else {
            return false;
        }
    }

    /**
     * This private does a get on the interface subtree of the device with the
     * interface name as the key and return the class corresponding to the interface
     * type.
     *
     * @param interfaceName
     *            Name of the interface
     *
     * @return true/false based on status of operation
     */

    public Interface getInterface(String interfaceName) {
        ReadOnlyTransaction rtx = netconfNodeDataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        try {
            Optional<Interface> interfaceObject = rtx.read(LogicalDatastoreType.CONFIGURATION, interfacesIID).get();
            if (interfaceObject.isPresent()) {
                return interfaceObject.get();
            } else {
                LOG.info("Interface subtree is not present for " + interfaceName + " on node " + nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.info("Read failed on interface subtree for" + interfaceName + " on node " + nodeId, ex);
            return null;
        }
        return null;
    }

    /**
     * This methods does an edit-config operation on the openROADM device in order
     * to delete the given interface.
     *
     * <p>
     * Before deleting the method:
     *
     * <p>
     * 1. Checks if interface exists
     *
     * <p>
     * 2. If exists then changes the state of interface to outOfService
     *
     * @param interfaceName
     *            Name of the interface to delete.
     *
     * @return Result of operation true/false based on success/failure.
     */
    public boolean deleteInterface(String interfaceName) {
        // Post interface with its specific augmentation to the device
        if (netconfNodeDataBroker != null) {
            Interface intf2Delete = getInterface(interfaceName);
            if (intf2Delete != null) {
                // State admin state to out of service
                InterfaceBuilder ifBuilder = new InterfaceBuilder(intf2Delete);
                ifBuilder.setAdministrativeState(AdminStates.OutOfService);
                // post interface with updated admin state
                if (postInterface(ifBuilder)) {
                    InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                            .child(Interface.class, new InterfaceKey(interfaceName));
                    final WriteTransaction writeTransaction = netconfNodeDataBroker.newWriteOnlyTransaction();
                    writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, interfacesIID);
                    final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();

                    try {
                        submit.checkedGet();
                        LOG.info("Successfully deleted " + interfaceName + " on node " + nodeId);
                        return true;

                    } catch (TransactionCommitFailedException ex) {
                        LOG.error("Failed to delete interface " + interfaceName + " on node " + nodeId);
                        return false;
                    }

                } else {

                    LOG.error("Error changing the state of interface " + interfaceName + " on node " + nodeId);
                    return false;
                }
            } else {
                LOG.info("Interface does not exist, cannot delete on node " + nodeId);
                return false;
            }

        } else {

            LOG.info("Device databroker not found on node " + nodeId);
            return false;
        }
    }
}
