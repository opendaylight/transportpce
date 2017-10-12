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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
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
     * This method creates logical to physical port mapping for a given device.
     * Instead of parsing all the circuit packs/ports in the device this methods
     * does a selective read operation on degree/srg subtree to get circuit
     * packs/ports that map to :
     *
     * <p>
     * 1. DEGn-TTP-TX, DEGn-TTP-RX, DEGn-TTP-TXRX
     *
     * <p>
     * 2. SRGn-PPp-TX, SRGn-PPp-RX, SRGn-PPp-TXRX
     *
     * <p>
     * 3. LINEn
     *
     * <p>
     * 4. CLNTn.
     *
     * <p>
     * If the port is Mw it also store the OMS, OTS interface provisioned on the
     * port. It skips the logical ports that are internal. If operation is
     * successful the mapping gets stored in datastore corresponding to
     * portmapping.yang data model.
     *
     * @return true/false based on status of operation
     */
    public boolean createMappingData() {

        LOG.info("Create Mapping Data for node " + nodeId);
        DataBroker deviceDb = getDeviceDataBroker(nodeId, mps);
        List<Mapping> portMapList = new ArrayList<>();
        Info deviceInfo;
        Integer nodeType = 1;
        if (deviceDb != null) {
            deviceInfo = getDeviceInfo(deviceDb);
            if (deviceInfo != null) {
                if (deviceInfo.getNodeType() == null) {
                    LOG.info("Node type mandatory field is missing");
                    return false;
                }
                nodeType = deviceInfo.getNodeType().getIntValue();
                // Create Mapping for Roadm Node
                if (nodeType == 1) {
                    // Get TTP port mapping
                    if (!createTtpPortMapping(deviceDb, deviceInfo, portMapList)) {
                        // return false if mapping creation for TTP's failed
                        LOG.info("Unable to create mapping for TTP's");
                        return false;
                    }

                    // Get PP port mapping
                    if (!createPpPortMapping(deviceDb, deviceInfo, portMapList)) {
                        // return false if mapping creation for PP's failed
                        LOG.info("Unable tp create mapping for PP's");
                        return false;
                    }
                }
                // Create Mapping for Xponder Node
                if (nodeType == 2) {
                    if (!createXpdrPortMapping(deviceDb, deviceInfo, portMapList)) {
                        LOG.info("Unable to create mapping for Xponder");
                        return false;
                    }
                }
            } else {
                LOG.info("Device info subtree is absent for " + nodeId);
                return false;
            }

        } else {
            LOG.info("Unable to get Data broker for node " + nodeId);
            return false;
        }
        return postPortMapping(deviceInfo, portMapList, nodeType);
    }

    /**
     * This method removes mapping data from the datastore after disconnecting
     * ODL from a Netconf device.
     */
    public void deleteMappingData() {
        LOG.info("Deleting Mapping Data corresponding at node " + nodeId);
        WriteTransaction rw = db.newWriteOnlyTransaction();
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        rw.delete(LogicalDatastoreType.CONFIGURATION, nodesIID);
        try {
            rw.submit().get(1, TimeUnit.SECONDS);
            LOG.info("Port mapping removal for node " + nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing port mapping infos for node " + nodeId);
        }
    }

    /**
     * This private method gets the list of external ports on a degree. For each
     * port in the degree, it does a get on port subtree with
     * circuit-pack-name/port-name as key in order to get the logical connection
     * point name corresponding to it.
     *
     * @param deviceDb
     *            Reference to device's databroker
     * @param deviceInfo
     *            Info subtree read from the device
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL
     *
     * @return true/false based on status of operation
     */
    private boolean createTtpPortMapping(DataBroker deviceDb, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for degree TTP's
        List<ConnectionPorts> degreeConPorts = getDegreePorts(deviceDb, deviceInfo);

        // Getting circuit-pack-name/port-name corresponding to TTP's
        for (ConnectionPorts cp : degreeConPorts) {
            String circuitPackName = cp.getCircuitPackName();
            String portName = cp.getPortName().toString();
            InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                CircuitPacks.class, new CircuitPacksKey(circuitPackName)).child(Ports.class, new PortsKey(portName));
            try {
                LOG.info("Fetching logical Connection Point value for port " + portName + " at circuit pack "
                    + circuitPackName);
                ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
                Optional<Ports> portObject = rtx.read(LogicalDatastoreType.OPERATIONAL, portIID).get();
                if (portObject.isPresent()) {
                    Ports port = portObject.get();
                    if (port.getLogicalConnectionPoint() != null) {

                        LOG.info("Logical Connection Point for " + circuitPackName + " " + portName + " is " + port
                            .getLogicalConnectionPoint());
                        portMapList.add(createMappingObject(port, circuitPackName, port.getLogicalConnectionPoint(),
                            deviceDb));
                    } else {

                        LOG.warn("Logical Connection Point value missing for " + circuitPackName + " " + port
                            .getPortName());
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Read failed for Logical Connection Point value missing for " + circuitPackName + " "
                    + portName, ex);
                return false;
            }
        }
        return true;
    }

    /**
     * This private method gets the list of circuit packs on an Srg. For each
     * circuit pack on an Srg, it does a get on circuit-pack subtree with
     * circuit-pack-name as key in order to get the list of ports. It then
     * iterates over the list of ports to get ports with port-qual as
     * roadm-external. It appends a TX,RX,TXRX to the logical connection point
     * name based on the direction of the port.
     *
     * @param deviceDb
     *            Reference to device's databroker
     * @param deviceInfo
     *            Info subtree read from the device
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL
     *
     * @return true/false based on status of operation
     */

    private boolean createPpPortMapping(DataBroker deviceDb, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for degree PP's
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCps = getSrgCps(
            deviceDb, deviceInfo);

        for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks cps : srgCps) {
            String circuitPackName = cps.getCircuitPackName();
            try {
                InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                    CircuitPacks.class, new CircuitPacksKey(circuitPackName));
                ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
                Optional<CircuitPacks> circuitPackObject = rtx.read(LogicalDatastoreType.OPERATIONAL, cpIID).get();

                if (circuitPackObject.isPresent()) {
                    CircuitPacks cp = circuitPackObject.get();
                    if (cp.getPorts() == null) {
                        LOG.warn("No port found for {} {}: {}", deviceInfo.getNodeId(), circuitPackName, cp);
                    } else if (!cp.getPorts().isEmpty()) {
                        for (Ports port : cp.getPorts()) {

                            if (port.getLogicalConnectionPoint() != null && port.getPortQual().getIntValue() == 2) {
                                String logicalConnectionPoint = null;
                                if (port.getPortDirection().getIntValue() == 1) {
                                    // Port direction is transmit
                                    logicalConnectionPoint = port.getLogicalConnectionPoint() + "-TX";
                                }
                                if (port.getPortDirection().getIntValue() == 2) {
                                    // Port direction is receive
                                    logicalConnectionPoint = port.getLogicalConnectionPoint() + "-RX";
                                }
                                if (port.getPortDirection().getIntValue() == 3) {
                                    // port is bi-directional
                                    logicalConnectionPoint = port.getLogicalConnectionPoint() + "-TXRX";
                                }

                                LOG.info("Logical Connection Point for " + circuitPackName + " " + port.getPortName()
                                    + " is " + logicalConnectionPoint);

                                portMapList.add(createMappingObject(port, circuitPackName, logicalConnectionPoint,
                                    deviceDb));

                            } else if (port.getPortQual().getIntValue() == 1) {

                                LOG.info("Port is internal, skipping Logical Connection Point missing for "
                                    + circuitPackName + " " + port.getPortName());

                            } else if (port.getLogicalConnectionPoint() == null) {

                                LOG.info("Value missing, Skipping Logical Connection Point missing for "
                                    + circuitPackName + " " + port.getPortName());
                            }

                        }

                    }

                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Read failed for " + circuitPackName, ex);
                return false;
            }
        }

        return true;
    }

    /**
     * This private method gets the list of circuit packs on a xponder. For each
     * circuit pack on a Xponder, it does a get on circuit-pack subtree with
     * circuit-pack-name as key in order to get the list of ports. It then
     * iterates over the list of ports to get ports with port-qual as
     * xpdr-network/xpdr-client. The line and client ports are saved as:
     *
     * <p>
     * 1. LINEn
     *
     * <p>
     * 2. CLNTn
     *
     * @param deviceDb
     *            Reference to device's databroker
     * @param deviceInfo
     *            Info subtree read from the device
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL
     *
     * @return true/false based on status of operation
     */

    private boolean createXpdrPortMapping(DataBroker deviceDb, Info deviceInfo, List<Mapping> portMapList) {
        // Creating for Xponder Line and Client Ports
        try {
            InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
            ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
            Optional<OrgOpenroadmDevice> deviceObject = rtx.read(LogicalDatastoreType.OPERATIONAL, deviceIID).get();

            // Variable to keep track of number of line ports
            int line = 1;
            // Variable to keep track of number of client ports
            int client = 1;
            if (deviceObject.isPresent()) {
                for (CircuitPacks cp : deviceObject.get().getCircuitPacks()) {
                    String circuitPackName = cp.getCircuitPackName();
                    if (cp.getPorts() == null) {
                        LOG.warn("No port found for {}, circuit pack {}", deviceInfo.getNodeId(), circuitPackName);
                    } else {
                        for (Ports port : cp.getPorts()) {
                            if (port.getPortQual() != null) {
                                if (port.getPortQual().getIntValue() == 3) {
                                    // Port is xpdr-network
                                    portMapList.add(createMappingObject(port, circuitPackName, "XPDR-LINE"
                                        + line, deviceDb));
                                    LOG.info("Logical Connection Point for {} {} is XPDR-LINE{}", circuitPackName, port
                                        .getPortName(), line);
                                    line++;
                                }
                                if (port.getPortQual().getIntValue() == 4) {
                                    // port is xpdr-client
                                    portMapList.add(createMappingObject(port, circuitPackName, "XPDR-CLNT"
                                        + client, deviceDb));
                                    LOG.info("Logical Connection Point for {} {} is XPDR-CLNT{}", circuitPackName, port
                                        .getPortName(), client);
                                    client++;
                                }
                            } else {
                                LOG.info("no PortQual for port {} of circuit pack {}", port.getPortName(), cp
                                    .getCircuitPackName());
                            }
                        }
                    }
                }
            } else {
                LOG.info("No deviceObject present for {}", nodeId);
                return false;
            }

        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Read failed for CircuitPacks of {}", nodeId, ex);
            return false;
        }
        return true;
    }

    /**
     * This private method builds the mapping object to be pushed in MD-SAL in
     * order to save port mapping. In case of TTP ports, it also saves the
     * OTS,OMS interfaces provisioned on the port.
     *
     * @param port
     *            Reference to device's port subtree object.
     * @param circuitPackName
     *            Name of cp where port exists.
     * @param logicalConnectionPoint
     *            logical name of the port.
     * @param deviceDb
     *            Reference to device's databroker.
     *
     * @return true/false based on status of operation
     */

    private Mapping createMappingObject(Ports port, String circuitPackName, String logicalConnectionPoint,
        DataBroker deviceDb) {
        MappingBuilder mpBldr = new MappingBuilder();
        mpBldr.setKey(new MappingKey(logicalConnectionPoint)).setLogicalConnectionPoint(logicalConnectionPoint)
            .setSupportingCircuitPackName(circuitPackName).setSupportingPort(port.getPortName());

        // Get OMS and OTS interface provisioned on the TTP's
        if (logicalConnectionPoint.contains("TTP") && port.getInterfaces() != null) {
            for (Interfaces interfaces : port.getInterfaces()) {
                Class<? extends InterfaceType> interfaceType = new OpenRoadmInterfaces(db, mps, nodeId,
                    logicalConnectionPoint).getInterface(interfaces.getInterfaceName()).getType();
                // Check if interface type is OMS or OTS
                if (interfaceType.equals(OpenROADMOpticalMultiplex.class)) {
                    String omsInterfaceName = interfaces.getInterfaceName();
                    mpBldr.setSupportingOms(omsInterfaceName);
                }
                if (interfaceType.equals(OpticalTransport.class)) {
                    String otsInterfaceName = interfaces.getInterfaceName();
                    mpBldr.setSupportingOts(otsInterfaceName);
                }
            }
        }
        return mpBldr.build();
    }

    /**
     * This method does a get operation on info subtree of the netconf device's
     * configuration datastore and returns info object.It is required to get
     * device attributes such as maxDegrees,maxSrgs and node-type.
     *
     * @param deviceDb
     *            Reference to device's databroker
     * @return Info object
     *
     */
    private Info getDeviceInfo(DataBroker deviceDb) {
        ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        try {
            Optional<Info> ordmInfoObject = rtx.read(LogicalDatastoreType.OPERATIONAL, infoIID).get();
            if (ordmInfoObject.isPresent()) {
                LOG.info("Info subtree is present {}", ordmInfoObject.get());
                return ordmInfoObject.get();
            } else {
                LOG.error("Info subtree is not present");
            }
        } catch (NullPointerException ex) {
            LOG.warn("Try to get Info from a non Open ROADM device {}", deviceDb);
            return null;
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Read failed on info subtree ", ex);
            return null;
        }
        return null;
    }

    /**
     * This method does a get operation on degree subtree of the netconf
     * device's config datastore and returns a list of all connection port
     * objects. It is required for doing a selective get on ports that
     * correspond to logical connection points of interest.
     *
     * @param deviceDb
     *            Reference to device's databroker
     * @param ordmInfo
     *            Info subtree from the device
     * @return List of connection ports object belonging to- degree subtree
     */
    private List<ConnectionPorts> getDegreePorts(DataBroker deviceDb, Info ordmInfo) {

        List<ConnectionPorts> degreeConPorts = new ArrayList<>();
        ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
        Integer maxDegree;

        // Get value for max degree from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
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
                    degreeConPorts.addAll(new ArrayList<>(ordmDegreeObject.get().getConnectionPorts()));

                } else {
                    LOG.info("Device has " + (degreeCounter - 1) + " degree");
                    break;
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error("Failed to read degree " + degreeCounter, ex);
                break;

            }
            degreeCounter++;
        }
        return degreeConPorts;
    }

    /**
     * This method does a get operation on shared risk group subtree of the
     * netconf device's config datastore and returns a list of all circuit packs
     * objects that are part of srgs. It is required to do a selective get on
     * all the circuit packs that contain add/drop ports of interest.
     *
     * @param deviceDb
     *            Reference to device's databroker
     * @param ordmInfo
     *            Info subtree from the device
     * @return List of circuit packs object belonging to- shared risk group
     *         subtree
     */

    private List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> getSrgCps(
        DataBroker deviceDb, Info ordmInfo) {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCps =
            new ArrayList<>();
        ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
        Integer maxSrg;
        // Get value for max Srg from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
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
                        new ArrayList<>(ordmSrgObject.get().getCircuitPacks()));

                } else {
                    LOG.info("Device has " + (srgCounter - 1) + " Srg");
                    break;
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Failed to read Srg " + srgCounter, ex);
                break;
            }
            srgCounter++;
        }

        return srgCps;
    }

    /**
     * This method for ports the portMapping corresponding to the
     * portmapping.yang file to the MD-SAL datastore.
     *
     * <p>
     * 1. Supporting circuit pack 2. Supporting port 3. Supporting OMS interface
     * (if port on ROADM)
     *
     * @param deviceInfo
     *            Info subtree from the device.
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL.
     *
     * @return Result true/false based on status of operation.
     */
    private boolean postPortMapping(Info deviceInfo, List<Mapping> portMapList, Integer nodeType) {

        NodesBuilder nodesBldr = new NodesBuilder();
        nodesBldr.setKey(new NodesKey(deviceInfo.getNodeId())).setNodeId(deviceInfo.getNodeId());
        nodesBldr.setNodeType(NodeTypes.forValue(nodeType));
        nodesBldr.setMapping(portMapList);
        List<Nodes> nodesList = new ArrayList<>();
        nodesList.add(nodesBldr.build());
        NetworkBuilder nwBldr = new NetworkBuilder();
        nwBldr.setNodes(nodesList);
        final WriteTransaction writeTransaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<Network> nodesIID = InstanceIdentifier.builder(Network.class).build();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodesIID, nwBldr.build());
        CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
        try {
            submit.checkedGet();
            return true;

        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to post {} ", nwBldr.build(), e);
            return false;

        }
    }

    /**
     * This method for a given node's termination point returns the Mapping
     * object based on portmapping.yang model stored in the MD-SAL data store
     * which is created when the node is connected for the first time. The
     * mapping object basically contains the following attributes of interest:
     *
     * <p>
     * 1. Supporting circuit pack
     *
     * <p>
     * 2. Supporting port
     *
     * <p>
     * 3. Supporting OMS interface (if port on ROADM) 4. Supporting OTS
     * interface (if port on ROADM)
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     * @param logicalConnPoint
     *            Name of the logical point
     * @param db
     *            Databroker / MD-SAL data store
     *
     * @return Result Mapping object if success otherwise null.
     */
    public static Mapping getMapping(String nodeId, String logicalConnPoint, DataBroker db) {

        /*
         * Getting physical mapping corresponding to logical connection point
         */
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
            LOG.error("Unable to read mapping for logical connection point : " + logicalConnPoint + " for nodeId "
                + nodeId, ex);
        }
        return null;
    }

    /**
     * This static method returns the DataBroker for a netconf node.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param mps
     *            Reference to mount service
     * @return Databroker for the given device
     */
    public static DataBroker getDeviceDataBroker(String nodeId, MountPointService mps) {
        MountPoint netconfNode = getDeviceMountPoint(nodeId, mps);
        if (netconfNode != null) {
            DataBroker netconfNodeDataBroker = netconfNode.getService(DataBroker.class).get();
            return netconfNodeDataBroker;
        } else {
            LOG.error("Device Data broker not found for :" + nodeId);
            return null;
        }
    }

    public static MountPoint getDeviceMountPoint(String nodeId, MountPointService mps) {
        InstanceIdentifier<Node> netconfNodeIID = InstanceIdentifier.builder(NetworkTopology.class).child(
            Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName()))).child(Node.class,
                new NodeKey(new NodeId(nodeId))).build();

        // Get mount point for specified device
        final Optional<MountPoint> netconfNodeOptional = mps.getMountPoint(netconfNodeIID);
        if (netconfNodeOptional.isPresent()) {
            MountPoint netconfNode = netconfNodeOptional.get();
            return netconfNode;
        } else {
            LOG.error("Mount Point not found for :" + nodeId);
            return null;
        }

    }
}
