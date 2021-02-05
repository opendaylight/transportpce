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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyUtils {

    private final NetworkTransactionService networkTransactionService;
    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(TopologyUtils.class);
    public static final String T0_MULTILAYER = "T0 - Multi-layer topology";
    public static final String TPDR_100G = "Transponder 100GE";
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;

    public TopologyUtils(NetworkTransactionService networkTransactionService, DataBroker dataBroker) {
        this.networkTransactionService = networkTransactionService;
        this.dataBroker = dataBroker;
        this.tapiSips = new HashMap<>();
    }

    public Topology createAbstracted100GTpdrTopology(Topology topology) {
        List<Node> dsrNodes
                = topology.nonnullNode().values().stream()
                .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
                .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> nep100GTpdrList = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node2 : dsrNodes) {
            List<OwnedNodeEdgePoint> nepList = node2.getOwnedNodeEdgePoint().values().stream()
                    .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr"))).collect(Collectors.toList());
            nep100GTpdrList.addAll(nepList);
        }
        Name topoName = new NameBuilder().setValue(TopologyUtils.TPDR_100G).setValueName("TAPI Topology Name").build();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.TPDR_100G.getBytes(Charset.forName("UTF-8")))
                .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node
                = createTapiNode(nep100GTpdrList, topoUuid);
        return new TopologyBuilder()
                .setName(Map.of(topoName.key(), topoName))
                .setUuid(topoUuid)
                .setNode(Map.of(node.key(), node))
                .build();
    }

    public Network readTopology(InstanceIdentifier<Network> networkIID) throws TapiTopologyException {
        Network topology = null;
        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, networkIID);
        try {
            topology = topologyFuture.get().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException("Unable to get from mdsal topology: " + networkIID
                    .firstKeyOf(Network.class).getNetworkId().getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException("Unable to get from mdsal topology: " + networkIID
                    .firstKeyOf(Network.class).getNetworkId().getValue(), e);
        }
        return topology;
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
        Map<NodeId, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.rev180226.networks.network.Node> otnNodeMap = otnTopo.nonnullNode().values().stream()
                .collect(Collectors.toMap(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.network.rev180226.networks.network.Node::getNodeId, node -> node));

        Map<String, List<String>> networkPortMap = new HashMap<>();
        Iterator<Map.Entry<NodeId, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.rev180226.networks.network.Node>> itOtnNodeMap = otnNodeMap.entrySet().iterator();
        while (itOtnNodeMap.hasNext()) {
            Map.Entry<NodeId, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.rev180226.networks.network.Node> entry = itOtnNodeMap.next();
            String portMappingNodeId = entry.getValue().getSupportingNode().values().stream()
                    .filter(sn -> sn.getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .findFirst()
                    .get().getNodeRef().getValue();
            List<String> networkPortList = new ArrayList<>();
            for (TerminationPoint tp : entry.getValue().augmentation(Node1.class).getTerminationPoint().values()) {
                if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)
                        &&
                        checkTp(entry.getKey().getValue(), portMappingNodeId, tp, xponderOutLinkList,
                                xponderInLinkList)) {
                    networkPortList.add(tp.getTpId().getValue());
                }
            }
            if (!networkPortList.isEmpty()) {
                networkPortMap.put(entry.getKey().getValue(), networkPortList);
            }
        }
        Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
                tapiNodeList = new HashMap<>();
        Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link>
                tapiLinkList = new HashMap<>();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER.getBytes(Charset.forName("UTF-8")))
                .toString());
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topoUuid);
        Iterator<Map.Entry<String, List<String>>> it = networkPortMap.entrySet().iterator();
        while (it.hasNext()) {
            String nodeId = it.next().getKey();
            tapiFactory.convertNode(otnNodeMap.get(new NodeId(nodeId)), networkPortMap.get(nodeId));
            tapiNodeList.putAll(tapiFactory.getTapiNodes());
            tapiLinkList.putAll(tapiFactory.getTapiLinks());
        }
        // roadm infrastructure not abstracted
        // read openroadm-network
        Network openroadmNet = readTopology(InstanceIdentifiers.UNDERLAY_NETWORK_II);
        if (openroadmNet.nonnullNode().values().stream().filter(nt ->
                nt.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
                        .getNodeType().equals(OpenroadmNodeType.ROADM)).count() > 0) {
            // map roadm nodes
            for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node
                    roadm:openroadmNet.nonnullNode().values().stream().filter(
                            nt -> nt.augmentation(org.opendaylight.yang.gen.v1.http
                            .org.openroadm.common.network.rev200529.Node1.class)
                            .getNodeType().equals(OpenroadmNodeType.ROADM))
                    .collect(Collectors.toList())) {
                tapiFactory.convertRoadmNode(roadm, openroadmTopo);
                tapiNodeList.putAll(tapiFactory.getTapiNodes());
            }
        } else {
            LOG.warn("No roadm nodes exist in the network");
        }
        // map roadm to roadm link
        List<Link> rdmTordmLinkList = linkList.stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.ROADMTOROADM))
                .collect(Collectors.toList());
        tapiFactory.convertRdmToRdmLinks(rdmTordmLinkList);
        tapiLinkList.putAll(tapiFactory.getTapiLinks());
        // map xpdr_input to roadm and xpdr_output to roadm links.
        xponderInLinkList.addAll(xponderOutLinkList);
        tapiFactory.convertXpdrToRdmLinks(xponderInLinkList);
        tapiLinkList.putAll(tapiFactory.getTapiLinks());

        if (otnTopo.augmentation(Network1.class) != null) {
            List<Link> otnLinkList = new ArrayList<>(otnTopo.augmentation(Network1.class).getLink().values());
            Collections.sort(otnLinkList, (l1, l2) -> l1.getLinkId().getValue()
                    .compareTo(l2.getLinkId().getValue()));
            tapiFactory.convertLinks(otnLinkList);
            tapiLinkList.putAll(tapiFactory.getTapiLinks());
        }
        Name name = new NameBuilder().setValue(TopologyUtils.T0_MULTILAYER).setValueName("TAPI Topology Name").build();
        // Retrieve created sips map in TapiFactory when mapping all the nodes
        this.tapiSips = tapiFactory.getTapiSips();
        return new TopologyBuilder()
                .setName(Map.of(name.key(), name))
                .setUuid(topoUuid)
                .setNode(tapiNodeList)
                .setLink(tapiLinkList).build();
    }

    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node createTapiNode(
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
                .setLayerProtocolName(List.of(LayerProtocolName.ETH))
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOwnedNodeEdgePoint(onepMap)
                .setNodeRuleGroup(createNodeRuleGroupFor100gTpdrNode(topoUuid, nodeUuid, nepList))
                .build();
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getSipMap() {
        return tapiSips;
    }

    public boolean checkTp(String nodeIdTopo, String nodeIdPortMap, TerminationPoint tp, List<Link> xpdOut,
                           List<Link> xpdIn) {
        String networkLcp;
        if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERCLIENT)) {
            networkLcp = tp.augmentation(
                    org.opendaylight.yang.gen.v1.http.transportpce.topology.rev201019.TerminationPoint1.class)
                    .getAssociatedConnectionMapPort();
        } else {
            networkLcp = tp.getTpId().getValue();
        }
        @NonNull
        KeyedInstanceIdentifier<Mapping, MappingKey> pmIID = InstanceIdentifier.create(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.Network.class)
                .child(Nodes.class, new NodesKey(nodeIdPortMap)).child(Mapping.class, new MappingKey(networkLcp));
        @NonNull
        FluentFuture<Optional<Mapping>> mappingOpt = this.dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.CONFIGURATION, pmIID);
        Mapping mapping = null;
        if (mappingOpt.isDone()) {
            try {
                mapping = mappingOpt.get().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error getting mapping for {}", networkLcp,e);
                return false;
            }
        } else {
            LOG.error("Impossible to get mapping of associated network port {} of tp {}", networkLcp, tp.getTpId()
                    .getValue());
            return false;
        }
        String networkPortDirection = mapping.getPortDirection();
        long count = 0;
        switch (networkPortDirection) {
            case "bidirectional":
                count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo) && lk
                        .getSource().getSourceTp().equals(networkLcp)).count();
                count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                        && lk.getDestination().getDestTp().equals(networkLcp)).count();
                return (count == 2);
            case "tx":
            case "rx":
                @Nullable
                String partnerLcp = mapping.getPartnerLcp();
                if (mapping.getPortQual().equals("tx")) {
                    count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo)
                            && lk.getSource().getSourceTp().equals(networkLcp)).count();
                    count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                            && lk.getDestination().getDestTp().equals(partnerLcp)).count();
                }
                if (mapping.getPortQual().equals("rx")) {
                    count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                            && lk.getDestination().getDestTp().equals(networkLcp)).count();
                    count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo)
                            && lk.getSource().getSourceTp().equals(partnerLcp)).count();
                }
                return (count == 2);
            default:
                LOG.error("Invalid port direction for {}", networkLcp);
                return false;
        }
    }

    public Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupFor100gTpdrNode(Uuid topoUuid, Uuid nodeUuid,
                                                                               Collection<OwnedNodeEdgePoint> onepl) {

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
                nepMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : onepl) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint
                    nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                    .NodeEdgePointBuilder()
                    .setTopologyUuid(topoUuid)
                    .setNodeUuid(nodeUuid)
                    .setNodeEdgePointUuid(onep.key().getUuid())
                    .build();
            nepMap.put(nep.key(), nep);
        }
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
                .setLocalId("forward")
                .setForwardingRule(ForwardingRule.MAYFORWARDACROSSGROUP)
                .setRuleType(RuleType.FORWARDING)
                .build();
        ruleList.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(("rdm infra node rule group").getBytes(Charset.forName("UTF-8")))
                .toString()))
            .setRule(ruleList)
            .setNodeEdgePoint(nepMap)
            .build();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        return nodeRuleGroupMap;
    }
}
