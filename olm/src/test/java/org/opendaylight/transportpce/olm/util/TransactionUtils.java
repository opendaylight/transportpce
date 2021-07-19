/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegreeKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.oms.attributes.AmplifiedLinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SupportingLinkKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class TransactionUtils {

    private TransactionUtils() {

    }

    @SuppressWarnings("unchecked")
    public static boolean writeTransaction(DataBroker dataBroker, InstanceIdentifier instanceIdentifier,
        DataObject object) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        transaction.commit();// submit(Timeouts.DATASTORE_WRITE, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT).get();
        return true;
    }

//
//    public static boolean writeTransaction(DataBroker dataBroker, LogicalDatastoreType logicalDatastoreType,
//        InstanceIdentifier instanceIdentifier,
//        DataObject object)

//    public static DataObject readTransaction(DeviceTransactionManager deviceTransactionManager,
//                                  String nodeId,
//                                  LogicalDatastoreType logicalDatastoreType,
//                                  InstanceIdentifier<? extends DataObject> instanceIdentifier)
//            throws ExecutionException, InterruptedException {
//        Future<Optional<DeviceTransaction>> deviceTxFuture =
//                deviceTransactionManager.getDeviceTransaction(nodeId);
//        if (!deviceTxFuture.get().isPresent()) {
//            return null;
//        }
//        DeviceTransaction deviceTx = deviceTxFuture.get().get();
//        com.google.common.base.Optional<? extends DataObject> readOpt
//                = deviceTx.read(logicalDatastoreType, instanceIdentifier).get();
//        if (!readOpt.isPresent()) {
//            return null;
//        }
//        return readOpt.get();
//    }

    public static Network1 getNullNetwork() {
        Map<LinkKey, Link> nullMap = null;
        Network1 network = new Network1Builder().setLink(nullMap).build();
        Optional.of(network);
        return network;
    }

    public static Network1 getEmptyNetwork() {
        Network1 network = new Network1Builder().setLink(Map.of()).build();
        Optional.of(network);
        return network;
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
            .rev180226.networks.Network getOverLayNetwork() {
        SupportingNode supportingNode = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .build();
        NodeBuilder node1Builder = new NodeBuilder()
                .setNodeId(new NodeId("node 1"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .setSupportingNode(Map.of(supportingNode.key(),supportingNode));
        Map<NodeKey,Node> nodes = new HashMap<>();
        Node node1 = node1Builder.build();
        nodes.put(node1.key(),node1);
        SupportingNode supportingNode2 = new SupportingNodeBuilder()
            .setNodeRef(new NodeId("node 2"))
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .build();
        NodeBuilder node2Builder = new NodeBuilder()
                .setNodeId(new NodeId("node 2"))
                .withKey(new NodeKey(new NodeId("node 2")))
                .setSupportingNode(Map.of(supportingNode2.key(),supportingNode2));
        Node node2 = node2Builder.build();
        nodes.put(node2.key(),node2);
        SupportingNode supportingNode3 = new SupportingNodeBuilder()
            .setNodeRef(new NodeId("node 3"))
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .build();
        NodeBuilder node3Builder = new NodeBuilder()
                .setNodeId(new NodeId("node 3"))
                .withKey(new NodeKey(new NodeId("node 3")))
                .setSupportingNode(Map.of(supportingNode3.key(),supportingNode3));
        Node node3 = node3Builder.build();
        nodes.put(node3.key(),node3);
        SupportingNode supportingNode4 = new SupportingNodeBuilder()
            .setNodeRef(new NodeId("node 4"))
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .build();
        NodeBuilder node4Builder = new NodeBuilder()
                .setNodeId(new NodeId("node 4"))
                .withKey(new NodeKey(new NodeId("node 4")))
                .setSupportingNode(Map.of(supportingNode4.key(),supportingNode4));
        Node node4 = node4Builder.build();
        nodes.put(node4.key(),node4);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder
            networkBuilder =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks
                    .NetworkBuilder()
                .setNode(nodes)
                .setNetworkId(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network network =
            networkBuilder.build();
        Optional.of(network);
        return network;
    }

    public static Network1 getNetwork() {
        Map<SupportingLinkKey,SupportingLink> supportingLinks = new HashMap<>();
        SupportingLink supportingLink1 = new SupportingLinkBuilder()
                .setLinkRef(new LinkId("ref1"))
                .setNetworkRef(new NetworkId("net1"))
                .build();
        SupportingLink supportingLink2 = new SupportingLinkBuilder()
                .setLinkRef(new LinkId("ref2"))
                .setNetworkRef(new NetworkId("net2"))
                .build();
        supportingLinks.put(supportingLink1.key(),supportingLink1);
        supportingLinks.put(supportingLink2.key(),supportingLink2);
        Map<LinkKey,Link> links = new HashMap<>();
        Link link1 = new LinkBuilder()
                .setLinkId(new LinkId("link 1"))
                .setDestination(
                    new DestinationBuilder()
                        .setDestNode(new NodeId("node 1"))
                        .setDestTp(new TpId("dest tp")).build())
                .setSource(
                    new SourceBuilder()
                        .setSourceNode(new NodeId("node 2"))
                        .setSourceTp(new TpId("src tp"))
                        .build())
            .setSupportingLink(supportingLinks)
            .build();

        Link link2 = new LinkBuilder()
                .setLinkId(new LinkId("link 2"))
                .setDestination(
                    new DestinationBuilder()
                        .setDestNode(new NodeId("node 3"))
                        .setDestTp(new TpId("dest tp"))
                        .build())
                .setSource(
                    new SourceBuilder()
                        .setSourceNode(new NodeId("node 4"))
                        .setSourceTp(new TpId("src tp"))
                        .build())
                .setSupportingLink(supportingLinks)
                .build();
        links.put(link1.key(),link1);
        links.put(link2.key(),link2);
        Network1 network = new Network1Builder().setLink(links).build();
        Optional.of(network);
        return network;
    }

    public static Network1 getNetwork2() {
        Map<LinkConcatenationKey,LinkConcatenation> linkConcentationValues = new HashMap<>();
        LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
            .setFiberType(LinkConcatenation.FiberType.Truewave)
            .setPmd(new FiberPmd(BigDecimal.ONE))
            .setSRLGId(Uint32.valueOf(1))
            .setSRLGLength(Uint32.valueOf(1))
            .build();
        LinkConcatenation linkConcatenation2 = new LinkConcatenationBuilder()
            .setFiberType(LinkConcatenation.FiberType.Truewave)
            .setPmd(new FiberPmd(BigDecimal.ONE))
            .setSRLGId(Uint32.valueOf(1))
            .setSRLGLength(Uint32.valueOf(1))
            .build();
        linkConcentationValues.put(linkConcatenation.key(),linkConcatenation);
        linkConcentationValues.put(linkConcatenation2.key(),linkConcatenation2);
        Map<AmplifiedLinkKey,AmplifiedLink>
            amplifiedLinkValues = new HashMap<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink al =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes
                    .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link
                    .attributes.amplified.link.section.element.section.element.SpanBuilder()
                        .setSpan(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link
                                .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                                    .setAdministrativeState(AdminStates.InService)
                                    .setAutoSpanloss(true)
                                    .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                    .setLinkConcatenation(linkConcentationValues)
                                    .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                    .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                    .build())
                    .build())
                .build())
            .setSectionEltNumber(Uint16.valueOf(1)).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink al2 = new
            org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes
                .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link
                     .attributes.amplified.link.section.element.section.element.SpanBuilder()
                        .setSpan(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link
                                .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                                    .setAdministrativeState(AdminStates.InService)
                                    .setAutoSpanloss(true)
                                    .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                    .setLinkConcatenation(linkConcentationValues)
                                    .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                    .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                    .build())
                                    .build())
            .build())
            .setSectionEltNumber(Uint16.valueOf(1)).build();
        amplifiedLinkValues.put(al.key(),al);
        amplifiedLinkValues.put(al2.key(),al2);
        Map<Class<? extends Augmentation<Link>>, Augmentation<Link>> map = Collections.emptyMap();
        Augmentation<Link> aug1 = new Link1Builder()
            .setAmplified(true)
            .setOMSAttributes(new OMSAttributesBuilder()
                .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                .setSpan(new SpanBuilder().build())
                .build())
            .build();
        Augmentation<Link> aug2 = new Link1Builder()
            .setAmplified(true)
            .setOMSAttributes(new OMSAttributesBuilder()
                .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                .setSpan(new SpanBuilder().build())
                .build())
            .build();

        Map<SupportingLinkKey,SupportingLink> supportingLinks = new HashMap<>();
        SupportingLink supportingLink = new SupportingLinkBuilder().setLinkRef(new LinkId("ref1"))
            .setNetworkRef(new NetworkId("net1")).build();
        SupportingLink supportingLink2 = new SupportingLinkBuilder().setLinkRef(new LinkId("ref2"))
            .setNetworkRef(new NetworkId("net1")).build();
        supportingLinks.put(supportingLink.key(),supportingLink);
        supportingLinks.put(supportingLink2.key(),supportingLink2);
        Map<LinkKey,Link> links = new HashMap<>();
        Link link1 = new LinkBuilder().setLinkId(new LinkId("link 1"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 1"))
                .setDestTp(new TpId("dest tp")).build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 2"))
                .setSourceTp(new TpId("src tp")).build())
            .setSupportingLink(supportingLinks)
            .addAugmentation(aug1)
            .addAugmentation(aug2).build();

        Link link2 = new LinkBuilder().setLinkId(new LinkId("link 2"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 3"))
                .setDestTp(new TpId("dest tp")).build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 4"))
                .setSourceTp(new TpId("src tp")).build())
            .setSupportingLink(supportingLinks).build();
        links.put(link1.key(),link1);
        links.put(link2.key(),link2);
        Network1 network = new Network1Builder().setLink(links).build();
        Optional.of(network);
        return network;
    }

    public static List<NodeId> getNodeIds() {
        List<NodeId> nodes = new ArrayList<>();
        NodeId node = new NodeId("node 1");
        NodeId node2 = new NodeId("node 1");
        NodeId node3 = new NodeId("node 1");
        NodeId node4 = new NodeId("node 1");
        nodes.add(node);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        return nodes;
    }

    public static Nodes getNodes(String nodeId, String mappingKey) {
        Map<CpToDegreeKey,CpToDegree> cpList = new HashMap<>();
        CpToDegree cp1 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name1")
            .setDegreeNumber(Uint32.valueOf(123))
            .build();
        CpToDegree cp2 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name2")
            .setDegreeNumber(Uint32.valueOf(123))
            .build();
        cpList.put(cp1.key(),cp1);
        cpList.put(cp2.key(),cp2);
        Mapping map1 = new MappingBuilder()
            .setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey((mappingKey != null) ? mappingKey : "null"))
            .build();
        Nodes nodes = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder()
                .setNodeType(org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes.Xpdr)
                .setOpenroadmVersion(OpenroadmNodeVersion._121)
                .build())
            .setCpToDegree(cpList)
            .setMapping(Map.of(map1.key(),map1))
            .build();
        return nodes;
    }

    public static Nodes getNodes2(String nodeId, String mappingKey) {
        Map<CpToDegreeKey,CpToDegree> cpList = new HashMap<>();
        CpToDegree cp1 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name1")
            .setDegreeNumber(Uint32.valueOf(123))
            .build();
        CpToDegree cp2 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name2")
            .setDegreeNumber(Uint32.valueOf(123))
            .build();
        cpList.put(cp1.key(),cp1);
        cpList.put(cp2.key(),cp2);
        Mapping map1 = new MappingBuilder()
            .setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey((mappingKey != null) ? mappingKey : "null"))
            .build();
        Nodes nodes = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder()
                .setNodeType(null)
                .setOpenroadmVersion(OpenroadmNodeVersion._121)
                .build())
            .setCpToDegree(cpList)
            .setMapping(Map.of(map1.key(),map1))
            .build();
        return nodes;
    }

    public static Nodes getNodes3(String nodeId, String mappingKey) {
        Map<CpToDegreeKey,CpToDegree> cpList = new HashMap<>();
        CpToDegree cp1 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name1")
            .setDegreeNumber(Uint32.valueOf(123))
            .build();
        CpToDegree cp2 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name2")
            .setDegreeNumber(Uint32.valueOf(123))
            .build();
        cpList.put(cp1.key(),cp1);
        cpList.put(cp2.key(),cp2);
        Mapping map1 = new MappingBuilder()
            .setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey((mappingKey != null) ? mappingKey : "null"))
            .build();
        Nodes nodes = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder()
                .setNodeType(org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes.Rdm)
                .setOpenroadmVersion(OpenroadmNodeVersion._121)
                .build())
            .setCpToDegree(cpList)
            .setMapping(Map.of(map1.key(),map1))
            .build();
        return nodes;
    }

    public static void writeNodeTransaction(String nodeId, DataBroker dataBroker, String mappingKey) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        Nodes nodes = getNodes(nodeId, mappingKey);
        writeTransaction(dataBroker, nodesIID, nodes);
    }

    public static void writeNodeTransaction2(String nodeId, DataBroker dataBroker, String mappingKey) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        Nodes nodes = getNodes2(nodeId, mappingKey);
        writeTransaction(dataBroker, nodesIID, nodes);
    }

    public static void writeNodeTransaction3(String nodeId, DataBroker dataBroker, String mappingKey) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        Nodes nodes = getNodes3(nodeId, mappingKey);
        writeTransaction(dataBroker, nodesIID, nodes);
    }

}
