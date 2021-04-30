/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.util.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.NbrList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R2RLinkDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(R2RLinkDiscovery.class);

    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final DeviceTransactionManager deviceTransactionManager;

    public R2RLinkDiscovery(final DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        NetworkTransactionService networkTransactionService) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.networkTransactionService = networkTransactionService;
    }

    public boolean readLLDP(NodeId nodeId, String nodeVersion) {

        if (nodeVersion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            InstanceIdentifier<Protocols> protocolsIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(Protocols.class);
            Optional<Protocols> protocolObject = this.deviceTransactionManager.getDataFromDevice(nodeId.getValue(),
                LogicalDatastoreType.OPERATIONAL, protocolsIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!protocolObject.isPresent() || (protocolObject.get().augmentation(Protocols1.class) == null)) {
                LOG.warn("LLDP subtree is missing : isolated openroadm device");
                return false;
            }
            NbrList nbrList = protocolObject.get().augmentation(Protocols1.class).getLldp().getNbrList();
            LOG.info("LLDP subtree is present. Device has {} neighbours", nbrList.getIfName().size());
            boolean success = true;
            for (IfName ifName : nbrList.nonnullIfName().values()) {
                if (ifName.getRemoteSysName() == null) {
                    LOG.warn("LLDP subtree neighbour is empty for nodeId: {}, ifName: {}",
                        nodeId.getValue(),ifName.getIfName());
                } else {
                    Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName
                        .getRemoteSysName());
                    if (!mps.isPresent()) {
                        LOG.warn("Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                        // The controller raises a warning rather than an error because the first node to
                        // mount cannot see its neighbors yet. The link will be detected when processing
                        // the neighbor node.
                    } else {
                        if (!createR2RLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(),
                            ifName.getRemotePortId())) {
                            LOG.error("Link Creation failed between {} and {} nodes.", nodeId.getValue(),
                                ifName.getRemoteSysName());
                            success = false;
                        }
                    }
                }
            }
            return success;
        }
        else if (nodeVersion.equals(OPENROADM_DEVICE_VERSION_2_2_1)) {
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                .container.org.openroadm.device.Protocols> protocolsIID = InstanceIdentifier.create(org.opendaylight
                .yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
                .OrgOpenroadmDevice.class).child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.Protocols.class);
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                .container.org.openroadm.device.Protocols> protocolObject = this.deviceTransactionManager
                .getDataFromDevice(nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocolsIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!protocolObject.isPresent() || (protocolObject.get().augmentation(org.opendaylight.yang.gen.v1.http.org
                .openroadm.lldp.rev181019.Protocols1.class) == null)) {
                LOG.warn("LLDP subtree is missing : isolated openroadm device");
                return false;
            }
            org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.@Nullable NbrList nbrList
                = protocolObject.get().augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019
                .Protocols1.class).getLldp().getNbrList();
            LOG.info("LLDP subtree is present. Device has {} neighbours", nbrList.getIfName().size());
            boolean success = true;
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.nbr.list.IfName
                ifName : nbrList.nonnullIfName().values()) {
                if (ifName.getRemoteSysName() == null) {
                    LOG.warn("LLDP subtree neighbour is empty for nodeId: {}, ifName: {}",
                        nodeId.getValue(),ifName.getIfName());
                } else {
                    Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName
                        .getRemoteSysName());
                    if (!mps.isPresent()) {
                        LOG.warn("Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                        // The controller raises a warning rather than an error because the first node to
                        // mount cannot see its neighbors yet. The link will be detected when processing
                        // the neighbor node.
                    } else {
                        if (!createR2RLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(),
                            ifName.getRemotePortId())) {
                            LOG.error("Link Creation failed between {} and {} nodes.", nodeId, ifName
                                .getRemoteSysName());
                            success = false;
                        }
                    }
                }
            }
            return success;
        }
        else {
            LOG.error("Unable to read LLDP data for unmanaged openroadm device version");
            return false;
        }
    }

    public Direction getDegreeDirection(Integer degreeCounter, NodeId nodeId) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue())).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodesObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodesIID).get();
            if (nodesObject.isPresent() && (nodesObject.get().getMapping() != null)) {
                Collection<Mapping> mappingList = nodesObject.get().nonnullMapping().values();
                mappingList = mappingList.stream().filter(mp -> mp.getLogicalConnectionPoint().contains("DEG"
                    + degreeCounter)).collect(Collectors.toList());
                if (mappingList.size() == 1) {
                    return Direction.Bidirectional;
                } else if (mappingList.size() > 1) {
                    return Direction.Tx;
                } else {
                    return Direction.NotApplicable;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
        }
        return Direction.NotApplicable;
    }

    public boolean createR2RLink(NodeId nodeId, String interfaceName, String remoteSystemName,
                                 String remoteInterfaceName) {
        String srcTpTx = null;
        String srcTpRx = null;
        String destTpTx = null;
        String destTpRx = null;
        // Find which degree is associated with ethernet interface
        Integer srcDegId = getDegFromInterface(nodeId, interfaceName);
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
        Integer destDegId = getDegFromInterface(destNodeId, remoteInterfaceName);
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
        r2rlinkBuilderAToZ.setRdmANode(nodeId.getValue()).setDegANum(Uint8.valueOf(srcDegId))
            .setTerminationPointA(srcTpTx).setRdmZNode(destNodeId.getValue()).setDegZNum(Uint8.valueOf(destDegId))
            .setTerminationPointZ(destTpRx);
        if (!OrdLink.createRdm2RdmLinks(r2rlinkBuilderAToZ.build(), this.dataBroker)) {
            LOG.error("OMS Link creation failed between node: {} and nodeId: {} in A->Z direction", nodeId.getValue(),
                destNodeId.getValue());
            return false;
        }
        // Z->A
        LOG.debug(
            "Found a neighbor SrcNodeId: {} , SrcDegId: {}"
                + ", SrcTPId: {}, DestNodeId:{} , DestDegId: {}, DestTPId: {}",
            destNodeId, destDegId, destTpTx, nodeId.getValue(), srcDegId, srcTpRx);

        InitRoadmNodesInputBuilder r2rlinkBuilderZToA = new InitRoadmNodesInputBuilder()
            .setRdmANode(destNodeId.getValue())
            .setDegANum(Uint8.valueOf(destDegId))
            .setTerminationPointA(destTpTx)
            .setRdmZNode(nodeId.getValue())
            .setDegZNum(Uint8.valueOf(srcDegId))
            .setTerminationPointZ(srcTpRx);
        if (!OrdLink.createRdm2RdmLinks(r2rlinkBuilderZToA.build(), this.dataBroker)) {
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
        Integer srcDegId = getDegFromInterface(nodeId, interfaceName);
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
        Integer destDegId = getDegFromInterface(destNodeId, remoteInterfaceName);
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
        return TopologyUtils.deleteLink(nodeId.getValue() + "-" + srcDegId, destNodeId.getValue() + "-" + destDegId,
            srcTpTx, destTpRx, networkTransactionService)
            && TopologyUtils.deleteLink(destNodeId.getValue() + "-" + destDegId, nodeId.getValue() + "-" + srcDegId,
                destTpTx, srcTpRx, networkTransactionService);
    }

    private Integer getDegFromInterface(NodeId nodeId, String interfaceName) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue())).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodesObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodesIID).get();
            if (nodesObject.isPresent() && (nodesObject.get().getCpToDegree() != null)) {
                Collection<CpToDegree> cpToDeg = nodesObject.get().nonnullCpToDegree().values();
                Stream<CpToDegree> cpToDegStream = cpToDeg.stream().filter(cp -> cp.getInterfaceName() != null)
                    .filter(cp -> cp.getInterfaceName().equals(interfaceName));
                if (cpToDegStream != null) {
                    @SuppressWarnings("unchecked") Optional<CpToDegree> firstCpToDegree = cpToDegStream.findFirst();
                    if (firstCpToDegree.isPresent() && (firstCpToDegree != null)) {
                        LOG.debug("Found and returning {}",firstCpToDegree.get().getDegreeNumber().intValue());
                        return firstCpToDegree.get().getDegreeNumber().intValue();
                    } else {
                        LOG.debug("Not found so returning nothing");
                        return null;
                    }
                } else {
                    LOG.warn("CircuitPack stream couldnt find anything for nodeId: {} and interfaceName: {}",
                        nodeId.getValue(),interfaceName);
                }
            } else {
                LOG.warn("Could not find mapping for Interface {} for nodeId {}", interfaceName,
                    nodeId.getValue());
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for Interface : {} for nodeId {}", interfaceName, nodeId, ex);
        }
        return null;
    }
}
