/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.NbrList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegreeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R2RLinkDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(R2RLinkDiscovery.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmTopology openRoadmTopology;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    public R2RLinkDiscovery(final DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
            OpenRoadmTopology openRoadmTopology, OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmTopology = openRoadmTopology;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public boolean readLLDP(NodeId nodeId) {
        InstanceIdentifier<Protocols> protocolsIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Protocols.class);
        Optional<Protocols> protocolObject = this.deviceTransactionManager.getDataFromDevice(nodeId.getValue(),
                LogicalDatastoreType.OPERATIONAL, protocolsIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent() || (protocolObject.get().getAugmentation(Protocols1.class) == null)) {
            LOG.warn("LLDP subtree is missing : isolated openroadm device");
            return false;
        }
        NbrList nbrList = protocolObject.get().getAugmentation(Protocols1.class).getLldp().getNbrList();
        LOG.info("LLDP subtree is present. Device has {} neighbours", nbrList.getIfName().size());
        for (IfName ifName : nbrList.getIfName()) {
            if (ifName.getRemoteSysName() == null) {
                LOG.error("LLDP subtree is empty in the device for nodeId: {}", nodeId.getValue());
                return false;
            }
            Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName.getRemoteSysName());
            if (!mps.isPresent()) {
                LOG.warn("Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                // The controller raises a warning rather than an error because the first node to
                // mount cannot see its neighbors yet. The link will be detected when processing
                // the neighbor node.
            } else {
                if (!createR2RLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(), ifName.getRemotePortId())) {
                    LOG.error("Link Creation failed between {} and {} nodes.", nodeId, ifName.getRemoteSysName());
                    return false;
                }
            }
        }
        return true;
    }

    public Direction getDegreeDirection(Integer degreeCounter, NodeId nodeId) {
        InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Degree.class,
                new DegreeKey(degreeCounter));
        Optional<Degree> degreeObject = this.deviceTransactionManager.getDataFromDevice(nodeId.getValue(),
                LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (degreeObject.isPresent()) {
            Integer connectionPortCount = degreeObject.get().getConnectionPorts().size();
            if (connectionPortCount == 1) {
                return Direction.Bidirectional;
            } else if (connectionPortCount > 1) {
                return Direction.Tx;
            } else {
                return Direction.NotApplicable;
            }
        } else {
            LOG.error("Couldnt retrieve Degree object for nodeId: {} and DegreeNumbner: {}", nodeId.getValue(),
                    degreeCounter);
            return Direction.NotApplicable;
        }
    }

    public boolean createR2RLink(NodeId nodeId, String interfaceName, String remoteSystemName,
            String remoteInterfaceName) {
        String srcTpTx = null;
        String srcTpRx = null;
        String destTpTx = null;
        String destTpRx = null;
        // Find which degree is associated with ethernet interface
        // portmapping.getDegFromCP(nodeId,interfaceName);
        Integer srcDegId = getDegFromCP(nodeId, interfaceName);
        if (srcDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return false;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting
        // number of
        // circuit-packs under degree subtree
        Direction sourceDirection = getDegreeDirection(srcDegId, nodeId);
        if (Direction.NotApplicable == sourceDirection) {
            LOG.error("Couldnt find degree direction for nodeId: {} and degree: {}", nodeId, srcDegId);
            return false;
        } else if (Direction.Bidirectional == sourceDirection) {
            srcTpTx = "DEG" + srcDegId + "-TTP-TXRX";
            srcTpRx = "DEG" + srcDegId + "-TTP-TXRX";
        } else {
            srcTpTx = "DEG" + srcDegId + "-TTP-TX";
            srcTpRx = "DEG" + srcDegId + "-TTP-RX";
        }
        // Find degree for which Ethernet interface is created on other end
        NodeId destNodeId = new NodeId(remoteSystemName);
        Integer destDegId = getDegFromCP(destNodeId, remoteInterfaceName);
        // portmapping.getDegFromCP(nodeId,interfaceName);
        if (destDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return false;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting
        // number of
        // circuit-packs under degree subtree
        Direction destinationDirection = getDegreeDirection(destDegId, destNodeId);
        if (Direction.NotApplicable == destinationDirection) {
            LOG.error("Couldnt find degree direction for nodeId: {} and degree: {}", destNodeId, destDegId);
            return false;
        } else if (Direction.Bidirectional == destinationDirection) {
            destTpTx = "DEG" + destDegId + "-TTP-TXRX";
            destTpRx = "DEG" + destDegId + "-TTP-TXRX";
        } else {
            destTpTx = "DEG" + destDegId + "-TTP-TX";
            destTpRx = "DEG" + destDegId + "-TTP-RX";
        }
        // A->Z
        LOG.debug(
            "Found a neighbor SrcNodeId: {} , SrcDegId: {} , SrcTPId: {}, DestNodeId:{} , DestDegId: {}, DestTPId: {}",
                nodeId.getValue(), srcDegId, srcTpTx, destNodeId, destDegId, destTpRx);
        InitRoadmNodesInputBuilder r2rlinkBuilderAToZ = new InitRoadmNodesInputBuilder();
        r2rlinkBuilderAToZ.setRdmANode(nodeId.getValue()).setDegANum(srcDegId.shortValue())
                .setTerminationPointA(srcTpTx).setRdmZNode(destNodeId.getValue()).setDegZNum(destDegId.shortValue())
                .setTerminationPointZ(destTpRx);
        if (!OrdLink.createRdm2RdmLinks(r2rlinkBuilderAToZ.build(), openRoadmTopology, dataBroker)) {
            LOG.error("OMS Link creation failed between node: {} and nodeId: {} in A->Z direction", nodeId.getValue(),
                    destNodeId.getValue());
            return false;
        }
        // Z->A
        LOG.debug(
                "Found a neighbor SrcNodeId: {} , SrcDegId: {}"
                        + ", SrcTPId: {}, DestNodeId:{} , DestDegId: {}, DestTPId: {}",
                destNodeId, destDegId, destTpTx, nodeId.getValue(), srcDegId, srcTpRx);

        InitRoadmNodesInputBuilder r2rlinkBuilderZToA = new InitRoadmNodesInputBuilder();
        r2rlinkBuilderZToA.setRdmANode(destNodeId.getValue()).setDegANum(destDegId.shortValue())
                .setTerminationPointA(destTpTx).setRdmZNode(nodeId.getValue()).setDegZNum(srcDegId.shortValue())
                .setTerminationPointZ(srcTpRx);
        if (!OrdLink.createRdm2RdmLinks(r2rlinkBuilderZToA.build(), openRoadmTopology, dataBroker)) {
            LOG.error("OMS Link creation failed between node: {} and nodeId: {} in Z->A direction",
                    destNodeId.getValue(), nodeId.getValue());
            return false;
        }
        return true;
    }

    public boolean deleteR2RLink(NodeId nodeId, String interfaceName, String remoteSystemName,
            String remoteInterfaceName) {
        String srcTpTx = null;
        String srcTpRx = null;
        String destTpTx = null;
        String destTpRx = null;
        // Find which degree is associated with ethernet interface
        // portmapping.getDegFromCP(nodeId,interfaceName);
        Integer srcDegId = getDegFromCP(nodeId, interfaceName);
        if (srcDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return false;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting number of
        // circuit-packs under degree subtree
        Direction sourceDirection = getDegreeDirection(srcDegId, nodeId);
        if (Direction.NotApplicable == sourceDirection) {
            LOG.error("Couldnt find degree direction for nodeId: {} and degree: {}", nodeId, srcDegId);
            return false;
        } else if (Direction.Bidirectional == sourceDirection) {
            srcTpTx = "DEG" + srcDegId + "-TTP-TXRX";
            srcTpRx = "DEG" + srcDegId + "-TTP-TXRX";
        } else {
            srcTpTx = "DEG" + srcDegId + "-TTP-TX";
            srcTpRx = "DEG" + srcDegId + "-TTP-RX";
        }
        // Find degree for which Ethernet interface is created on other end
        NodeId destNodeId = new NodeId(remoteSystemName);
        // portmapping.getDegFromCP(nodeId,interfaceName);
        Integer destDegId = getDegFromCP(destNodeId, remoteInterfaceName);
        if (destDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return false;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting number of
        // circuit-packs under degree subtree
        Direction destinationDirection = getDegreeDirection(destDegId, destNodeId);
        if (Direction.NotApplicable == destinationDirection) {
            LOG.error("Couldnt find degree direction for nodeId: {} and degree: {}", destNodeId, destDegId);
            return false;
        } else if (Direction.Bidirectional == destinationDirection) {
            destTpTx = "DEG" + destDegId + "-TTP-TXRX";
            destTpRx = "DEG" + destDegId + "-TTP-TXRX";
        } else {
            destTpTx = "DEG" + destDegId + "-TTP-TX";
            destTpRx = "DEG" + destDegId + "-TTP-RX";
        }
        return this.openRoadmTopology.deleteLink(nodeId.getValue(), destNodeId.getValue(), srcTpTx, destTpRx)
                && this.openRoadmTopology.deleteLink(destNodeId.getValue(), nodeId.getValue(), destTpTx, srcTpRx);
    }

    private CpToDegree getCPtoDegreeMapping(NodeId nodeId, String circuitPackName) {
        InstanceIdentifier<CpToDegree> cptoDegMappingIID = InstanceIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId.getValue()))
                .child(CpToDegree.class, new CpToDegreeKey(circuitPackName)).build();
        LOG.debug("Input parameters are {},{}", nodeId.getValue(), circuitPackName);
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<CpToDegree> cptpDegObject = readTx.read(LogicalDatastoreType.CONFIGURATION, cptoDegMappingIID)
                    .get().toJavaUtil();
            if (cptpDegObject.isPresent()) {
                CpToDegree cpToDeg = cptpDegObject.get();
                LOG.debug("Found mapping for the Circuit Pack {}. Degree: {}", circuitPackName, cpToDeg);
                return cpToDeg;
            } else {
                LOG.warn("Could not find mapping for Circuit Pack {} for nodeId {}", circuitPackName,
                        nodeId.getValue());
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for circuit pack : {} for nodeId {}", circuitPackName, nodeId, ex);
        }
        return null;
    }

    private Integer getDegFromParentCP(NodeId nodeId, String interfaceName, String supportingCircuitPack) {
        InstanceIdentifier<CircuitPacks> circuitPackIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(supportingCircuitPack));
        Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(nodeId.getValue(),
                LogicalDatastoreType.OPERATIONAL, circuitPackIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!circuitPackObject.isPresent()
                || (circuitPackObject.get().getParentCircuitPack().getCircuitPackName() == null)) {
            LOG.warn("Parent circuitpack not found for NodeId: {} and Interface: {}", nodeId, interfaceName);
            return null;
        }
        String parentCP = circuitPackObject.get().getParentCircuitPack().getCircuitPackName();
        CpToDegree cpToDegree = getCPtoDegreeMapping(nodeId, parentCP);
        if (cpToDegree == null) {
            LOG.error("CP to Degree mapping not found even with parent circuitpack for NodeID: {}" + "and Interface {}",
                    nodeId, interfaceName);
            return null;
        } else {
            LOG.debug("CP to degree is {}", cpToDegree.getDegreeNumber());
            return cpToDegree.getDegreeNumber().intValue();
        }
    }

    private Integer getDegFromCP(NodeId nodeId, String interfaceName) {
        try {
            java.util.Optional<Interface> interfaceOpt = this.openRoadmInterfaces.getInterface(nodeId.getValue(),
                    interfaceName);
            if (!interfaceOpt.isPresent()) {
                LOG.warn("Interface with {} on node {} was not found!", interfaceName, nodeId.getValue());
                return null;
            }
            String supportingCircuitPack = interfaceOpt.get().getSupportingCircuitPackName();
            LOG.debug("Supporting circuitpack name is :{}", interfaceOpt.get().getSupportingCircuitPackName());
            CpToDegree cpToDegree = getCPtoDegreeMapping(nodeId, supportingCircuitPack);
            // Currently devices have different ways to represent connection to Ethernet port
            // and degree port.
            // If Circuit pack is not present under degree tree then read parent CP of given
            // CP (Circuit pack).
            if (cpToDegree != null) {
                return cpToDegree.getDegreeNumber().intValue();
            } else {
                return getDegFromParentCP(nodeId, interfaceName, supportingCircuitPack);
            }
        } catch (OpenRoadmInterfaceException ex) {
            LOG.error("Failed to get source interface {} from node {}!", interfaceName, nodeId, ex);
            return null;
        }
    }
}
