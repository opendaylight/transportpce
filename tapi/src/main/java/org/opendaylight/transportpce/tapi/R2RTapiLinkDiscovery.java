/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.NbrList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfName;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.Direction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R2RTapiLinkDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(R2RTapiLinkDiscovery.class);

    private final NetworkTransactionService networkTransactionService;
    private final DeviceTransactionManager deviceTransactionManager;
    private final TapiLink tapiLink;

    public R2RTapiLinkDiscovery(NetworkTransactionService networkTransactionService,
            DeviceTransactionManager deviceTransactionManager, TapiLink tapiLink) {
        this.networkTransactionService = networkTransactionService;
        this.deviceTransactionManager = deviceTransactionManager;
        this.tapiLink = tapiLink;
    }

    public Map<LinkKey, Link> readLLDP(NodeId nodeId, int nodeVersion, Uuid tapiTopoUuid) {
        LOG.info("Tapi R2R Link Node version = {}", nodeVersion);
        // TODO -> waiting for device 7.1 in network model to change this to a switch statement and include
        //  support for 7.1 devices
        switch (nodeVersion) {
            case 1:
                // 1.2.1
                DataObjectIdentifier<Protocols> protocols121IID = DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(Protocols.class)
                    .build();
                Optional<Protocols> protocol121Object = this.deviceTransactionManager.getDataFromDevice(
                    nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocols121IID, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (hasNoNeighbor121(protocol121Object)) {
                    LOG.warn("LLDP subtree is missing or incomplete: isolated openroadm device");
                    return new HashMap<>();
                }
                // get neighbor list
                NbrList nbr121List = protocol121Object.orElseThrow().augmentation(Protocols1.class).getLldp()
                    .getNbrList();
                LOG.info("LLDP subtree is present. Device has {} neighbours", nbr121List.getIfName().size());
                // try to create rdm2rdm link
                return rdm2rdmLinkCreatev121(nodeId, tapiTopoUuid, nbr121List);
            case 2:
                // 2.2.1
                DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                        .org.openroadm.device.container.org.openroadm.device.Protocols>
                        protocols221IID = DataObjectIdentifier
                    .builderOfInherited(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData.class,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
                            .OrgOpenroadmDevice.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                        .container.org.openroadm.device.Protocols.class)
                    .build();
                var protocol221Object = this.deviceTransactionManager
                    .getDataFromDevice(nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocols221IID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (hasNoNeighbor221(protocol221Object)) {
                    LOG.warn("LLDP subtree is missing or incomplete: isolated openroadm device");
                    return new HashMap<>();
                }
                var nbr221List = protocol221Object.orElseThrow().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1.class)
                    .getLldp().getNbrList();
                LOG.info("LLDP subtree is present. Device has {} neighbours", nbr221List.getIfName().size());
                return rdm2rdmLinkCreatev221(nodeId, tapiTopoUuid, nbr221List);
            case 3:
                // 7.1.0
                LOG.info("Not yet supported?");
                return new HashMap<>();
            default:
                LOG.error("Unable to read LLDP data for unmanaged openroadm device version");
                return new HashMap<>();
        }
    }

    private boolean hasNoNeighbor121(Optional<Protocols> protocol121Object) {
        return protocol121Object.isEmpty()
                || protocol121Object.orElseThrow().augmentation(Protocols1.class) == null
                || protocol121Object.orElseThrow().augmentation(Protocols1.class).getLldp() == null
                || protocol121Object.orElseThrow().augmentation(Protocols1.class).getLldp().getNbrList() == null;
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

    private Map<LinkKey, Link> rdm2rdmLinkCreatev221(NodeId nodeId, Uuid tapiTopoUuid,
            org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.NbrList nbrList) {
        Map<LinkKey, Link> linkMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.nbr.list.IfName
                ifName : nbrList.nonnullIfName().values()) {
            if (ifName.getRemoteSysName() == null) {
                LOG.warn("Tapi R2R Link LLDP subtree neighbour is empty for nodeId: {}, ifName: {}",
                    nodeId.getValue(),ifName.getIfName());
                continue;
            }
            Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName.getRemoteSysName());
            if (!mps.isPresent()) {
                LOG.warn("Tapi R2R Link Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                // The controller raises a warning rather than an error because the first node to
                // mount cannot see its neighbors yet. The link will be detected when processing
                // the neighbor node.
                continue;
            }
            Link omsLink = createR2RTapiLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(),
                ifName.getRemotePortId(), tapiTopoUuid);
            if (omsLink != null) {
                linkMap.put(omsLink.key(), omsLink);
            } else {
                LOG.error("Link was not created");
            }
        }
        return linkMap;
    }

    private Map<LinkKey, Link> rdm2rdmLinkCreatev121(NodeId nodeId, Uuid tapiTopoUuid, NbrList nbrList) {
        Map<LinkKey, Link> linkMap = new HashMap<>();
        for (IfName ifName : nbrList.nonnullIfName().values()) {
            if (ifName.getRemoteSysName() == null) {
                LOG.warn("Tapi R2R Link LLDP subtree neighbour is empty for nodeId: {}, ifName: {}",
                    nodeId.getValue(),ifName.getIfName());
                continue;
            }
            Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName
                .getRemoteSysName());
            if (!mps.isPresent()) {
                LOG.warn("Tapi R2R Link Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                // The controller raises a warning rather than an error because the first node to
                // mount cannot see its neighbors yet. The link will be detected when processing
                // the neighbor node.
                continue;
            }
            Link omsLink = createR2RTapiLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(),
                ifName.getRemotePortId(), tapiTopoUuid);
            if (omsLink != null) {
                linkMap.put(omsLink.key(), omsLink);
            } else {
                LOG.error("Link was not created");
            }
        }
        return linkMap;
    }

    public Link createR2RTapiLink(NodeId nodeId, String interfaceName, String remoteSystemName,
                                 String remoteInterfaceName, Uuid tapiTopoUuid) {
        String srcTpTx = null;
        String srcTpRx = null;
        // Find which degree is associated with ethernet interface
        Integer srcDegId = getDegFromInterface(nodeId, interfaceName);
        if (srcDegId == null) {
            LOG.error("Tapi R2R Link Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return null;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting
        // number of
        // circuit-packs under degree subtree
        Direction sourceDirection = getDegreeDirection(srcDegId, nodeId);
        if (Direction.NotApplicable == sourceDirection) {
            LOG.error("Tapi R2R Link Couldnt find degree direction for nodeId: {} and degree: {}", nodeId, srcDegId);
            return null;
        } else if (Direction.Bidirectional == sourceDirection) {
            srcTpTx = "DEG" + srcDegId + "-TTP-TXRX";
            srcTpRx = "DEG" + srcDegId + "-TTP-TXRX";
        } else {
            srcTpTx = "DEG" + srcDegId + "-TTP-TX";
            srcTpRx = "DEG" + srcDegId + "-TTP-RX";
        }
        LOG.debug("Tapi R2R Link SrcTPTx {}, SrcTPRx {}", srcTpTx, srcTpRx);
        // Find degree for which Ethernet interface is created on other end
        NodeId destNodeId = new NodeId(remoteSystemName);
        Integer destDegId = getDegFromInterface(destNodeId, remoteInterfaceName);
        if (destDegId == null) {
            LOG.error("Tapi R2R Link Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return null;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting
        // number of
        // circuit-packs under degree subtree
        String destTpTx = null;
        String destTpRx = null;
        Direction destinationDirection = getDegreeDirection(destDegId, destNodeId);
        if (Direction.NotApplicable == destinationDirection) {
            LOG.error("Tapi R2R Link Couldnt find degree direction for nodeId: {} and degree: {}",
                destNodeId, destDegId);
            return null;
        } else if (Direction.Bidirectional == destinationDirection) {
            destTpTx = "DEG" + destDegId + "-TTP-TXRX";
            destTpRx = "DEG" + destDegId + "-TTP-TXRX";
        } else {
            destTpTx = "DEG" + destDegId + "-TTP-TX";
            destTpRx = "DEG" + destDegId + "-TTP-RX";
        }
        // Todo -> only handling for the bidirectional case. I assume all tps are of the type bidirectional
        LOG.debug("Tapi R2R Link DstTPTx {}, DstTPRx {}", destTpTx, srcTpRx);

        // Create OMS Tapi Link
        LOG.info("Tapi R2R Link Found a neighbor SrcNodeId: {} , SrcDegId: {} , SrcTPId: {}, DestNodeId:{} , "
            + "DestDegId: {}, DestTPId: {}", nodeId.getValue(), srcDegId, srcTpTx, destNodeId, destDegId, destTpRx);
        Link omsLink = this.tapiLink.createTapiLink(nodeId.getValue(), srcTpTx, destNodeId.getValue(), destTpTx,
            TapiStringConstants.OMS_RDM_RDM_LINK, TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.PHTNC_MEDIA,
            TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS,
            this.tapiLink.getAdminState(nodeId.getValue(), destNodeId.getValue(), srcTpTx, destTpTx),
            this.tapiLink.getOperState(nodeId.getValue(), destNodeId.getValue(), srcTpTx, destTpTx),
            Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()), tapiTopoUuid);
        LOG.info("Tapi R2R Link OMS link created = {}", omsLink);
        LOG.debug("inputAdminstate= {}, inputoperstate = {}",
            this.tapiLink.getAdminState(nodeId.getValue(), destNodeId.getValue(), srcTpTx, destTpTx),
            this.tapiLink.getOperState(nodeId.getValue(), destNodeId.getValue(), srcTpTx, destTpTx));
        return omsLink;
    }

    private Integer getDegFromInterface(NodeId nodeId, String interfaceName) {
        DataObjectIdentifier<Nodes> nodesIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue()))
            .build();
        try {

            Optional<Nodes> nodesObject = this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                    nodesIID).get();
            if (nodesObject.isEmpty() || (nodesObject.orElseThrow().getCpToDegree() == null)) {
                LOG.warn("Could not find mapping for Interface {} for nodeId {}", interfaceName,
                    nodeId.getValue());
                return null;
            }
            Collection<CpToDegree> cpToDeg = nodesObject.orElseThrow().nonnullCpToDegree().values();
            Stream<CpToDegree> cpToDegStream = cpToDeg.stream().filter(cp -> cp.getInterfaceName() != null)
                .filter(cp -> cp.getInterfaceName().equals(interfaceName));
            if (cpToDegStream != null) {
                Optional<CpToDegree> firstCpToDegree = cpToDegStream.findFirst();
                if (firstCpToDegree.isEmpty() || (firstCpToDegree == null)) {
                    LOG.debug("Not found so returning nothing");
                    return null;
                }
                LOG.debug("Found and returning {}",firstCpToDegree.orElseThrow().getDegreeNumber().intValue());
                return firstCpToDegree.orElseThrow().getDegreeNumber().intValue();
            } else {
                LOG.warn("CircuitPack stream couldnt find anything for nodeId: {} and interfaceName: {}",
                    nodeId.getValue(),interfaceName);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for Interface : {} for nodeId {}", interfaceName, nodeId, ex);
        }
        return null;
    }

    public Direction getDegreeDirection(Integer degreeCounter, NodeId nodeId) {
        DataObjectIdentifier<Nodes> nodesIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue()))
            .build();
        try {
            Optional<Nodes> nodesObject = this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                nodesIID).get();
            if (nodesObject.isPresent() && (nodesObject.orElseThrow().getMapping() != null)) {
                Collection<Mapping> mappingList = nodesObject.orElseThrow().nonnullMapping().values();
                mappingList = mappingList.stream().filter(mp -> mp.getLogicalConnectionPoint().contains("DEG"
                    + degreeCounter)).collect(Collectors.toList());
                if (mappingList.size() == 1) {
                    return Direction.Bidirectional;
                } else if (mappingList.size() > 1) {
                    return Direction.Tx;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
        }
        return Direction.NotApplicable;
    }
}
