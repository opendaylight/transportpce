/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TapiCommonService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.UpdateServiceInterfacePointInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.UpdateServiceInterfacePointOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.SipBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.SipKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.link.details.output.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.node.edge.point.details.output.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopologyImpl implements TapiTopologyService, TapiCommonService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImpl.class);
    private final DataBroker dataBroker;
    private final TapiContext tapiContext;
    private final TopologyUtils topologyUtils;

    public TapiTopologyImpl(DataBroker dataBroker, TapiContext tapiContext, TopologyUtils topologyUtils) {
        this.dataBroker = dataBroker;
        this.tapiContext = tapiContext;
        this.topologyUtils = topologyUtils;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeDetailsOutput>> getNodeDetails(GetNodeDetailsInput input) {
        // TODO Auto-generated method stub
        // TODO -> maybe we get errors when having CEPs?
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // Node id: if roadm -> ROADM+PHOTONIC_MEDIA. if xpdr -> XPDR-XPDR+DSR/OTSi
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(input.getNodeIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node = this.tapiContext
                .getTapiNode(topoUuid, nodeUuid);
        if (node == null) {
            LOG.error("Invalid TAPI node name");
            return RpcResultBuilder.<GetNodeDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                "Invalid Tapi Node name").buildFuture();
        }
        return RpcResultBuilder.success(new GetNodeDetailsOutputBuilder()
                .setNode(new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.topology.rev181210.get.node.details.output.NodeBuilder(node).build())
                .build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyDetailsOutput>> getTopologyDetails(GetTopologyDetailsInput input) {
        // TODO -> Add check for Full T0 Multilayer
        if (!TopologyUtils.T0_MULTILAYER.equals(input.getTopologyIdOrName())
            && !TopologyUtils.TPDR_100G.equals(input.getTopologyIdOrName())) {
            if (TopologyUtils.T0_FULL_MULTILAYER.equals(input.getTopologyIdOrName())) {
                Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName()
                    .getBytes(Charset.forName("UTF-8"))).toString());
                Context context = this.tapiContext.getTapiContext();
                Map<TopologyKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology>
                    topologyMap = context.augmentation(Context1.class).getTopologyContext().getTopology();
                if (!(topologyMap != null && topologyMap.containsKey(new TopologyKey(topoUuid)))) {
                    LOG.error("Topology {} not found in datastore", input.getTopologyIdOrName());
                    return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
                        .withError(RpcError.ErrorType.RPC, "Invalid Topology name").buildFuture();
                }
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology
                    topology = topologyMap.get(new TopologyKey(topoUuid));
                return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                        .setTopology(this.topologyUtils.transformTopology(topology))
                        .build())
                    .buildFuture();
            }
            LOG.error("Invalid TAPI topology name");
            return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
                .withError(RpcError.ErrorType.RPC, "Invalid Topology name").buildFuture();
        }
        try {
            LOG.info("Building TAPI Topology abstraction for {}", input.getTopologyIdOrName());
            Topology topology = createAbstractedOtnTopology();
            if (TopologyUtils.TPDR_100G.equals(input.getTopologyIdOrName())) {
                topology = createAbstracted100GTpdrTopology(topology);
            }
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().setTopology(topology).build())
                .buildFuture();
        } catch (TapiTopologyException e) {
            LOG.error("error building TAPI topology");
            return RpcResultBuilder.<GetTopologyDetailsOutput>failed()
                .withError(RpcError.ErrorType.RPC, "Error building topology").buildFuture();
        }
    }

    private Topology createAbstracted100GTpdrTopology(Topology topology) {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> dsrNodes =
            topology.nonnullNode().values().stream()
                .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
                .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> nep100GTpdrList = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node2: dsrNodes) {
            List<OwnedNodeEdgePoint> nepList = node2.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr"))).collect(Collectors.toList());
            nep100GTpdrList.addAll(nepList);
        }
        Name topoName = new NameBuilder().setValue(TopologyUtils.TPDR_100G).setValueName("TAPI Topology Name").build();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.TPDR_100G.getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node =
            createTapiNode(nep100GTpdrList, topoUuid);
        return new TopologyBuilder()
            .setName(Map.of(topoName.key(), topoName))
            .setUuid(topoUuid)
            .setNode(Map.of(node.key(), node))
            .build();
    }

    private Network readTopology(InstanceIdentifier<Network> networkIID) throws TapiTopologyException {
        Network topology = null;
        FluentFuture<Optional<Network>> topologyFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, networkIID);
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

    private Topology createAbstractedOtnTopology() throws TapiTopologyException {
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
            .collect(Collectors.toMap(Node::getNodeId, node -> node));

        Map<String, List<String>> networkPortMap = new HashMap<>();
        Iterator<Entry<NodeId, Node>> itOtnNodeMap = otnNodeMap.entrySet().iterator();
        while (itOtnNodeMap.hasNext()) {
            Entry<NodeId, Node> entry = itOtnNodeMap.next();
            String portMappingNodeId = entry.getValue().getSupportingNode().values().stream()
                .filter(sn -> sn.getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                .findFirst()
                .get().getNodeRef().getValue();
            List<String> networkPortList = new ArrayList<>();
            for (TerminationPoint tp: entry.getValue().augmentation(Node1.class).getTerminationPoint().values()) {
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
        Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
            tapiNodeList = new HashMap<>();
        Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link>
            tapiLinkList = new HashMap<>();
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topoUuid);
        Iterator<Entry<String, List<String>>> it = networkPortMap.entrySet().iterator();
        while (it.hasNext()) {
            String nodeId = it.next().getKey();
            tapiFactory.convertNode(otnNodeMap.get(new NodeId(nodeId)), networkPortMap.get(nodeId));
            tapiNodeList.putAll(tapiFactory.getTapiNodes());
            tapiLinkList.putAll(tapiFactory.getTapiLinks());
        }
        if (openroadmTopo.nonnullNode().values().stream().filter(nt ->
                nt.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
                .getNodeType().equals(OpenroadmNodeType.SRG)).count() > 0) {
            tapiFactory.convertRoadmInfrastructure();
            tapiNodeList.putAll(tapiFactory.getTapiNodes());
            tapiLinkList.putAll(tapiFactory.getTapiLinks());
        } else {
            LOG.warn("Unable to abstract an ROADM infrasctructure from openroadm-topology");
        }
        if (otnTopo.augmentation(Network1.class) != null) {
            Map<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.networks.network.LinkKey, Link> otnLinkMap =
                otnTopo.augmentation(Network1.class).getLink();
            tapiFactory.convertLinks(otnLinkMap);
            tapiLinkList.putAll(tapiFactory.getTapiLinks());
        }
        Name name = new NameBuilder().setValue(TopologyUtils.T0_MULTILAYER).setValueName("TAPI Topology Name").build();
        return new TopologyBuilder()
                .setName(Map.of(name.key(), name))
                .setUuid(topoUuid)
                .setNode(tapiNodeList)
                .setLink(tapiLinkList).build();
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeEdgePointDetailsOutput>> getNodeEdgePointDetails(
            GetNodeEdgePointDetailsInput input) {
        // TODO Auto-generated method stub
        // TODO -> maybe we get errors when having CEPs?
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // Node id: if roadm -> ROADMid+PHOTONIC_MEDIA. if xpdr -> XPDRid-XPDRnbr+DSR/OTSi
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(input.getNodeIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // NEP id: if roadm -> ROADMid+PHOTONIC_MEDIA/MC/OTSiMC+TPid.
        // if xpdr -> XPDRid-XPDRnbr+DSR/eODU/iODU/iOTSi/eOTSi/PHOTONIC_MEDIA+TPid
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(input.getEpIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint nep = this.tapiContext.getTapiNEP(topoUuid, nodeUuid, nepUuid);
        if (nep == null) {
            LOG.error("Invalid TAPI nep name");
            return RpcResultBuilder.<GetNodeEdgePointDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                "Invalid NEP name").buildFuture();
        }
        return RpcResultBuilder.success(new GetNodeEdgePointDetailsOutputBuilder()
                .setNodeEdgePoint(new NodeEdgePointBuilder(nep).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetLinkDetailsOutput>> getLinkDetails(GetLinkDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // Link id: same as OR link id
        Uuid linkUuid = new Uuid(UUID.nameUUIDFromBytes(input.getLinkIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link link = this.tapiContext
                .getTapiLink(topoUuid, linkUuid);
        if (link == null) {
            LOG.error("Invalid TAPI link name");
            return RpcResultBuilder.<GetLinkDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                "Invalid Link name").buildFuture();
        }
        return RpcResultBuilder.success(new GetLinkDetailsOutputBuilder().setLink(new LinkBuilder(link).build())
                .build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyListOutput>> getTopologyList(GetTopologyListInput input) {
        // TODO Auto-generated method stub
        // TODO -> maybe we get errors when having CEPs?
        Map<TopologyKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology>
                topologyMap = this.tapiContext.getTopologyContext();
        if (topologyMap.isEmpty()) {
            LOG.error("No topologies exist in tapi context");
            return RpcResultBuilder.<GetTopologyListOutput>failed().withError(RpcError.ErrorType.APPLICATION,
                "No topologies exist in tapi context").buildFuture();
        }
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.TopologyKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.Topology>
                newTopoMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology
                topo:topologyMap.values()) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.Topology
                newTopo = new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.TopologyBuilder(topo).build();
            newTopoMap.put(newTopo.key(), newTopo);
        }
        return RpcResultBuilder.success(new GetTopologyListOutputBuilder().setTopology(newTopoMap).build())
                .buildFuture();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node
            createTapiNode(List<OwnedNodeEdgePoint> nepList, Uuid topoUuid) {
        Name name = new NameBuilder().setValueName("Tpdr100g node name").setValue("Tpdr100g over WDM node").build();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (OwnedNodeEdgePoint ownedNodeEdgePoint: nepList) {
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

    private boolean checkTp(String nodeIdTopo, String nodeIdPortMap, TerminationPoint tp, List<Link> xpdOut,
                            List<Link> xpdIn) {
        String networkLcp;
        if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERCLIENT)) {
            networkLcp = tp.augmentation(
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.TerminationPoint1.class)
                .getAssociatedConnectionMapPort();
        } else {
            networkLcp = tp.getTpId().getValue();
        }
        @NonNull
        KeyedInstanceIdentifier<Mapping, MappingKey> pmIID = InstanceIdentifier.create(
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.Network.class)
            .child(Nodes.class, new NodesKey(nodeIdPortMap)).child(Mapping.class, new MappingKey(networkLcp));
        @NonNull
        FluentFuture<Optional<Mapping>> mappingOpt = dataBroker.newReadOnlyTransaction().read(
            LogicalDatastoreType.CONFIGURATION, pmIID);
        Mapping mapping = null;
        if (mappingOpt.isDone()) {
            try {
                mapping = mappingOpt.get().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error getting mapping for {}", networkLcp, e);
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
                count += xpdOut.stream().filter(lk -> lk.getSource().getSourceNode().getValue().equals(nodeIdTopo)
                    && lk.getSource().getSourceTp().getValue().equals(networkLcp)).count();
                count += xpdIn.stream().filter(lk -> lk.getDestination().getDestNode().getValue().equals(nodeIdTopo)
                    && lk.getDestination().getDestTp().getValue().equals(networkLcp)).count();
                return (count == 2);
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
                return (count == 2);
            default:
                LOG.error("Invalid port direction for {}", networkLcp);
                return false;
        }
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupFor100gTpdrNode(
            Uuid topoUuid, Uuid nodeUuid, Collection<OwnedNodeEdgePoint> onepl) {

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
            nepMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep: onepl) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint nep =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
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

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointDetailsOutput>>
            getServiceInterfacePointDetails(GetServiceInterfacePointDetailsInput input) {
        Uuid sipUuid = new Uuid(input.getSipIdOrName());
        Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips =
            this.tapiContext.getTapiContext().getServiceInterfacePoint();
        if (sips == null || sips.isEmpty()) {
            return RpcResultBuilder.<GetServiceInterfacePointDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                "No sips in datastore").buildFuture();
        }
        if (!sips.containsKey(new ServiceInterfacePointKey(sipUuid))) {
            return RpcResultBuilder.<GetServiceInterfacePointDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                "Sip doesnt exist in datastore").buildFuture();
        }
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.details.output.Sip outSip =
                new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.details.output.SipBuilder(
                        sips.get(new ServiceInterfacePointKey(sipUuid)))
                    .build();
        return RpcResultBuilder.success(new GetServiceInterfacePointDetailsOutputBuilder().setSip(outSip).build())
            .buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointListOutput>>
            getServiceInterfacePointList(GetServiceInterfacePointListInput input) {
        Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips =
            this.tapiContext.getTapiContext().getServiceInterfacePoint();
        if (sips == null || sips.isEmpty()) {
            return RpcResultBuilder.<GetServiceInterfacePointListOutput>failed().withError(RpcError.ErrorType.RPC,
                "No sips in datastore").buildFuture();
        }
        Map<SipKey, Sip> outSipMap = new HashMap<>();
        for (ServiceInterfacePoint sip : sips.values()) {
            Sip si = new SipBuilder(sip).build();
            outSipMap.put(si.key(), si);
        }
        return RpcResultBuilder.success(new GetServiceInterfacePointListOutputBuilder().setSip(outSipMap).build())
            .buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UpdateServiceInterfacePointOutput>>
            updateServiceInterfacePoint(UpdateServiceInterfacePointInput input) {
        // TODO --> not yet implemented
        return null;
    }
}
