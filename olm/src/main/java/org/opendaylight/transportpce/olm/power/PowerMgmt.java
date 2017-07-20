/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaces;
import org.opendaylight.transportpce.renderer.provisiondevice.CrossConnect;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
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

public class PowerMgmt {
    private static final Logger LOG = LoggerFactory.getLogger(PowerMgmt.class);
    private final DataBroker db;
    private final MountPointService mps;
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class,
                            new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public PowerMgmt(DataBroker db, MountPointService mps) {
        this.db = db;
        this.mps = mps;
    }

    /**
     * This methods measures power requirement for turning up a WL
     * from the Spanloss at OTS transmit direction and update
     * roadm-connection target-output-power.
     *
     * @param input
     *            Input parameter from the olm servicePowerSetup rpc
     *
     * @return true/false based on status of operation.
     */
    public Boolean setPower(ServicePowerSetupInput input) {
        LOG.info("Olm-setPower initiated");
        for (int i = 0; i < input.getNodes().size(); i++) {
            String nodeId = input.getNodes().get(i).getNodeId();
            String srcTpId =  input.getNodes().get(i).getSrcTp();
            String destTpId = input.getNodes().get(i).getDestTp();
            DataBroker deviceDb = getDeviceDataBroker(nodeId , mps);
            Nodes inputNode = getNode(nodeId, mps, db);
            LOG.info("Getting data from input node {}",inputNode.getNodeType());
            LOG.info("Getting mapping data for node is {}",inputNode.getMapping().stream().filter(o -> o.getKey()
                            .equals(new MappingKey(destTpId))).findFirst().toString());
            // If node type is transponder
            if (inputNode.getNodeType() != null && inputNode.getNodeType().equals(NodeTypes.Xpdr)) {
                // If its A-End transponder
                if (destTpId.toLowerCase().contains("network")) {
                    java.util.Optional<Mapping> mappingObject = inputNode.getMapping().stream().filter(o -> o.getKey()
                            .equals(new MappingKey(destTpId))).findFirst();
                    if (mappingObject.isPresent()) {
                        Map<String, Double> txPowerRangeMap = getXponderPowerRange(nodeId, mappingObject.get()
                                .getSupportingCircuitPackName(),
                                mappingObject.get().getSupportingPort(),deviceDb);
                        if (!txPowerRangeMap.isEmpty()) {
                            LOG.info("Transponder range exists for nodeId: {}", nodeId);
                            String srgId =  input.getNodes().get(i + 1).getSrcTp();
                            String nextNodeId = input.getNodes().get(i + 1).getNodeId();
                            DataBroker deviceDbSRG = getDeviceDataBroker(nextNodeId , mps);
                            Map<String, Double> rxSRGPowerRangeMap = getSRGRxPowerRange(nextNodeId, srgId, deviceDbSRG);
                            Double powerValue = new Double(0);
                            if (!rxSRGPowerRangeMap.isEmpty()) {
                                LOG.info("SRG Rx Power range exists for nodeId: {}", nodeId);
                                if (txPowerRangeMap.get("MaxTx").doubleValue()
                                        <= rxSRGPowerRangeMap.get("MaxRx").doubleValue()) {
                                    powerValue = txPowerRangeMap.get("MaxTx").doubleValue();
                                } else if (rxSRGPowerRangeMap.get("MaxRx").doubleValue()
                                        < txPowerRangeMap.get("MaxTx").doubleValue()) {
                                    powerValue = rxSRGPowerRangeMap.get("MaxRx").doubleValue();
                                }
                                LOG.info("Calculated Transponder Power value is {}" , powerValue);
                                if (setTransponderPower(nodeId, destTpId, destTpId + "-" + input.getWaveNumber(),
                                        new BigDecimal(powerValue), deviceDb)) {
                                    LOG.info("Transponder OCH connection: {} power updated ",
                                            destTpId + "-" + input.getWaveNumber());
                                    try {
                                        LOG.info("Now going in sleep mode");
                                        Thread.sleep(180000);
                                    } catch (InterruptedException e) {
                                        LOG.info("Transponder warmup failed for OCH connection: {}",
                                              destTpId + "-" + input.getWaveNumber(), e);
                                    }
                                } else {
                                    LOG.info("Transponder OCH connection: {} power update failed ",
                                           destTpId + "-" + input.getWaveNumber());
                                }
                            } else {
                                LOG.info("SRG Power Range not found");
                            }
                        } else {
                            LOG.info("Tranponder range not available seting to default "
                                    + "power for nodeId: {}", nodeId);
                            if (setTransponderPower(nodeId, destTpId, destTpId + "-" + input.getWaveNumber(),
                                    new BigDecimal(-5), deviceDb)) {
                                LOG.info("Transponder OCH connection: {} power updated ",
                                        destTpId + "-" + input.getWaveNumber());
                                try {
                                    Thread.sleep(180000);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    LOG.info("Transponder warmup failed for OCH connection: {}",
                                            destTpId + "-" + input.getWaveNumber(), e);
                                }
                            } else {
                                LOG.info("Transponder OCH connection: {} power update failed ",
                                         destTpId + "-" + input.getWaveNumber());
                            }
                        }
                    } else {
                        LOG.info("Mapping object not found for nodeId: {}", nodeId);
                        return false;
                    }
                } else {
                    LOG.info("{} is a drop node. Net power settings needed", nodeId);
                }
            } else if (inputNode.getNodeType() != null && inputNode.getNodeType().equals(NodeTypes.Rdm)) {
            // If Degree is transmitting end then set power
                if (destTpId.toLowerCase().contains("deg")) {
                    java.util.Optional<Mapping> mappingObject = inputNode.getMapping().stream().filter(o -> o.getKey()
                            .equals(new MappingKey(destTpId))).findFirst();
                    Mapping portMapping = mappingObject.get();
                    if (portMapping != null && deviceDb != null) {
                        BigDecimal spanLossTx = new OpenRoadmInterfaces(db, mps, nodeId, destTpId)
                                .getInterface(portMapping.getSupportingOts()).getAugmentation(Interface1.class).getOts()
                                .getSpanLossTransmit().getValue();
                        Double powerValue = Math.min(spanLossTx.doubleValue() - 9 , 2);
                        CrossConnect roadmCrossConnect = new CrossConnect(deviceDb, srcTpId
                               + "-" + destTpId + "-" + input.getWaveNumber());
                        try {
                            Boolean setXconnPowerSuccessVal = roadmCrossConnect.setPowerLevel(OpticalControlMode.Power,
                                    new PowerDBm(BigDecimal.valueOf(powerValue)));
                            if (setXconnPowerSuccessVal) {
                                LOG.info("Roadm-connection: {} updated ");
                                //TODO - commented code because one vendor is not supporting
                                //GainLoss with target-output-power
                                Thread.sleep(20000);
                                roadmCrossConnect.setPowerLevel(OpticalControlMode.GainLoss,
                                        new PowerDBm(BigDecimal.valueOf(powerValue)));
                            } else {
                                LOG.info("Set Power failed for Roadm-connection: {} on Node: {}",
                                        srcTpId + "-" + destTpId + "-" + input.getWaveNumber(), nodeId);
                                return false;
                            }
                        } catch (InterruptedException e) {
                            LOG.error("Olm-setPower wait failed {}",e);
                            return false;
                        }
                    }
                  // If Drop node leave node is power mode
                } else if (destTpId.toLowerCase().contains("srg")) {
                    LOG.info("Setting power at drop node");
                    CrossConnect roadmDropCrossConnect = new CrossConnect(deviceDb, srcTpId + "-"
                            + destTpId + "-" + input.getWaveNumber());
                    roadmDropCrossConnect.setPowerLevel(OpticalControlMode.Power, null);
                }
            }
        }
        return true;
    }

    /**
     * This methods turns down power a WL by performing
     * following steps:
     *
     * <p>
     * 1. Pull interfaces used in service and change
     * status to outOfService
     *
     * <p>
     * 2. For each of the ROADM node set target-output-power
     * to -60dbm, wait for 20 seconds, turn power mode to off
     *
     * <p>
     * 3. Turn down power in Z to A direction and A to Z
     *
     * @param input
     *            Input parameter from the olm servicePowerTurndown rpc
     *
     * @return true/false based on status of operation
     */
    public Boolean powerTurnDown(ServicePowerTurndownInput input) {
        LOG.info("Olm-powerTurnDown initiated");
        /**Starting with last element into the list Z -> A for
          turning down A -> Z **/
        for (int i = input.getNodes().size() - 1; i >= 0; i--) {
            String nodeId = input.getNodes().get(i).getNodeId();
            String srcTpId =  input.getNodes().get(i).getSrcTp();
            String destTpId = input.getNodes().get(i).getDestTp();
            Long wlNumber = input.getWaveNumber();
            DataBroker deviceDb = getDeviceDataBroker(nodeId , mps);
            if (!setInterfaceOutOfService(nodeId, srcTpId,
                    srcTpId + "-" + wlNumber, deviceDb)) {
                LOG.warn("Out of service status update failed for interface {} ",
                       srcTpId + "-" + wlNumber);
                return false;
            }
            if (!setInterfaceOutOfService(nodeId, destTpId,
                    destTpId + "-" + wlNumber, deviceDb)) {
                LOG.warn("Out of service status update failed for interface {} ",
                       destTpId + "-" + wlNumber);
                return false;
            }
            CrossConnect roadmCrossConnect = new CrossConnect(deviceDb, srcTpId
                    + "-" + destTpId + "-" + wlNumber);
            if (destTpId.toLowerCase().contains("srg")) {
                roadmCrossConnect.setPowerLevel(OpticalControlMode.Off, null);
            } else if (destTpId.toLowerCase().contains("deg")) {
                try {
                    if (!roadmCrossConnect.setPowerLevel(OpticalControlMode.Power,
                            new PowerDBm(new BigDecimal(-60)))) {
                        LOG.warn("Power down failed for Roadm-connection: {}", srcTpId
                                + "-" + destTpId + "-" + wlNumber);
                        return false;
                    }
                    Thread.sleep(20000);
                    if (!roadmCrossConnect.setPowerLevel(OpticalControlMode.Off,
                            null)) {
                        LOG.warn("Setting power-control mode off failed for Roadm-connection: {}",
                                srcTpId + "-" + destTpId + "-" + wlNumber);
                        return false;
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Olm-powerTurnDown wait failed {}",e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method updates interface administrative state to
     * outOfService.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param tpId
     *            Termination point of mounted netconf - node
     * @param interfaceName
     *            Name of interface which needs status update
     * @param deviceDb
     *            Reference to device data broker
     * @return true/false based on status of operation
     */
    public Boolean setInterfaceOutOfService(String nodeId, String tpId,
            String interfaceName, DataBroker deviceDb) {
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier
                .create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        Interface nodeInterface = new OpenRoadmInterfaces(db, mps, nodeId, tpId)
                .getInterface(interfaceName);
        InterfaceBuilder intfBuilder = new InterfaceBuilder(nodeInterface);
        intfBuilder.setAdministrativeState(AdminStates.OutOfService);
        final WriteTransaction writeTransaction = deviceDb.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, intfBuilder.build());
        final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
        try {
            submit.checkedGet();
            LOG.info("Successfully posted interface {}" , interfaceName);
            return true;
        } catch (TransactionCommitFailedException ex) {
            LOG.warn("Failed to post {} ", interfaceName ,ex);
            return false;
        }
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
            LOG.info("Device Data broker not found for :" + nodeId);
            return null;
        }
    }

    /**
     * This static method returns the Mountpoint for a netconf node.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param mps
     *            Reference to mount service
     * @return MountPoint for the given device
     */
    public static MountPoint getDeviceMountPoint(String nodeId, MountPointService mps) {
        final Optional<MountPoint> netconfNodeOptional = mps.getMountPoint(NETCONF_TOPO_IID
                .child(Node.class, new NodeKey(new NodeId(nodeId))));
        // Get mount point for specified device
        if (netconfNodeOptional.isPresent()) {
            MountPoint netconfNode = netconfNodeOptional.get();
            return netconfNode;
        } else {
            LOG.info("Mount Point not found for :" + nodeId);
            return null;
        }

    }

    /**
     * This static method returns the DataBroker for a netconf node.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param mps
     *            Reference to mount service
     * @param db
     *            Databroker
     * @return Nodes from portMapping for given nodeId
     */
    public static Nodes getNode(String nodeId, MountPointService mps, DataBroker db) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
                .child(Nodes.class, new NodesKey(nodeId));
        ReadOnlyTransaction readTransaction = db.newReadOnlyTransaction();
        Optional<Nodes> nodeObject;
        try {
            nodeObject = readTransaction.read(LogicalDatastoreType.CONFIGURATION, nodesIID).get();
            if (nodeObject.isPresent()) {
                LOG.info("Found Node in Portmapping for nodeId {}", nodeObject.get().getNodeId());
                return nodeObject.get();
            } else {
                LOG.info("Could not find Portmapping for nodeId {}", nodeId);
                return null;
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.info("Unable to read Portmapping for nodeId {}", nodeId, ex);
        }
        return null;
    }

    /**
     * This method provides Transponder transmit power range.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param circuitPackName
     *            Transponder circuitPack name
     * @param portName
     *            Transponder port name
     * @param deviceDb
     *            Databroker for the given device
     * @return HashMap holding Min and Max transmit power for given port
     */
    public Map<String, Double> getXponderPowerRange(String nodeId, String circuitPackName, String portName,
            DataBroker deviceDb) {
        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                CircuitPacks.class, new CircuitPacksKey(circuitPackName)).child(Ports.class, new PortsKey(portName));
        ReadOnlyTransaction readTransaction = deviceDb.newReadOnlyTransaction();
        Map<String, Double> powerRangeMap = new HashMap<String, Double>();
        try {
            LOG.info("Fetching logical Connection Point value for port " + portName + " at circuit pack "
                + circuitPackName);
            Optional<Ports> portObject = readTransaction.read(LogicalDatastoreType.OPERATIONAL, portIID).get();
            if (portObject.isPresent()) {
                Ports port = portObject.get();
                if (port.getTransponderPort() != null) {
                    powerRangeMap.put("MaxTx", port.getTransponderPort().getPortPowerCapabilityMaxTx()
                            .getValue().doubleValue());
                    powerRangeMap.put("MinTx", port.getTransponderPort().getPortPowerCapabilityMinTx()
                            .getValue().doubleValue());
                } else {
                    LOG.warn("Logical Connection Point value missing for " + circuitPackName + " " + port
                        .getPortName());
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Read failed for Logical Connection Point value missing for " + circuitPackName + " "
                + portName,ex);
            return powerRangeMap;
        }
        return powerRangeMap;
    }

    /**
     * This method provides Transponder transmit power range.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param srgId
     *            SRG Id connected to transponder
     * @param deviceDb
     *            Databroker for the given device
     * @return HashMap holding Min and Max transmit power for given port
     */
    public Map<String, Double> getSRGRxPowerRange(String nodeId, String srgId, DataBroker deviceDb) {
        Map<String, Double> powerRangeMap = new HashMap<String, Double>();
        LOG.info("Coming inside Xpdr power range");
        java.util.Optional<Mapping> mappingSRGObject = getNode(nodeId, mps, db).getMapping()
                .stream().filter(o -> o.getKey()
                .equals(new MappingKey(srgId))).findFirst();
        if (mappingSRGObject.isPresent()) {
            LOG.info("Mapping object exists.");
            String circuitPackName = mappingSRGObject.get().getSupportingCircuitPackName();
            String portName = mappingSRGObject.get().getSupportingPort();
            InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
                    .child(Ports.class, new PortsKey(portName));
            try {
                LOG.info("Fetching logical Connection Point value for port " + portName + " at circuit pack "
                    + circuitPackName + portIID);
                ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
                Optional<Ports> portObject = rtx.read(LogicalDatastoreType.OPERATIONAL, portIID).get();
                if (portObject.isPresent()) {
                    Ports port = portObject.get();
                    if (port.getRoadmPort() != null) {
                        LOG.info("Port found on the node ID");
                        powerRangeMap.put("MinRx", port.getRoadmPort()
                                .getPortPowerCapabilityMinRx().getValue().doubleValue());
                        powerRangeMap.put("MaxRx", port.getRoadmPort()
                                .getPortPowerCapabilityMaxRx().getValue().doubleValue());
                        return powerRangeMap;
                    } else {
                        LOG.warn("Roadm ports power value is missing for " + circuitPackName + " " + port
                            .getPortName());
                    }
                } else {
                    LOG.info("Port not found");
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Read failed for Logical Connection Point value missing for " + circuitPackName + " "
                    + portName,ex);
            }
        } else {
            LOG.info("Port mapping not found for nodeId: {} and srgId: {} ",
                    nodeId, srgId);
        }
        return powerRangeMap;

    }

    /**
     * This method retrieves transponder OCH interface and
     * sets power.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param tpId
     *            Termination port for transponder connected to SRG
     * @param interfaceName
     *            OCH interface name carrying WL
     * @param txPower
     *            Calculated transmit power
     * @param deviceDb
     *            Databroker for the given device
     * @return true/false based on status of operation
     */
    public boolean setTransponderPower(String nodeId, String tpId, String interfaceName, BigDecimal txPower,
            DataBroker deviceDb) {
        LOG.info("Setting target-power for transponder nodeId: {} InterfaceName: {}",
                nodeId, interfaceName);
        InterfaceBuilder ochInterfaceBuilder = new InterfaceBuilder(
                new OpenRoadmInterfaces(db, mps, nodeId, tpId)
                   .getInterface(interfaceName));
        OchBuilder ochBuilder = new OchBuilder(ochInterfaceBuilder
                .getAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                        .channel.interfaces.rev161014.Interface1.class).getOch());
        ochBuilder.setTransmitPower(new PowerDBm(txPower));
        ochInterfaceBuilder.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                .channel.interfaces.rev161014.Interface1.class,
                new Interface1Builder().setOch(ochBuilder.build()).build());
        ReadWriteTransaction rwtx = deviceDb.newReadWriteTransaction();
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        rwtx.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, ochInterfaceBuilder.build());
        CheckedFuture<Void, TransactionCommitFailedException> submit = rwtx.submit();
        try {
            submit.checkedGet();
            LOG.info("Power update is submitted");
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.info("Setting transponder power failed {}" ,e);
        }
        return false;
    }

}