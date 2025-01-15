/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyUtils {

    private final NetworkTransactionService networkTransactionService;
    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(TopologyUtils.class);
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private final TapiLink tapiLink;
    private static final String TOPOLOGICAL_MODE = TapiProvider.TOPOLOGICAL_MODE;
    public static final String NOOPMODEDECLARED = "No operational mode declared in Topo for Tp {}, assumes by default ";

    public TopologyUtils(
            NetworkTransactionService networkTransactionService, DataBroker dataBroker, TapiLink tapiLink) {
        this.networkTransactionService = networkTransactionService;
        this.dataBroker = dataBroker;
        this.tapiSips = new HashMap<>();
        this.tapiLink = tapiLink;
        // TODO: Initially set topological mode to Full. Shall be set through the setter at controller initialization
    }

    public Network readTopology(DataObjectIdentifier<Network> networkIID) throws TapiTopologyException {
        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, networkIID);
        try {
            return topologyFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException(
                "Unable to get from mdsal topology: " + networkIID.toLegacy().firstKeyOf(Network.class).getNetworkId()
                .getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException(
                "Unable to get from mdsal topology: " + networkIID.toLegacy().firstKeyOf(Network.class).getNetworkId()
                .getValue(), e);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public List<String> readTopologyName(Uuid topoUuid) throws TapiTopologyException {
        Topology topology = null;
        DataObjectIdentifier<Topology> topoIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .build();
        ListenableFuture<Optional<Topology>> topologyFuture =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, topoIID);
        try {
            topology = topologyFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException(
                "Unable to get from mdsal topology: " + topoIID.toLegacy().firstKeyOf(Topology.class).getUuid()
                .getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException(
                "Unable to get from mdsal topology: " + topoIID.toLegacy().firstKeyOf(Topology.class).getUuid()
                .getValue(), e);
        } catch (NoSuchElementException e) {
            return null;
        }
        List<String> nameList = new ArrayList<>();
        for (Name value : topology.getName().values()) {
            nameList.add(value.getValue());
        }
        LOG.debug("Topology nameList {} = ", nameList);
        return nameList;
    }

    public Topology createOtnTopology() throws TapiTopologyException {
        // read openroadm-topology
        Network openroadmTopo = readTopology(InstanceIdentifiers.OVERLAY_NETWORK_II);
        String topoType = TOPOLOGICAL_MODE.equals("Full") ? TapiStringConstants.T0_FULL_MULTILAYER
            : TapiStringConstants.T0_TAPI_MULTILAYER;
        LOG.info("TOPOUTILS, createOtnTopology, the TOPOLOGICAL_MODE is {} ",topoType);
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(topoType.getBytes(Charset.forName("UTF-8"))).toString());
        Name name = new NameBuilder().setValue(topoType).setValueName("TAPI Topology Name").build();
        var topoBdr = new TopologyBuilder()
                .setName(Map.of(name.key(), name))
                .setUuid(topoUuid)
                .setLayerProtocolName(Set.of(
                        LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.ODU,
                        LayerProtocolName.DSR, LayerProtocolName.DIGITALOTN));
        if (openroadmTopo == null) {
            return topoBdr.build();
        }
        List<Link> linkList = openroadmTopo.augmentation(Network1.class) == null ? new ArrayList<>()
            : new ArrayList<>(openroadmTopo.augmentation(Network1.class).getLink().values());
        List<Link> xponderOutLinkList = new ArrayList<>();
        List<Link> xponderInLinkList = new ArrayList<>();
        for (Link lk : linkList) {
            switch (lk.augmentation(Link1.class).getLinkType()) {
                case XPONDEROUTPUT:
                    xponderOutLinkList.add(lk);
                    break;
                case XPONDERINPUT:
                    xponderInLinkList.add(lk);
                    break;
                default:
                    break;
            }
        }
        // read otn-topology
        Network otnTopo = readTopology(InstanceIdentifiers.OTN_NETWORK_II);
        Map<NodeId,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node>
                    otnNodeMap =
            otnTopo.nonnullNode().values().stream()
                .filter(onode -> !onode.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE"))
                .collect(Collectors.toMap(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.network.Node::getNodeId, node -> node));
        Map<String, List<String>> networkPortMap = new HashMap<>();
        for (var entry : otnNodeMap.entrySet()) {
            var entVal = entry.getValue();
            String portMappingNodeId = entVal.getSupportingNode().values().stream()
                .filter(sn -> sn.getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                .findFirst()
                .orElseThrow().getNodeRef().getValue();
            List<String> networkPortList = new ArrayList<>();
            var entKeyVal = entry.getKey().getValue();
            for (TerminationPoint tp: entVal.augmentation(Node1.class).getTerminationPoint().values()) {
                // TODO -> why are we checking with respect to XPDR links?? Is there a real purpose on doing that?
                if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)
                        && checkTp(entKeyVal, portMappingNodeId, tp, xponderOutLinkList, xponderInLinkList)) {
                    networkPortList.add(tp.getTpId().getValue());
                }
            }
            if (!networkPortList.isEmpty()) {
                networkPortMap.put(entKeyVal, networkPortList);
            }
        }
        Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            tapiNodeList = new HashMap<>();
        Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link>
            tapiLinkList = new HashMap<>();
        ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topoUuid, this.tapiLink);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(topoUuid);
        for (var entry : networkPortMap.entrySet()) {
            tapiFactory.convertNode(otnNodeMap.get(new NodeId(entry.getKey())), entry.getValue());
            this.tapiSips.putAll(tapiFactory.getTapiSips());
            tapiFullFactory.setTapiNodes(tapiFactory.getTapiNodes());
            tapiFullFactory.setTapiSips(tapiFactory.getTapiSips());
            tapiNodeList.putAll(tapiFactory.getTapiNodes());
            tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
        }
        // roadm infrastructure not abstracted
        // read openroadm-network
        Network openroadmNet = readTopology(InstanceIdentifiers.UNDERLAY_NETWORK_II.toIdentifier());
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                .networks.network.Node> rdmList =
            openroadmNet == null ? new ArrayList<>()
                : openroadmNet.nonnullNode().values().stream()
                    .filter(nt -> !nt.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE"))
                    .filter(nt -> nt
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                        .getNodeType()
                        .equals(OpenroadmNodeType.ROADM))
                    .collect(Collectors.toList());
        if (rdmList.isEmpty()) {
            LOG.warn("No roadm nodes exist in the network");
        } else {
            // map roadm nodes
            if (TOPOLOGICAL_MODE.equals("Full")) {
                for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node roadm : rdmList) {
                    tapiFullFactory.convertRoadmNode(roadm, openroadmTopo, "Full");
                    this.tapiSips.putAll(tapiFullFactory.getTapiSips());
                    tapiNodeList.putAll(tapiFullFactory.getTapiNodes());
                }
                tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
                // map roadm to roadm link
                List<Link> rdmTordmLinkList = linkList.stream()
                    .filter(lk -> lk.augmentation(Link1.class).getLinkType()
                        .equals(OpenroadmLinkType.ROADMTOROADM))
                    .collect(Collectors.toList());
                tapiFullFactory.convertRdmToRdmLinks(rdmTordmLinkList);
            } else {
                tapiFullFactory.convertRoadmNode(null, openroadmTopo, "Abstracted");
                this.tapiSips.putAll(tapiFullFactory.getTapiSips());
                tapiNodeList.putAll(tapiFullFactory.getTapiNodes());
                tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
            }
        }
        // map xpdr_input to roadm and xpdr_output to roadm links.
        xponderInLinkList.addAll(xponderOutLinkList);
        tapiFullFactory.convertXpdrToRdmLinks(xponderInLinkList);
        tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
        // Retrieve created sips map in TapiFactory when mapping all the nodes
        this.tapiSips.putAll(tapiFullFactory.getTapiSips());
        return topoBdr.setNode(tapiNodeList).setLink(tapiLinkList).build();
    }

    public boolean checkTp(
                String nodeIdTopo, String nodeIdPortMap, TerminationPoint tp, List<Link> xpdOut, List<Link> xpdIn) {
        LOG.info("Inside Checktp for node {}-{}", nodeIdTopo, nodeIdPortMap);
        String networkLcp = tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERCLIENT)
            ? tp.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1.class)
                .getAssociatedConnectionMapTp().iterator().next().getValue()
            : tp.getTpId().getValue();
        LOG.info("Network LCP associated = {}", networkLcp);
        @NonNull
        FluentFuture<Optional<Mapping>> mappingOpt = this.dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.Network.class)
                .child(Nodes.class, new NodesKey(nodeIdPortMap))
                .child(Mapping.class, new MappingKey(networkLcp))
                .build());
        if (!mappingOpt.isDone()) {
            LOG.error("Impossible to get mapping of associated network port {} of tp {}",
                networkLcp, tp.getTpId().getValue());
            return false;
        }
        Mapping mapping = null;
        try {
            mapping = mappingOpt.get().orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error getting mapping for {}", networkLcp, e);
            return false;
        }
        LOG.info("Mapping found = {}", mapping);
        switch (mapping.getPortDirection()) {
            // TODO -> remove the part of counting only if the Network LCP is part of a Link.
            //  We want to have all OTN nodes in the TAPI topology
            case "bidirectional":
                return true;
            case "tx":
            case "rx":
                LOG.info("PartnerLCP = {}", mapping.getPartnerLcp());
                return true;
            default:
                LOG.error("Invalid port direction for {}", networkLcp);
                return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
            .get.topology.details.output.Topology transformTopology(Topology topology) {
        var topologyBuilder = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                .get.topology.details.output.TopologyBuilder()
            .setUuid(topology.getUuid())
            .setName(topology.getName())
            .setLayerProtocolName(topology.getLayerProtocolName())
            .setLink(topology.getLink());
        if (topology.nonnullNode().isEmpty()) {
            return topologyBuilder.build();
        }
        Map<NodeKey, Node> mapNode = new HashMap<>();
        for (Node node: topology.nonnullNode().values()) {
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
            for (OwnedNodeEdgePoint onep: node.nonnullOwnedNodeEdgePoint().values()) {
                OwnedNodeEdgePoint newOnep = new OwnedNodeEdgePointBuilder()
                        .setUuid(onep.getUuid())
                        .setLayerProtocolName(onep.getLayerProtocolName())
                        .setName(onep.getName())
                        .setSupportedCepLayerProtocolQualifierInstances(onep
                            .getSupportedCepLayerProtocolQualifierInstances())
                        .setAdministrativeState(onep.getAdministrativeState())
                        .setOperationalState(onep.getOperationalState())
                        .setLifecycleState(onep.getLifecycleState())
//                            .setTerminationDirection(onep.getTerminationDirection())
//                            .setTerminationState(onep.getTerminationState())
                        .setDirection(onep.getDirection())
                        .setSupportedPayloadStructure(onep.getSupportedPayloadStructure())
                        .setAvailablePayloadStructure(onep.getAvailablePayloadStructure())
                        .setLinkPortRole(onep.getLinkPortRole())
                        .setMappedServiceInterfacePoint(onep.nonnullMappedServiceInterfacePoint())
                        .build();
                onepMap.put(newOnep.key(), newOnep);
            }
            Node newNode = new NodeBuilder()
                    .setUuid(node.getUuid())
                    .setName(node.getName())
                    .setOperationalState(node.getOperationalState())
                    .setAdministrativeState(node.getAdministrativeState())
                    .setLifecycleState(node.getLifecycleState())
                    .setLayerProtocolName(node.getLayerProtocolName())
                    .setNodeRuleGroup(node.getNodeRuleGroup())
                    .setInterRuleGroup(node.getInterRuleGroup())
                    .setOwnedNodeEdgePoint(onepMap)
                    .build();
            mapNode.put(newNode.key(), newNode);
        }
        return topologyBuilder.setNode(mapNode).build();
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getSipMap() {
        return tapiSips;
    }

}
