/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_7_1;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.util.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.InitRoadmNodesInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that manages the WDM ROADM-to-ROADM links in the openroadm-topology.
 */
public class R2RLinkDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(R2RLinkDiscovery.class);

    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final DeviceTransactionManager deviceTransactionManager;

    /**
     * Instantiate the R2RLinkDiscovery object.
     *
     * @param dataBroker Provides access to the conceptual data tree store
     * @param deviceTransactionManager Manages data transactions with the netconf devices
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     */
    public R2RLinkDiscovery(final DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        NetworkTransactionService networkTransactionService) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * Depending on the org-openroadm-device version, get from the device relevant information concerning the node
     * neighbors.
     *
     * @param nodeId Node name
     * @param nodeVersion org-openroadm-device version
     * @return True if the node has at least one neighbor. False otherwise.
     */
    public boolean readLLDP(NodeId nodeId, String nodeVersion) {
        switch (nodeVersion) {
            case OPENROADM_DEVICE_VERSION_2_2_1:
                DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm
                        .device.container.org.openroadm.device.Protocols> protocols221IID = DataObjectIdentifier
                    .builderOfInherited(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData.class,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
                            .OrgOpenroadmDevice.class)
                    .child(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
                            .org.openroadm.device.Protocols.class)
                    .build();
                var protocol221Object = this.deviceTransactionManager
                    .getDataFromDevice(nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocols221IID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (hasNoNeighbor221(protocol221Object)) {
                    LOG.warn("LLDP subtree is missing or incomplete: isolated openroadm device");
                    return false;
                }
                var nbr221List = protocol221Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1.class)
                    .getLldp().getNbrList();
                LOG.info("LLDP subtree is present. Device has {} neighbours", nbr221List.getIfName().size());
                return rdm2rdmLinkCreatedv221(nodeId, nbr221List);
            case OPENROADM_DEVICE_VERSION_7_1:
                DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                        .org.openroadm.device.container.org.openroadm.device.Protocols> protocols71IID =
                    DataObjectIdentifier.builderOfInherited(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceData.class,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container
                                .OrgOpenroadmDevice.class)
                    .child(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container
                                .org.openroadm.device.Protocols.class)
                    .build();
                var protocols71Object = this.deviceTransactionManager
                        .getDataFromDevice(nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocols71IID,
                                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (hasNoNeighbor71(protocols71Object)) {
                    LOG.warn("LLDP subtree is missing or incomplete: isolated openroadm device");
                    return false;
                }
                var nbr71List = protocols71Object.orElseThrow().augmentation(
                                org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev200529.Protocols1.class)
                        .getLldp().getNbrList();
                LOG.info("LLDP subtree is present. Device has {} neighbours", nbr71List.getIfName().size());
                return rdm2rdmLinkCreatedv71(nodeId, nbr71List);
            default:
                LOG.error("Unable to read LLDP data for unmanaged openroadm device version");
                return false;
        }
    }

    private boolean hasNoNeighbor221(Optional<
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org
                    .openroadm.device.Protocols> protocol221Object) {
        return protocol221Object.isEmpty()
                || protocol221Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1.class) == null
                || protocol221Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1.class)
                    .getLldp() == null
                || protocol221Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1.class)
                    .getLldp().getNbrList() == null;
    }

    private boolean hasNoNeighbor71(Optional<
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org
                    .openroadm.device.Protocols> protocol71Object) {
        return protocol71Object.isEmpty()
                || protocol71Object.orElseThrow().augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev200529.Protocols1.class) == null
                || protocol71Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev200529.Protocols1.class)
                .getLldp() == null
                || protocol71Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev200529.Protocols1.class)
                .getLldp().getNbrList() == null;
    }

    private boolean rdm2rdmLinkCreatedv71(NodeId nodeId,
            org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev200529.lldp.container.lldp.NbrList nbrList) {
        boolean success = true;
        for (org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev200529.lldp.container.lldp.nbr.list.IfName
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

    private boolean rdm2rdmLinkCreatedv221(NodeId nodeId,
            org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.NbrList nbrList) {
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

    /**
     * TX and RX TTP termination points of a degree. Both are the TXRX TP for a bidirectional degree.
     */
    private record DegreeTtps(String tx, String rx) {
    }

    /**
     * Get the TTP termination points of a degree, as declared in the port mapping of the node.
     *
     * @param nodeId Node name
     * @param degreeNb Number of the degree
     * @return TX and RX TTPs of the degree, or null if the port mapping has neither a bidirectional TTP nor both
     *     unidirectional TTPs for this degree
     */
    private DegreeTtps getDegreeTtps(NodeId nodeId, Integer degreeNb) {
        DataObjectIdentifier<Nodes> nodesIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue()))
            .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodesObject = Optional.ofNullable(readTx.read(LogicalDatastoreType.CONFIGURATION, nodesIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS))
                    .orElse(Optional.empty());
            if (nodesObject.isPresent()) {
                Map<MappingKey, Mapping> mappings = nodesObject.orElseThrow().nonnullMapping();
                String ttpPrefix = "DEG" + degreeNb + "-TTP-";
                String txrx = ttpPrefix + "TXRX";
                if (mappings.containsKey(new MappingKey(txrx))) {
                    return new DegreeTtps(txrx, txrx);
                }
                String tx = ttpPrefix + "TX";
                String rx = ttpPrefix + "RX";
                if (mappings.containsKey(new MappingKey(tx)) && mappings.containsKey(new MappingKey(rx))) {
                    return new DegreeTtps(tx, rx);
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed getting Mapping data from portMapping", e);
            return null;
        }
        LOG.error("Couldnt find TTP of degree {} in port mapping of nodeId: {}", degreeNb, nodeId.getValue());
        return null;
    }

    /**
     * Create a ROADM-to-ROADM link when a ROADM node has a neighbor declared in its configuration.
     *
     * @param nodeId Node name
     * @param interfaceName Name of the WDM line interface
     * @param remoteSystemName Name of the neighbor node
     * @param remoteInterfaceName Name of the WDM line interface on the neighbor node
     * @return True if the links are correctly created, False otherwise
     */
    public boolean createR2RLink(NodeId nodeId, String interfaceName, String remoteSystemName,
                                 String remoteInterfaceName) {
        // Find which degree is associated with ethernet interface
        Integer srcDegId = getDegFromInterface(nodeId, interfaceName);
        if (srcDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return false;
        }
        DegreeTtps srcTtps = getDegreeTtps(nodeId, srcDegId);
        if (srcTtps == null) {
            return false;
        }
        // Find degree for which Ethernet interface is created on other end
        NodeId destNodeId = new NodeId(remoteSystemName);
        Integer destDegId = getDegFromInterface(destNodeId, remoteInterfaceName);
        if (destDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", destNodeId);
            return false;
        }
        DegreeTtps destTtps = getDegreeTtps(destNodeId, destDegId);
        if (destTtps == null) {
            return false;
        }
        // A->Z
        LOG.debug(
            "Found a neighbor SrcNodeId: {} , SrcDegId: {} , SrcTPId: {}, DestNodeId:{} , DestDegId: {}, DestTPId: {}",
            nodeId.getValue(), srcDegId, srcTtps.tx(), destNodeId, destDegId, destTtps.rx());
        InitRoadmNodesInputBuilder r2rlinkBuilderAToZ = new InitRoadmNodesInputBuilder();
        r2rlinkBuilderAToZ.setRdmANode(nodeId.getValue()).setDegANum(Uint8.valueOf(srcDegId))
            .setTerminationPointA(srcTtps.tx()).setRdmZNode(destNodeId.getValue())
            .setDegZNum(Uint8.valueOf(destDegId)).setTerminationPointZ(destTtps.rx());
        if (!OrdLink.createRdm2RdmLinks(r2rlinkBuilderAToZ.build(), this.dataBroker)) {
            LOG.error("OMS Link creation failed between node: {} and nodeId: {} in A->Z direction", nodeId.getValue(),
                destNodeId.getValue());
            return false;
        }
        // Z->A
        LOG.debug(
            "Found a neighbor SrcNodeId: {} , SrcDegId: {}"
                + ", SrcTPId: {}, DestNodeId:{} , DestDegId: {}, DestTPId: {}",
            destNodeId, destDegId, destTtps.tx(), nodeId.getValue(), srcDegId, srcTtps.rx());

        InitRoadmNodesInputBuilder r2rlinkBuilderZToA = new InitRoadmNodesInputBuilder()
            .setRdmANode(destNodeId.getValue())
            .setDegANum(Uint8.valueOf(destDegId))
            .setTerminationPointA(destTtps.tx())
            .setRdmZNode(nodeId.getValue())
            .setDegZNum(Uint8.valueOf(srcDegId))
            .setTerminationPointZ(srcTtps.rx());
        if (!OrdLink.createRdm2RdmLinks(r2rlinkBuilderZToA.build(), this.dataBroker)) {
            LOG.error("OMS Link creation failed between node: {} and nodeId: {} in Z->A direction",
                destNodeId.getValue(), nodeId.getValue());
            return false;
        }
        return true;
    }

    /**
     * Delete a ROADM-to-ROADM link when a ROADM node is removed from the openroadm topology.
     *
     * @param nodeId Node name
     * @param interfaceName Name of the WDM line interface
     * @param remoteSystemName Name of the neighbor node
     * @param remoteInterfaceName Name of the WDM line interface on the neighbor node
     * @return True if the links are correctly created, False otherwise
     */
    public boolean deleteR2RLink(NodeId nodeId, String interfaceName, String remoteSystemName,
                                 String remoteInterfaceName) {
        // Find which degree is associated with ethernet interface
        Integer srcDegId = getDegFromInterface(nodeId, interfaceName);
        if (srcDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return false;
        }
        DegreeTtps srcTtps = getDegreeTtps(nodeId, srcDegId);
        if (srcTtps == null) {
            return false;
        }
        // Find degree for which Ethernet interface is created on other end
        NodeId destNodeId = new NodeId(remoteSystemName);
        Integer destDegId = getDegFromInterface(destNodeId, remoteInterfaceName);
        if (destDegId == null) {
            LOG.error("Couldnt find degree connected to Ethernet interface for nodeId: {}", destNodeId);
            return false;
        }
        DegreeTtps destTtps = getDegreeTtps(destNodeId, destDegId);
        if (destTtps == null) {
            return false;
        }
        return TopologyUtils.deleteLink(nodeId.getValue() + "-" + srcDegId, destNodeId.getValue() + "-" + destDegId,
            srcTtps.tx(), destTtps.rx(), networkTransactionService)
            && TopologyUtils.deleteLink(destNodeId.getValue() + "-" + destDegId, nodeId.getValue() + "-" + srcDegId,
                destTtps.tx(), srcTtps.rx(), networkTransactionService);
    }

    private Integer getDegFromInterface(NodeId nodeId, String interfaceName) {
        DataObjectIdentifier<Nodes> nodesIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue()))
            .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodesObject = Optional.ofNullable(readTx.read(LogicalDatastoreType.CONFIGURATION, nodesIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS))
                    .orElse(Optional.empty());
            if (nodesObject.isPresent() && (nodesObject.orElseThrow().getCpToDegree() != null)) {
                Collection<CpToDegree> cpToDeg = nodesObject.orElseThrow().nonnullCpToDegree().values();
                Stream<CpToDegree> cpToDegStream = cpToDeg.stream().filter(cp -> cp.getInterfaceName() != null)
                    .filter(cp -> cp.getInterfaceName().equals(interfaceName));
                if (cpToDegStream != null) {
                    Optional<CpToDegree> firstCpToDegree = cpToDegStream.findFirst();
                    if (firstCpToDegree.isPresent() && (firstCpToDegree != null)) {
                        LOG.debug("Found and returning {}",firstCpToDegree.orElseThrow().getDegreeNumber().intValue());
                        return firstCpToDegree.orElseThrow().getDegreeNumber().intValue();
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
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Unable to read mapping for Interface : {} for nodeId {}", interfaceName, nodeId, ex);
        }
        return null;
    }
}
