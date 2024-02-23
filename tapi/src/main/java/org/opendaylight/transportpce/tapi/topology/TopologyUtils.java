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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.NodesKey;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyUtils {

    private final NetworkTransactionService networkTransactionService;
    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(TopologyUtils.class);
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private final TapiLink tapiLink;
    private String topologicalMode;
    public static final String NOOPMODEDECLARED = "No operational mode declared in Topo for Tp {}, assumes by default ";

    public TopologyUtils(NetworkTransactionService networkTransactionService, DataBroker dataBroker,
                         TapiLink tapiLink) {
        this.networkTransactionService = networkTransactionService;
        this.dataBroker = dataBroker;
        this.tapiSips = new HashMap<>();
        this.tapiLink = tapiLink;
        // TODO: Initially set topological mode to Full. Shall be set through the setter at controller initialization
        this.topologicalMode = "Full";
    }

    public Network readTopology(InstanceIdentifier<Network> networkIID) throws TapiTopologyException {
        Network topology = null;
        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, networkIID);
        try {
            topology = topologyFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException("Unable to get from mdsal topology: " + networkIID
                    .firstKeyOf(Network.class).getNetworkId().getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException("Unable to get from mdsal topology: " + networkIID
                    .firstKeyOf(Network.class).getNetworkId().getValue(), e);
        } catch (NoSuchElementException e) {
            return null;
        }
        return topology;
    }

    public List<String> readTopologyName(Uuid topoUuid) throws TapiTopologyException {
        Topology topology = null;
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(
                Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.topology.rev221121.Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topoUuid)).build();

        ListenableFuture<Optional<Topology>> topologyFuture =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, topoIID);
        try {
            topology = topologyFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException("Unable to get from mdsal topology: " + topoIID
                    .firstKeyOf(Topology.class).getUuid().getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException("Unable to get from mdsal topology: " + topoIID
                .firstKeyOf(Topology.class).getUuid().getValue(), e);
        } catch (NoSuchElementException e) {
            return null;
        }
        List<String> nameList = new ArrayList<>();
        for (Map.Entry<NameKey, Name> entry : topology.getName().entrySet()) {
            nameList.add(entry.getValue().getValue());
        }
        LOG.debug("Topology nameList {} = ", nameList.toString());
        return nameList;
    }

    public Topology createFullOtnTopology() throws TapiTopologyException {
        // read openroadm-topology
        Network openroadmTopo = readTopology(InstanceIdentifiers.OVERLAY_NETWORK_II);
        String topoType = this.topologicalMode.equals("Full") ? TapiStringConstants.T0_FULL_MULTILAYER
            : TapiStringConstants.T0_TAPI_MULTILAYER;
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(topoType.getBytes(Charset.forName("UTF-8"))).toString());
        Name name = new NameBuilder()
            .setValue(topoType)
            .setValueName("TAPI Topology Name")
            .build();
        if (openroadmTopo != null) {
            List<Link> linkList = new ArrayList<>();
            if (openroadmTopo.augmentation(Network1.class) != null) {
                linkList.addAll(openroadmTopo.augmentation(Network1.class).getLink().values());
            }
            List<Link> xponderOutLinkList = linkList.stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT))
                .collect(Collectors.toList());
            List<Link> xponderInLinkList = linkList.stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDERINPUT))
                .collect(Collectors.toList());
            // read otn-topology
            Network otnTopo = readTopology(InstanceIdentifiers.OTN_NETWORK_II);
            Map<NodeId, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.rev180226.networks.network.Node> otnNodeMap = otnTopo.nonnullNode()
                .values().stream().collect(Collectors.toMap(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.rev180226.networks.network.Node::getNodeId, node -> node));

            Map<String, List<String>> networkPortMap = new HashMap<>();
            Iterator<Map.Entry<NodeId, org.opendaylight.yang.gen.v1.urn
                .ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node>> itOtnNodeMap = otnNodeMap
                .entrySet().iterator();
            while (itOtnNodeMap.hasNext()) {
                Map.Entry<NodeId, org.opendaylight.yang.gen.v1.urn
                    .ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node> entry = itOtnNodeMap.next();
                String portMappingNodeId = entry.getValue().getSupportingNode().values().stream()
                    .filter(sn -> sn.getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .findFirst()
                    .orElseThrow().getNodeRef().getValue();
                List<String> networkPortList = new ArrayList<>();
                for (TerminationPoint tp: entry.getValue().augmentation(Node1.class).getTerminationPoint().values()) {
                    // TODO -> why are we checking with respect to XPDR links?? Is there a real purpose on doing that?
                    if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)
                        && checkTp(entry.getKey().getValue(), portMappingNodeId, tp, xponderOutLinkList,
                        xponderInLinkList)) {
                        networkPortList.add(tp.getTpId().getValue());
                    }
                }
                if (!networkPortList.isEmpty()) {
                    networkPortMap.put(entry.getKey().getValue(), networkPortList);
                }
            }
            Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                tapiNodeList = new HashMap<>();
            Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link>
                tapiLinkList = new HashMap<>();
            ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topoUuid, this.tapiLink);
            ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(topoUuid);
            Iterator<Map.Entry<String, List<String>>> it = networkPortMap.entrySet().iterator();
            while (it.hasNext()) {
                String nodeId = it.next().getKey();
                tapiFactory.convertNode(otnNodeMap.get(new NodeId(nodeId)), networkPortMap.get(nodeId));
                this.tapiSips.putAll(tapiFactory.getTapiSips());
                tapiFullFactory.setTapiNodes(tapiFactory.getTapiNodes());
                tapiFullFactory.setTapiSips(tapiFactory.getTapiSips());
                tapiNodeList.putAll(tapiFactory.getTapiNodes());
                tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
            }
            // roadm infrastructure not abstracted
            // read openroadm-network
            Network openroadmNet = readTopology(InstanceIdentifiers.UNDERLAY_NETWORK_II);
            if (openroadmNet != null && openroadmNet.nonnullNode().values().stream()
                .filter(nt -> nt
                    .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                    .getNodeType()
                    .equals(OpenroadmNodeType.ROADM))
                .count() > 0) {
                // map roadm nodes
                if (this.topologicalMode.equals("Full")) {
                    for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks
                        .network.Node roadm:openroadmNet.nonnullNode().values().stream()
                        .filter(nt -> nt
                            .augmentation(
                                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                            .getNodeType()
                            .equals(OpenroadmNodeType.ROADM))
                        .collect(Collectors.toList())) {
                        tapiFullFactory.convertRoadmNode(roadm, openroadmTopo, "Full");
                        this.tapiSips.putAll(tapiFullFactory.getTapiSips());
                        tapiNodeList.putAll(tapiFullFactory.getTapiNodes());
                        tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
                        // map roadm to roadm link
                        List<Link> rdmTordmLinkList = linkList.stream()
                            .filter(lk -> lk.augmentation(Link1.class).getLinkType()
                                .equals(OpenroadmLinkType.ROADMTOROADM))
                            .collect(Collectors.toList());
                        tapiFullFactory.convertRdmToRdmLinks(rdmTordmLinkList);
                        tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
                    }
                } else {
                    tapiFullFactory.convertRoadmNode(null, openroadmTopo, "Abstracted");
                    this.tapiSips.putAll(tapiFullFactory.getTapiSips());
                    tapiNodeList.putAll(tapiFullFactory.getTapiNodes());
                    tapiLinkList.putAll(tapiFullFactory.getTapiLinks());
                }

            } else {
                LOG.warn("No roadm nodes exist in the network");
            }

            // map xpdr_input to roadm and xpdr_output to roadm links.
            xponderInLinkList.addAll(xponderOutLinkList);
            tapiFullFactory.convertXpdrToRdmLinks(xponderInLinkList);
            tapiLinkList.putAll(tapiFullFactory.getTapiLinks());

            // Retrieve created sips map in TapiFactory when mapping all the nodes
            this.tapiSips.putAll(tapiFullFactory.getTapiSips());
            return new TopologyBuilder()
                .setName(Map.of(name.key(), name))
                .setUuid(topoUuid)
                .setNode(tapiNodeList)
                .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.ODU,
                    LayerProtocolName.DSR, LayerProtocolName.DIGITALOTN))
                .setLink(tapiLinkList).build();
        }
        return new TopologyBuilder()
            .setName(Map.of(name.key(), name))
            .setUuid(topoUuid)
            .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.ODU,
                LayerProtocolName.DSR, LayerProtocolName.DIGITALOTN))
            .build();
    }

    public boolean checkTp(String nodeIdTopo, String nodeIdPortMap, TerminationPoint tp, List<Link> xpdOut,
                           List<Link> xpdIn) {
        LOG.info("Inside Checktp for node {}-{}", nodeIdTopo, nodeIdPortMap);
        String networkLcp;
        if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERCLIENT)) {
            networkLcp = tp.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1.class)
                .getAssociatedConnectionMapTp().iterator().next().getValue();
        } else {
            networkLcp = tp.getTpId().getValue();
        }
        LOG.info("Network LCP associated = {}", networkLcp);
        @NonNull
        KeyedInstanceIdentifier<Mapping, MappingKey> pmIID = InstanceIdentifier.create(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.Network.class)
                .child(Nodes.class, new NodesKey(nodeIdPortMap)).child(Mapping.class, new MappingKey(networkLcp));
        @NonNull
        FluentFuture<Optional<Mapping>> mappingOpt = this.dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.CONFIGURATION, pmIID);
        Mapping mapping = null;
        if (mappingOpt.isDone()) {
            try {
                mapping = mappingOpt.get().orElseThrow();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error getting mapping for {}", networkLcp,e);
                return false;
            }
        } else {
            LOG.error("Impossible to get mapping of associated network port {} of tp {}", networkLcp, tp.getTpId()
                    .getValue());
            return false;
        }
        LOG.info("Mapping found = {}", mapping);
        String networkPortDirection = mapping.getPortDirection();
        switch (networkPortDirection) {
            // TODO -> remove the part of counting only if the Network LCP is part of a Link.
            //  We want to have all OTN nodes in the TAPI topology
            case "bidirectional":
                return true;
            case "tx":
            case "rx":
                @Nullable
                String partnerLcp = mapping.getPartnerLcp();
                LOG.info("PartnerLCP = {}", partnerLcp);
                return true;
            default:
                LOG.error("Invalid port direction for {}", networkLcp);
                return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.topology.details.output.Topology
            transformTopology(Topology topology) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
            .get.topology.details.output.TopologyBuilder topologyBuilder =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
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
//                    OwnedNodeEdgePoint1 onep1 = onep.augmentation(OwnedNodeEdgePoint1.class);
//                    if (onep1 == null) {
//                        onepMap.put(onep.key(), onep);
//                        continue;
//                    }
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
        topologyBuilder.setNode(mapNode);
        return topologyBuilder.build();
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getSipMap() {
        return tapiSips;
    }

    public void setTopologicalMode(String topoMode) {
        this.topologicalMode = topoMode;
    }

    public String getTopologicalMode() {
        return topologicalMode;
    }

}
