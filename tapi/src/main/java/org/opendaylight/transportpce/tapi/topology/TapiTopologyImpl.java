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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopologyImpl implements TapiTopologyService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImpl.class);
    private static final String ETH_TOPO = "Ethernet Topology";
    private static final String T0_MULTI_LAYER_TOPO = "T0 - Multi-layer topology";
    private final DataBroker dataBroker;

    public TapiTopologyImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeDetailsOutput>> getNodeDetails(GetNodeDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyDetailsOutput>> getTopologyDetails(GetTopologyDetailsInput input) {
        LOG.info("Building TAPI Topology abstraction from {}", input.getTopologyIdOrName());
        Topology topology = null;
        switch (input.getTopologyIdOrName()) {
            case NetworkUtils.OVERLAY_NETWORK_ID:
                topology = createAbstractedOpenroadmTopology();
                break;
            case NetworkUtils.OTN_NETWORK_ID:
                topology = createAbstractedOtnTopology();
                break;
            default:
                LOG.error("{} unknown - can not be abstracted", input.getTopologyIdOrName());
                break;
        }
        if (topology != null) {
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().setTopology(topology).build())
                .buildFuture();
        } else {
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().build()).buildFuture();
        }
    }

    private Topology createAbstractedOpenroadmTopology() {
        // read openroadm-topology
        @NonNull
        FluentFuture<Optional<Network>> openroadmTopoOpt = dataBroker.newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OVERLAY_NETWORK_II);
        if (!openroadmTopoOpt.isDone()) {
            LOG.warn("Cannot get openroadm topology, returning null");
            return null;
        }
        Optional<Network> optionalOpenroadmTop = null;
        try {
            optionalOpenroadmTop = openroadmTopoOpt.get();
        } catch (InterruptedException e) {
            //sonar : "InterruptedException" should not be ignored (java:S2142)
            //https://www.ibm.com/developerworks/java/library/j-jtp05236/index.html?ca=drs-#2.1
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | NoSuchElementException e) {
            LOG.error("Impossible to retrieve openroadm-topology from mdsal", e);
            return null;
        }

        if (!optionalOpenroadmTop.isPresent()) {
            LOG.warn("Openroadm topology is not present, returning null");
            return null;
        }
        Network openroadmTopo = optionalOpenroadmTop.get();
        List<Node> nodeList = new ArrayList<>(openroadmTopo.getNode().values());
        List<Link> linkList = null;
        if (openroadmTopo.augmentation(Network1.class) != null) {
            linkList = new ArrayList<>(openroadmTopo.augmentation(Network1.class).getLink().values());
        } else {
            linkList = new ArrayList<>();
        }
        List<Link> xponderOutLinkList = linkList.stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT))
                .collect(Collectors.toList());
        List<Link> xponderInLinkList = linkList.stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDERINPUT))
                .collect(Collectors.toList());

        List<Node> xpdrNodeList = nodeList
                .stream()
                .filter(nt -> nt
                        .augmentation(org.opendaylight.yang.gen.v1
                                .http.org.openroadm.common.network.rev181130.Node1.class)
                        .getNodeType().equals(OpenroadmNodeType.XPONDER)).collect(Collectors.toList());
        Map<String, List<String>> clientPortMap = new HashMap<>();
        for (Node node : xpdrNodeList) {
            String nodeId = node.getSupportingNode().values().stream()
                .filter(sn -> sn.getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                .findFirst()
                .get().getNodeRef().getValue();
            List<String> clientPortList = new ArrayList<>();
            for (TerminationPoint tp : node.augmentation(Node1.class).getTerminationPoint().values()) {
                if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERCLIENT)
                        && checkTp(node.getNodeId().getValue(), nodeId, tp, xponderOutLinkList, xponderInLinkList)) {
                    clientPortList.add(tp.getTpId().getValue());
                }
            }
            if (!clientPortList.isEmpty()) {
                clientPortMap.put(nodeId, clientPortList);
            }
        }
        List<String> goodTpList = extractGoodTpList(clientPortMap);
        // tapi topology creation
        Map<NameKey, Name> names = new HashMap<>();
        Name name = new NameBuilder().setValue(ETH_TOPO).setValueName("Topo Name").build();
        names.put(name.key(), name);
        Uuid uuid = new Uuid(UUID.nameUUIDFromBytes(ETH_TOPO.getBytes(Charset.forName("UTF-8"))).toString());
        Map<NodeKey ,org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
            tapiNodeList = new HashMap<>();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node
            = createTapiNode(goodTpList);
        tapiNodeList.put(node.key(), node);
        return new TopologyBuilder().setName(names).setUuid(uuid).setNode(tapiNodeList).build();

    }

    private List<String> extractGoodTpList(Map<String, List<String>> clientPortMap) {
        List<String> goodTpList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : clientPortMap.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            for (String tpid : value) {
                goodTpList.add(key + "--" + tpid);
            }
        }
        return goodTpList;
    }

    private Topology createAbstractedOtnTopology() {
        // read otn-topology
        @NonNull
        FluentFuture<Optional<Network>> otnTopoOpt = dataBroker.newReadOnlyTransaction().read(
            LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OTN_NETWORK_II);
        if (otnTopoOpt.isDone()) {
            Network otnTopo = null;
            try {
                otnTopo = otnTopoOpt.get().get();
            } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
                LOG.error("Impossible to retreive otn-topology from mdsal",e);
                return null;
            }
            List<Node> nodeList = new ArrayList<>(otnTopo.getNode().values());
            List<Node> otnNodeList = nodeList.stream().filter(nt -> nt.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class)
                .getNodeType().equals(OpenroadmNodeType.SWITCH) || nt.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class)
                    .getNodeType().equals(OpenroadmNodeType.MUXPDR)).collect(Collectors.toList());
            Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
                tapiNodeList = new HashMap<>();
            Map<LinkKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link>
                tapiLinkList = new HashMap<>();
            Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(T0_MULTI_LAYER_TOPO.getBytes(Charset.forName("UTF-8")))
                .toString());
            ConvertORTopoObjectToTapiTopoObject tapiFactory = new ConvertORTopoObjectToTapiTopoObject(topoUuid);
            for (Node node : otnNodeList) {
                tapiFactory.convertNode(node);
                tapiNodeList.putAll(tapiFactory.getTapiNodes());
                tapiLinkList.putAll(tapiFactory.getTapiLinks());
            }
            if (otnTopo.augmentation(Network1.class) != null) {
                List<Link> otnLinkList = new ArrayList<>(otnTopo.augmentation(Network1.class).getLink().values());
                Collections.sort(otnLinkList, (l1, l2) -> l1.getLinkId().getValue()
                    .compareTo(l2.getLinkId().getValue()));
                tapiFactory.convertLinks(otnLinkList);
                tapiLinkList.putAll(tapiFactory.getTapiLinks());
            }
            Name name = new NameBuilder().setValue(T0_MULTI_LAYER_TOPO).setValueName("TAPI Topology Name").build();
            return new TopologyBuilder()
                    .setName(Map.of(name.key(), name))
                    .setUuid(topoUuid)
                    .setNode(tapiNodeList)
                    .setLink(tapiLinkList).build();
        } else {
            return null;
        }
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeEdgePointDetailsOutput>> getNodeEdgePointDetails(
        GetNodeEdgePointDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetLinkDetailsOutput>> getLinkDetails(GetLinkDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyListOutput>> getTopologyList(GetTopologyListInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node createTapiNode(List<
        String> tpList) {
        Name name = new NameBuilder().setValueName("node name").setValue("TAPI Ethernet Node").build();
        List<LayerProtocolName> layerProtocols = new ArrayList<>();
        layerProtocols.add(LayerProtocolName.ETH);
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        for (int i = 0; i < tpList.size(); i++) {
            Name onedName = new NameBuilder().setValueName("OwnedNodeEdgePoint " + i).setValue(tpList.get(i)).build();
            OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(("OwnedNodeEdgePoint " + i).getBytes(Charset.forName("UTF-8")))
                    .toString()))
                .setLayerProtocolName(LayerProtocolName.ETH).setMappedServiceInterfacePoint(createSIP(1))
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(AdministrativeState.UNLOCKED).setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED).setName(Map.of(onedName.key(), onedName))
                .setTerminationDirection(
                    TerminationDirection.BIDIRECTIONAL).setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL)
                .build();
            onepl.put(onep.key(), onep);
        }

        return new NodeBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(name.getValue().getBytes(Charset.forName("UTF-8")))
                    .toString()))
                .setName(Map.of(name.key(), name)).setLayerProtocolName(layerProtocols)
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOwnedNodeEdgePoint(onepl)
                .build();
    }

    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createSIP(int nb) {
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder().setServiceInterfacePointUuid(
                new Uuid(UUID.randomUUID().toString())).build();
            msipl.put(msip.key(), msip);
        }
        return msipl;
    }

    private boolean checkTp(String nodeIdTopo, String nodeIdPortMap, TerminationPoint tp, List<Link> xpdOut, List<
        Link> xpdIn) {
        @Nullable
        String networkLcp = tp.augmentation(
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1.class)
            .getAssociatedConnectionMapPort();
        @NonNull
        KeyedInstanceIdentifier<Mapping, MappingKey> pmIID = InstanceIdentifier.create(
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.Network.class)
            .child(Nodes.class, new NodesKey(nodeIdPortMap)).child(Mapping.class, new MappingKey(networkLcp));
        @NonNull
        FluentFuture<Optional<Mapping>> mappingOpt = dataBroker.newReadOnlyTransaction().read(
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

}
