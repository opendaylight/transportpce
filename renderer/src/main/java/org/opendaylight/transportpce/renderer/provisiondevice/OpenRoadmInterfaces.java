/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.renderer.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfaces {

    private final DataBroker db;
    private final String nodeId;
    private final String logicalConnPoint;
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfaces.class);
    private final DataBroker netconfNodeDataBroker;

    public OpenRoadmInterfaces(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint) {
        this.db = db;
        this.logicalConnPoint = logicalConnPoint;
        this.nodeId = nodeId;
        netconfNodeDataBroker = PortMapping.getDeviceDataBroker(nodeId, mps);

    }

    /**
     * This methods creates an OCH interface on the ROADM nodes's termination point
     * based on following steps:
     *
     *  <p>
     *  1. Get the physical mapping corresponding to logical port.
     *  2. Create generic interface object based on data in mapping object.
     *  3. Set the value for supporting interface (OMS) if present in the local mapping
     *     otherwise fetch it for the first time and update the local mapping.
     *  4. Add Och interface specific augmentation such as waveNumber etc.
     *
     * @param waveNumber wavelength number of the Och interface.
     *
     * @return Name of the interface if successful, otherwise return null.
     */
    public String createOchInterface(Long waveNumber) {

        // Add OOCH augmentation to the interface
        Mapping portMap = getMapping(nodeId, logicalConnPoint);
        if (portMap != null) {
            InterfaceBuilder ifBuilder = intfBuilder(portMap);
            // OCH interface specific data
            OchBuilder ocIfBuilder = new OchBuilder();
            ocIfBuilder.setWavelengthNumber(waveNumber);
            Interface1Builder optbld1 = new Interface1Builder();
            ifBuilder.setKey(new InterfaceKey(logicalConnPoint + "-" + waveNumber.toString()));
            ifBuilder.addAugmentation(Interface1.class, optbld1.setOch(ocIfBuilder.build()).build());
            ifBuilder.setName(logicalConnPoint + "-" + waveNumber.toString());
            InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                Interface.class, new InterfaceKey(logicalConnPoint + "-" + waveNumber.toString()));
            if (logicalConnPoint.toUpperCase().contains("TTP") && StringUtils.isNotEmpty(portMap.getSupportingOms())) {
                LOG.info("Oms interface present in local mapping for " + logicalConnPoint);
                ifBuilder.setSupportingInterface(portMap.getSupportingOms());
            } else if (logicalConnPoint.toUpperCase().contains("TTP") && StringUtils.isEmpty(portMap
                .getSupportingOms())) {
                Ports port = getPort(portMap);
                if (port != null && port.getInterfaces() != null) {
                    LOG.info("Oms interface not present in local mapping, getting it for the first time for "
                        + logicalConnPoint);
                    LOG.info(port.toString());
                    for (Interfaces intf : port.getInterfaces()) {
                        LOG.info(intf.toString());
                        //TODO: This method assumes the name of the interface contains OMS substring in it
                        //update it to fetch OMS interface more efficiently.
                        if (intf.getInterfaceName().toUpperCase().contains("OMS")) {
                            String omsInterface = intf.getInterfaceName();
                            LOG.info("found oms interface for {} with name {}", logicalConnPoint, omsInterface);
                            ifBuilder.setSupportingInterface(omsInterface);
                            MappingBuilder mapBldr = new MappingBuilder();
                            InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.create(Network.class).child(
                                Nodes.class, new NodesKey(nodeId)).child(Mapping.class, new MappingKey(portMap
                                    .getLogicalConnectionPoint()));
                            mapBldr.setSupportingOms(omsInterface);
                            mapBldr.setKey(new MappingKey(portMap.getLogicalConnectionPoint()));
                            try {
                                final WriteTransaction writeTransaction = db.newWriteOnlyTransaction();
                                writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, mapIID, mapBldr.build());
                                CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction
                                    .submit();
                                LOG.info("Updated mapping for " + port.getPortName() + "at " + portMap
                                    .getSupportingCircuitPackName() + " with support oms interface " + intf
                                        .getInterfaceName());
                                submit.checkedGet();
                                break;
                            } catch (TransactionCommitFailedException ex) {
                                // TODO Auto-generated catch block
                                LOG.info("unable to save mapping for " + port.getPortName() + " at " + portMap
                                    .getSupportingCircuitPackName());
                            }
                        }
                    }
                } else {
                    LOG.error("Interface is missing for Port " + port.getPortName() + " @ " + portMap
                        .getSupportingCircuitPackName());
                    return null;
                }
            }
            if (postInterface(interfacesIID, ifBuilder)) {
                return ifBuilder.getName();
            } else {
                return null;
            }
        }

        // TODO: implement OCH facility for xponder
        return null;
    }

    public String createODU4Interface() {
        // Add ODU4 augmentation to the interface

        // TODO: implement this method
        return null;

    }

    public String createOTU4Interface() {
        // Add OTU4 augmentation to the interface

        // TODO: implement this method

        return null;
    }

    public String createETHInterface() {
        // Add ETH augmentation to the interface

        // TODO: implement this method

        return null;

    }

    /**
     * This methods does a get operation on the port subtree of the device's
     * operational data store.This method will be called once for each Degree port
     * in order to fetch the OMS interface for the first time.
     *
     * @param portMap Mapping object
     *
     * @return Ports object read from the device.
     */
    public Ports getPort(Mapping portMap) {
        // Get Port subtree corresponding to the logical connection point
        if (netconfNodeDataBroker != null) {
            ReadOnlyTransaction rtx = netconfNodeDataBroker.newReadOnlyTransaction();
            InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                CircuitPacks.class, new CircuitPacksKey(portMap.getSupportingCircuitPackName())).child(Ports.class,
                    new PortsKey(portMap.getSupportingPort()));
            Optional<Ports> portObject;
            try {
                portObject = rtx.read(LogicalDatastoreType.OPERATIONAL, portIID).get();
                if (portObject.isPresent()) {
                    return portObject.get();
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.info("Error getting port subtree from the device ");
                return null;
            }
        }
        return null;
    }

    /**
     * This methods creates a generic interface builder object
     * to set the value that are common irrespective of the interface type.
     *
     * @param portMap Mapping object containing attributes required to create
     *        interface on the device.
     *
     * @return InterfaceBuilder object with the data.
     */
    public InterfaceBuilder intfBuilder(Mapping portMap) {

        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        ifBuilder.setType(OpticalChannel.class);
        ifBuilder.setDescription("  TBD   ");
        ifBuilder.setCircuitId("   TBD    ");
        ifBuilder.setSupportingCircuitPackName(portMap.getSupportingCircuitPackName());
        ifBuilder.setSupportingPort(portMap.getSupportingPort());
        return ifBuilder;
    }

    /**
     * This methods does an edit-config operation on the openROADM
     * device in order to create the given interface.
     *
     * @param interfacesIID Instance identifier for the interfaces subtree in the device.
     * @param ifBuilder Builder object containing the data to post.
     *
     * @return Result of operation true/false based on success/failure.
     */
    public boolean postInterface(InstanceIdentifier<Interface> interfacesIID, InterfaceBuilder ifBuilder) {
        // Post interface with its specific augmentation to the device
        if (netconfNodeDataBroker != null) {
            final WriteTransaction writeTransaction = netconfNodeDataBroker.newWriteOnlyTransaction();
            writeTransaction.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, ifBuilder.build());
            final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
            try {

                submit.checkedGet();
                LOG.info("Successfully posted interface " + ifBuilder.getName());
                return true;
            } catch (TransactionCommitFailedException ex) {
                LOG.warn("Failed to post {} ", ifBuilder.getName());
                return false;
            }

        } else {
            return false;
        }
    }

    /**
     * This method for a given node's termination point returns the Mapping
     * object based on portmapping.yang model stored in the MD-SAL
     * data store which is created when the node is connected for the first time.
     * The mapping object basically contains the following attributes of interest:
     *
     * <p>
     * 1. Supporting circuit pack
     * 2. Supporting port
     * 3. Supporting OMS interface (if port on ROADM)
     *
     * @param nodeId unique Identifier for the node of interest.
     * @param logicalConnPoint Name of the logical point
     *
     * @return Result Mapping object if success otherwise null.
     */
    public Mapping getMapping(String nodeId, String logicalConnPoint) {

        // Getting circuit pack and port corresponding to logical connection
        // point
        InstanceIdentifier<Mapping> portMapping = InstanceIdentifier.builder(Network.class).child(Nodes.class,
            new NodesKey(nodeId)).child(Mapping.class, new MappingKey(logicalConnPoint)).build();
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        Optional<Mapping> mapObject;
        try {
            mapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, portMapping).get();
            if (mapObject.isPresent()) {
                LOG.info("Found mapping for the logical port " + mapObject.get().toString());
                return mapObject.get();
            } else {
                LOG.info("Could not find mapping for logical connection point : " + logicalConnPoint + " for nodeId "
                    + nodeId);
                return null;
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.info("Unable to read mapping for logical connection point : " + logicalConnPoint + " for nodeId "
                + nodeId);
        }
        return null;
    }
}
