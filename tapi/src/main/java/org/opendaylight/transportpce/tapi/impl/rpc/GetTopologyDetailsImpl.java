/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.topology.ConvertORToTapiTopology;
import org.opendaylight.transportpce.tapi.topology.ConvertORTopoToTapiTopo;
import org.opendaylight.transportpce.tapi.topology.ConvertTapiTopoToAbstracted;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetTopologyDetailsImpl implements GetTopologyDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetTopologyDetailsImpl.class);

    private final TapiContext tapiContext;
    private final TopologyUtils topologyUtils;
    private final TapiLink tapiLink;
    private final NetworkTransactionService networkTransactionService;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;

    public GetTopologyDetailsImpl(TapiContext tapiContext, TopologyUtils topologyUtils, TapiLink tapiLink,
            NetworkTransactionService networkTransactionService) {
        this.tapiContext = tapiContext;
        this.topologyUtils = topologyUtils;
        this.tapiLink = tapiLink;
        this.tapiSips = new HashMap<>();
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyDetailsOutput>> invoke(GetTopologyDetailsInput input) {
        var topoId = input.getTopologyId();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology topology;
        Uuid topologyUuid100G = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.TPDR_100G.getBytes(
            Charset.forName("UTF-8"))).toString());
        if (!topologyUuid100G.equals(topoId)
                && !TapiStringConstants.T0_TAPI_MULTILAYER_UUID.equals(topoId.getValue())
                && !TapiStringConstants.T0_MULTILAYER_UUID.equals(topoId.getValue())) {
            Map<TopologyKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology>
                topologyMap = this.tapiContext.getTapiContext().augmentation(Context1.class).getTopologyContext()
                    .getTopology();
            if (topologyMap == null || (
                    !topologyMap.containsKey(new TopologyKey(new Uuid(TapiStringConstants.T0_FULL_MULTILAYER_UUID)))
                    && !topologyMap.containsKey(new TopologyKey(new Uuid(TapiStringConstants.SBI_TAPI_TOPOLOGY_UUID)))
                    && !topologyMap.containsKey(new TopologyKey(new Uuid(
                        TapiStringConstants.ALIEN_XPDR_TAPI_TOPOLOGY_UUID))))) {
                LOG.error("Topology {} not found in datastore", topoId);
                return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
                    .withError(ErrorType.RPC, "Invalid Topology name")
                    .buildFuture();
            }
            topology = topologyMap.get(new TopologyKey(topoId));
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                .setTopology(this.topologyUtils.transformTopology(topology))
                .build())
                .buildFuture();
        }
        if (input.getTopologyId().getValue().equals(TapiStringConstants.T0_TAPI_MULTILAYER_UUID)) {
            try {
                LOG.info("Building TAPI Topology abstraction for {}", topoId);
                topology = createAbsTopologyFromTapiTopo();
                return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                    .setTopology(this.topologyUtils.transformTopology(topology)).build())
                    .buildFuture();
            } catch (TapiTopologyException e) {
                LOG.error("error building TAPI topology");
                return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
                    .withError(ErrorType.RPC, "Error building topology")
                    .buildFuture();
            }
        }
        if (topologyUuid100G.equals(topoId)
                || TapiStringConstants.T0_MULTILAYER_UUID.equals(topoId.getValue())) {
            try {
                LOG.info("Building TAPI Topology abstraction for {}", topoId);
                topology = createAbstractedOtnTopology();
                if (topoId.getValue().equals(TapiStringConstants.T0_MULTILAYER_UUID)) {
                    return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                        .setTopology(this.topologyUtils.transformTopology(topology)).build())
                        .buildFuture();
                }
                topology = createAbstracted100GTpdrTopology(topology);
                return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                    .setTopology(this.topologyUtils.transformTopology(topology)).build())
                    .buildFuture();
            } catch (TapiTopologyException e) {
                LOG.error("error building TAPI topology");
                return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
                    .withError(ErrorType.RPC, "Error building topology")
                    .buildFuture();
            }
        }
        return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
            .withError(ErrorType.RPC, "Invalid Topology name")
            .buildFuture();
    }

    public Topology createAbstracted100GTpdrTopology(Topology topology) {
        List<OwnedNodeEdgePoint> nep100GTpdrList = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node2 :
                topology.nonnullNode().values().stream()
                    .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
                    .collect(Collectors.toList())) {
            List<OwnedNodeEdgePoint> nepList = node2.getOwnedNodeEdgePoint().values().stream()
                    .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr")))
                    .collect(Collectors.toList());
            nep100GTpdrList.addAll(nepList);
        }
        Name topoName = new NameBuilder()
                .setValue(TapiStringConstants.TPDR_100G)
                .setValueName("TAPI Topology Name")
                .build();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.TPDR_100G.getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node =
            createTapiNode(nep100GTpdrList, topoUuid);
        return new TopologyBuilder()
            .setName(Map.of(topoName.key(), topoName))
            .setUuid(topoUuid)
            .setNode(Map.of(node.key(), node))
            .setLayerProtocolName(Set.of(LayerProtocolName.DSR, LayerProtocolName.ETH))
            .build();
    }

    public Topology createAbstractedOtnTopology() throws TapiTopologyException {
        // read openroadm-topology
        Network openroadmTopo = readTopology(InstanceIdentifiers.OVERLAY_NETWORK_II);
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
        Map<NodeId, Node> otnNodeMap = otnTopo.nonnullNode().values().stream()
                .filter(onode -> !onode.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE"))
                .collect(Collectors.toMap(Node::getNodeId, node -> node));

        Map<String, List<String>> networkPortMap = new HashMap<>();
        Iterator<Entry<NodeId, Node>> itOtnNodeMap = otnNodeMap.entrySet().iterator();
        while (itOtnNodeMap.hasNext()) {
            Entry<NodeId, Node> entry = itOtnNodeMap.next();
            String portMappingNodeId = entry.getValue().getSupportingNode().values().stream()
                    .filter(sn -> sn.getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .findFirst()
                    .orElseThrow().getNodeRef().getValue();
            List<String> networkPortList = new ArrayList<>();
            for (TerminationPoint tp : entry.getValue().augmentation(Node1.class).getTerminationPoint().values()) {
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
        Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodeList
                = new HashMap<>();
        Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> tapiLinkList
                = new HashMap<>();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
        ConvertORTopoToTapiTopo tapiAbstractFactory = new ConvertORTopoToTapiTopo(topoUuid, this.tapiLink);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(topoUuid);
        Iterator<Entry<String, List<String>>> it = networkPortMap.entrySet().iterator();
        while (it.hasNext()) {
            String nodeId = it.next().getKey();
            tapiFactory.convertNode(otnNodeMap.get(new NodeId(nodeId)), networkPortMap.get(nodeId));
            tapiAbstractFactory.setTapiNodes(tapiFactory.getTapiNodes());
            tapiAbstractFactory.setTapiSips(tapiFactory.getTapiSips());
            tapiNodeList.putAll(tapiAbstractFactory.getTapiNodes());
            tapiLinkList.putAll(tapiAbstractFactory.getTapiLinks());
        }
        if (openroadmTopo.nonnullNode().values().stream()
                .filter(nt -> !nt.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE"))
                .filter(nt -> nt.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                    .Node1.class).getNodeType().equals(OpenroadmNodeType.SRG))
                .count() > 0) {
            tapiAbstractFactory.convertRoadmInfrastructure();
            tapiNodeList.putAll(tapiAbstractFactory.getTapiNodes());
            tapiLinkList.putAll(tapiAbstractFactory.getTapiLinks());
        } else {
            LOG.warn("Unable to abstract an ROADM infrasctructure from openroadm-topology");
        }
        if (otnTopo.augmentation(Network1.class) != null) {
            tapiAbstractFactory.convertLinks(otnTopo.augmentation(Network1.class).getLink());
            tapiLinkList.putAll(tapiAbstractFactory.getTapiLinks());
        }
        Name name = new NameBuilder().setValue(
                TapiStringConstants.T0_MULTILAYER).setValueName("TAPI Topology Name")
            .build();
        LOG.info("TOPOABSTRACTED : the list of node is as follows {}", tapiNodeList);
        this.tapiSips.putAll(tapiAbstractFactory.getTapiSips());
        return new TopologyBuilder()
            .setName(Map.of(name.key(), name))
            .setUuid(topoUuid)
            .setNode(tapiNodeList)
            .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.DIGITALOTN))
            .setLink(tapiLinkList)
            .build();
    }

    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology
            createAbsTopologyFromTapiTopo() throws TapiTopologyException {
        Uuid refTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology tapiFullTopo =
            this.tapiContext
                .getTopologyContext().entrySet().stream().filter(topo -> topo.getKey().getUuid().equals(refTopoUuid))
                .findAny().orElseThrow().getValue();
        ConvertTapiTopoToAbstracted absTapiTopo = new ConvertTapiTopoToAbstracted(refTopoUuid);
        absTapiTopo.setTapiLinks(tapiFullTopo.getLink());
        absTapiTopo.setTapiNodes(tapiFullTopo.getNode());
        absTapiTopo.convertRoadmInfrastructure();

        Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            tapiNodeList = new HashMap<>();
        Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link>
            tapiLinkList = new HashMap<>();
        tapiNodeList.putAll(absTapiTopo.getTapiNodes());
        tapiLinkList.putAll(absTapiTopo.getTapiLinks());
        Name name = new NameBuilder()
            .setValue(TapiStringConstants.T0_MULTILAYER)
            .setValueName("TAPI Topology Name")
            .build();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_TAPI_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info("ABSTRACTED TAPI TOPOLOGY : the list of node is as follows {}", tapiNodeList);
        return new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context
                .TopologyBuilder()
            .setName(Map.of(name.key(), name))
            .setUuid(topoUuid)
            .setNode(tapiNodeList)
            .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.DIGITALOTN))
            .setLink(tapiLinkList)
            .build();
    }

    private Network readTopology(InstanceIdentifier<Network> networkIID) throws TapiTopologyException {
        Network topology = null;
        ListenableFuture<Optional<Network>> topologyFuture = networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, networkIID);
        try {
            topology = topologyFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException("Unable to get from mdsal topology: "
                    + networkIID.firstKeyOf(Network.class).getNetworkId().getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException("Unable to get from mdsal topology: "
                    + networkIID.firstKeyOf(Network.class).getNetworkId().getValue(), e);
        }
        return topology;
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node createTapiNode(
            List<OwnedNodeEdgePoint> nepList, Uuid topoUuid) {
        Name name = new NameBuilder().setValueName("Tpdr100g node name").setValue("Tpdr100g over WDM node").build();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (OwnedNodeEdgePoint ownedNodeEdgePoint : nepList) {
            onepMap.put(ownedNodeEdgePoint.key(), ownedNodeEdgePoint);
        }
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(name.getValue().getBytes(Charset.forName("UTF-8"))).toString());
        return new NodeBuilder()
                .setUuid(nodeUuid)
                .setName(Map.of(name.key(), name))
                .setLayerProtocolName(Set.of(LayerProtocolName.ETH))
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOwnedNodeEdgePoint(onepMap)
                .setNodeRuleGroup(createNodeRuleGroupFor100gTpdrNode(topoUuid, nodeUuid, nepList))
                .build();
    }

    private boolean checkTp(
            String nodeIdTopo, String nodeIdPortMap, TerminationPoint tp, List<Link> xpdOut, List<Link> xpdIn) {
        String networkLcp =
            tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERCLIENT)
                ? tp.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1.class)
                    .getAssociatedConnectionMapTp().iterator().next().getValue()
                : tp.getTpId().getValue();
        ListenableFuture<Optional<Mapping>> mappingOpt =
            networkTransactionService.read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.Network.class)
                        .child(Nodes.class, new NodesKey(nodeIdPortMap))
                        .child(Mapping.class, new MappingKey(networkLcp)));
        if (!mappingOpt.isDone()) {
            LOG.error("Impossible to get mapping of associated network port {} of tp {}",
                    networkLcp, tp.getTpId().getValue());
            return false;
        }
        Mapping mapping;
        try {
            mapping = mappingOpt.get().orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error getting mapping for {}", networkLcp, e);
            return false;
        }
        long count = 0;
        switch (mapping.getPortDirection()) {
            case "bidirectional":
                count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo)
                        && lk.getSource().getSourceTp().getValue().equals(networkLcp)).count();
                count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                        && lk.getDestination().getDestTp().getValue().equals(networkLcp)).count();
                return count == 2;
            case "tx":
            case "rx":
                @Nullable
                String partnerLcp = mapping.getPartnerLcp();
                if (mapping.getPortQual().equals("tx")) {
                    count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo)
                            && lk.getSource().getSourceTp().getValue().equals(networkLcp)).count();
                    count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                            && lk.getDestination().getDestTp().getValue().equals(partnerLcp)).count();
                }
                if (mapping.getPortQual().equals("rx")) {
                    count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                            && lk.getDestination().getDestTp().getValue().equals(networkLcp)).count();
                    count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo)
                            && lk.getSource().getSourceTp().getValue().equals(partnerLcp)).count();
                }
                return count == 2;
            default:
                LOG.error("Invalid port direction for {}", networkLcp);
                return false;
        }
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupFor100gTpdrNode(
            Uuid topoUuid, Uuid nodeUuid, Collection<OwnedNodeEdgePoint> onepl) {
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint> nepMap
            = new HashMap<>();
        for (OwnedNodeEdgePoint onep : onepl) {
            var nep = new NodeEdgePointBuilder()
                .setTopologyUuid(topoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onep.key().getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        Rule rule = new RuleBuilder()
                .setLocalId("forward")
                .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                .setRuleType(new HashSet<RuleType>(Set.of(RuleType.FORWARDING)))
                .build();
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(
                        ("rdm infra node rule group").getBytes(Charset.forName("UTF-8")))
                    .toString()))
                .setRule(new HashMap<RuleKey, Rule>(Map.of(rule.key(), rule)))
                .setNodeEdgePoint(nepMap)
                .build();
        return new HashMap<NodeRuleGroupKey, NodeRuleGroup>(Map.of(nodeRuleGroup.key(), nodeRuleGroup));
    }

}
