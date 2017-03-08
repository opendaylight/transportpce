/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.mapping;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(PortMapping.class);
    private final DataBroker db;
    private final MountPointService mps;

    private final String nodeId;

    public PortMapping(DataBroker db, MountPointService mps, String nodeId) {
        this.db = db;
        this.mps = mps;
        this.nodeId = nodeId;

    }

    /**
    * This method creates logical to physical port mapping
    * for a given device.
    * Instead of parsing all the circuit packs/ports in the device
    * this methods does a selective read operation on degree/srg
    * subtree to get circuit packs/ports that map to :
    *   DEGn-TTP-TX
    *   DEGn-TTP-RX
    *   DEGn-TTP-TXRX
    *   SRGn-PPp
    * This methods skips the logical ports that are internal.
    * if operation is successful the mapping gets stored in
    * datastore corresponding to portmapping.yang data model.
    *
    * @return true/false based on status of operation
    */
    public boolean createMappingData() {

        LOG.info(" Create Mapping Data for node " + nodeId);

        DataBroker deviceDb = getDeviceDataBroker(nodeId, mps);
        Info deviceInfo = getDeviceInfo(deviceDb);

        InstanceIdentifier<Network> nodesIID = InstanceIdentifier.builder(Network.class).build();

        NetworkBuilder nwBldr = new NetworkBuilder();
        List<Mapping> portMapList = new ArrayList<>();
        List<Nodes> nodesList = new ArrayList<>();

        if (deviceDb != null && deviceInfo != null) {

            // Creating mapping data for degree TTP's
            List<ConnectionPorts> degreeConPorts = getDegreePorts(deviceDb, deviceInfo);

            //Getting circuit-pack-name/port-name corresponding to TTP's
            for (ConnectionPorts cp : degreeConPorts) {

                InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                    CircuitPacks.class, new CircuitPacksKey(cp.getCircuitPackName())).child(Ports.class, new PortsKey(cp
                        .getPortName().toString()));
                try {
                    LOG.info("Fetching logical Connection Point value for port " + cp.getPortName().toString()
                        + " at circuit pack " + cp.getCircuitPackName());
                    ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
                    Optional<Ports> portObject = rtx.read(LogicalDatastoreType.OPERATIONAL, portIID).get();

                    if (portObject.isPresent()) {
                        Ports port = portObject.get();
                        if (port.getLogicalConnectionPoint() != null) {

                            LOG.info("Logical Connection Point for " + cp.getCircuitPackName() + " " + port
                                .getPortName() + " is " + port.getLogicalConnectionPoint());
                            MappingBuilder mpBldr = new MappingBuilder();
                            mpBldr.setKey(new MappingKey(port.getLogicalConnectionPoint())).setLogicalConnectionPoint(
                                port.getLogicalConnectionPoint()).setSupportingCircuitPackName(cp.getCircuitPackName())
                                .setSupportingPort(port.getPortName());
                            portMapList.add(mpBldr.build());

                        } else {

                            LOG.warn("Logical Connection Point value missing for " + cp.getCircuitPackName() + " "
                                + port.getPortName());
                        }
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.warn("Read failed for Logical Connection Point value missing for " + cp.getCircuitPackName()
                        + " " + cp.getPortName());
                }
            }
            // Creating mapping data for degree PP's
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.srg.CircuitPacks> srgCps = getSrgCps(
                deviceDb, deviceInfo);

            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.srg.CircuitPacks cps : srgCps) {
                InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                    CircuitPacks.class, new CircuitPacksKey(cps.getCircuitPackName()));
                try {

                    ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
                    Optional<CircuitPacks> circuitPackObject = rtx.read(LogicalDatastoreType.OPERATIONAL, cpIID).get();

                    if (circuitPackObject.isPresent()) {
                        CircuitPacks circuitPack = circuitPackObject.get();
                        if (!circuitPack.getPorts().isEmpty()) {
                            for (Ports port : circuitPack.getPorts()) {

                                if (port.getLogicalConnectionPoint() != null && port.getPortQual().getIntValue() == 2) {

                                    LOG.info("Logical Connection Point for " + circuitPack.getCircuitPackName() + " "
                                        + port.getPortName() + " is " + port.getLogicalConnectionPoint());
                                    MappingBuilder mpBldr = new MappingBuilder();
                                    mpBldr.setKey(new MappingKey(port.getLogicalConnectionPoint()))
                                        .setLogicalConnectionPoint(port.getLogicalConnectionPoint())
                                        .setSupportingCircuitPackName(circuitPack.getCircuitPackName())
                                        .setSupportingPort(port.getPortName());
                                    portMapList.add(mpBldr.build());

                                } else if (port.getPortQual().getIntValue() == 1) {

                                    LOG.info("Port is internal, skipping Logical Connection Point missing for "
                                        + circuitPack.getCircuitPackName() + " " + port.getPortName());

                                } else if (port.getLogicalConnectionPoint() == null) {

                                    LOG.info("Value missing, Skipping Logical Connection Point missing for "
                                        + circuitPack.getCircuitPackName() + " " + port.getPortName());
                                }

                            }

                        }

                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.warn("Read failed for " + cps.getCircuitPackName());
                }

            }

            NodesBuilder nodesBldr = new NodesBuilder();
            nodesBldr.setKey(new NodesKey(deviceInfo.getNodeId())).setNodeId(deviceInfo.getNodeId());
            nodesBldr.setMapping(portMapList);
            nodesList.add(nodesBldr.build());
            nwBldr.setNodes(nodesList);
            final WriteTransaction writeTransaction = db.newWriteOnlyTransaction();
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodesIID, nwBldr.build());
            CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
            try {
                submit.checkedGet();
                return true;

            } catch (TransactionCommitFailedException e) {
                LOG.warn("Failed to post {} ", nwBldr.build(), e);
                return false;

            }
        } else {
            LOG.info(" Unable to get Data broker for node " + nodeId);
            return false;
        }

    }

    /**
     * This method does a get operation on info subtree
     * of the netconf device's config datastore and returns
     * info object.It is required to get device attributes such
     * as maxDegrees,maxSrgs.
     *
     * @param deviceDb Reference to device's databroker
     * @return Info object
     *
     */
    public Info getDeviceInfo(DataBroker deviceDb) {
        ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        try {
            Optional<Info> ordmInfoObject = rtx.read(LogicalDatastoreType.CONFIGURATION, infoIID).get();
            if (ordmInfoObject.isPresent()) {
                return ordmInfoObject.get();
            } else {
                LOG.info("Info subtree is not present");
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.info("Read failed on info subtree for");
            return null;
        }
        return null;
    }

    /**
     * This method does a get operation on degree subtree
     * of the netconf device's config datastore and returns a list
     * of all connection port objects.
     * It is required for doing a selective get on ports that
     * correspond to logical connection points of interest.
     *
     * @param deviceDb Reference to device's databroker
     * @param ordmInfo Info subtree from the device
     * @return List of connection ports object belonging to-
     *         degree subtree
     */
    public List<ConnectionPorts> getDegreePorts(DataBroker deviceDb, Info ordmInfo) {

        List<ConnectionPorts> degreeConPorts = new ArrayList<>();
        ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
        Integer maxDegree;

        //Get value for max degree from info subtree, required for iteration
        //if not present assume to be 20 (temporary)
        if (ordmInfo.getMaxDegrees() != null) {
            maxDegree = ordmInfo.getMaxDegrees();
        } else {
            maxDegree = 20;
        }
        Integer degreeCounter = 1;
        while (degreeCounter <= maxDegree) {
            LOG.info("Getting Connection ports for Degree Number " + degreeCounter);
            InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                Degree.class, new DegreeKey(degreeCounter));
            try {
                Optional<Degree> ordmDegreeObject = rtx.read(LogicalDatastoreType.CONFIGURATION, deviceIID).get();

                if (ordmDegreeObject.isPresent()) {
                    degreeConPorts.addAll(new ArrayList<ConnectionPorts>(ordmDegreeObject.get().getConnectionPorts()));

                } else {
                    LOG.info("Device has " + (degreeCounter - 1) + " degree");
                    break;
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.info("Failed to read degree " + degreeCounter);
                break;

            }
            degreeCounter++;
        }
        return degreeConPorts;
    }

    /**
     * This method does a get operation on shared risk group subtree
     * of the netconf device's config datastore and returns a list
     * of all circuit packs objects that are part of srgs.
     * It is required to do a selective get on all the circuit packs
     * that contain add/drop ports of interest.
     *
     * @param deviceDb Reference to device's databroker
     * @param ordmInfo Info subtree from the device
     * @return List of circuit packs object belonging to-
     *         shared risk group subtree
     */

    public List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.srg.CircuitPacks> getSrgCps(
        DataBroker deviceDb, Info ordmInfo) {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.srg.CircuitPacks> srgCps =
            new ArrayList<>();
        ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
        Integer maxSrg;
        //Get value for max Srg from info subtree, required for iteration
        //if not present assume to be 20 (temporary)
        if (ordmInfo.getMaxSrgs() != null) {
            maxSrg = ordmInfo.getMaxSrgs();
        } else {
            maxSrg = 20;
        }

        Integer srgCounter = 1;
        while (srgCounter <= maxSrg) {
            LOG.info("Getting Circuitpacks for Srg Number " + srgCounter);
            InstanceIdentifier<SharedRiskGroup> srgIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                SharedRiskGroup.class, new SharedRiskGroupKey(srgCounter));
            try {
                Optional<SharedRiskGroup> ordmSrgObject = rtx.read(LogicalDatastoreType.CONFIGURATION, srgIID).get();

                if (ordmSrgObject.isPresent()) {

                    srgCps.addAll(
                        new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.srg
                            .CircuitPacks>(ordmSrgObject.get().getCircuitPacks()));

                } else {
                    LOG.info("Device has " + (srgCounter - 1) + " Srg");
                    break;
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Failed to read Srg " + srgCounter);
                break;
            }
            srgCounter++;
        }

        return srgCps;
    }

    /**
     * This static method returns the DataBroker for a netconf
     * node.
     *
     * @param nodeId Unique identifier for the mounted netconf-
     *               node
     * @param mps Reference to mount service
     * @return Databroker for the given device
     */

    public static DataBroker getDeviceDataBroker(String nodeId, MountPointService mps) {
        InstanceIdentifier<Node> netconfNodeIID = InstanceIdentifier.builder(NetworkTopology.class).child(
            Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).child(Node.class,
                new NodeKey(new NodeId(nodeId))).build();

        // Get the mount point for the specified node
        final Optional<MountPoint> netconfNodeOptional = mps.getMountPoint(netconfNodeIID);
        if (netconfNodeOptional.isPresent()) {
            MountPoint netconfNode = netconfNodeOptional.get();
            // Get the DataBroker for the mounted node
            DataBroker netconfNodeDataBroker = netconfNode.getService(DataBroker.class).get();
            return netconfNodeDataBroker;
        } else {
            LOG.info("Device Data broker not found for :" + nodeId);
        }
        return null;
    }
}