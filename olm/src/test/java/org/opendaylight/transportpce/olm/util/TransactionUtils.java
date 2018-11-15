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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.oms.attributes.AmplifiedLinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SupportingLinkBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class TransactionUtils {

    private TransactionUtils() {

    }

    public static boolean writeTransaction(DataBroker dataBroker, InstanceIdentifier instanceIdentifier,
        DataObject object) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object, true);
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
        Network1 network = new Network1Builder().setLink(null).build();
        Optional.of(network);
        return network;
    }

    public static Network1 getEmptyNetwork() {
        Network1 network = new Network1Builder().setLink(new ArrayList<>()).build();
        Optional.of(network);
        return network;
    }

    public static Network1 getNetwork() {
        List<SupportingLink> supportingLinks = new ArrayList<>();
        SupportingLink supportingLink1 = new SupportingLinkBuilder().setLinkRef("ref1")
            .setNetworkRef(new NetworkId("net1")).build();
        SupportingLink supportingLink2 = new SupportingLinkBuilder().setLinkRef("ref2")
            .setNetworkRef(new NetworkId("net2")).build();
        supportingLinks.add(supportingLink1);
        supportingLinks.add(supportingLink2);
        List<Link> links = new ArrayList<>();
        Link link1 = new LinkBuilder().setLinkId(new LinkId("link 1"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 1"))
                .setDestTp("dest tp").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 2"))
                .setSourceTp("src tp").build())
            .setSupportingLink(supportingLinks).build();

        Link link2 = new LinkBuilder().setLinkId(new LinkId("link 2"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 3"))
                .setDestTp("dest tp").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 4"))
                .setSourceTp("src tp").build())
            .setSupportingLink(supportingLinks).build();
        links.add(link1);
        links.add(link2);
        Network1 network = new Network1Builder().setLink(links).build();
        Optional.of(network);
        return network;
    }

    public static Network1 getNetwork2() {
        List<LinkConcatenation> linkConcentationValues = new ArrayList<>();
        LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
            .setFiberType(LinkConcatenation.FiberType.Truewave)
            .setPmd(new FiberPmd(BigDecimal.ONE))
            .setSRLGId(Long.valueOf(1))
            .setSRLGLength(Long.valueOf(1))
            .build();
        LinkConcatenation linkConcatenation2 = new LinkConcatenationBuilder()
            .setFiberType(LinkConcatenation.FiberType.Truewave)
            .setPmd(new FiberPmd(BigDecimal.ONE))
            .setSRLGId(Long.valueOf(1))
            .setSRLGLength(Long.valueOf(1))
            .build();
        linkConcentationValues.add(linkConcatenation);
        linkConcentationValues.add(linkConcatenation2);
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes.AmplifiedLink>
            amplifiedLinkValues = new ArrayList<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes.AmplifiedLink al = new
            org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes
                .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link
                    .attributes.amplified.link.section.element.section.element.SpanBuilder()
                        .setSpan(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link
                                .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                                    .setAdministrativeState(State.InService)
                                    .setAutoSpanloss(true)
                                    .setClfi("clfi")
                                    .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                    .setLinkConcatenation(linkConcentationValues)
                                    .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                    .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                    .build())
                    .build())
                .build())
            .setSectionEltNumber(Integer.valueOf(1)).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes.AmplifiedLink al2 = new
            org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes
                .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link
                     .attributes.amplified.link.section.element.section.element.SpanBuilder()
                        .setSpan(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link
                                .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                                    .setAdministrativeState(State.InService)
                                    .setAutoSpanloss(true)
                                    .setClfi("clfi")
                                    .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                    .setLinkConcatenation(linkConcentationValues)
                                    .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                    .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                    .build())
                                    .build())
            .build())
            .setSectionEltNumber(Integer.valueOf(1)).build();
        amplifiedLinkValues.add(al);
        amplifiedLinkValues.add(al2);
        Map<Class<? extends Augmentation<Link>>, Augmentation<Link>> map = Collections.emptyMap();
        Augmentation<Link> aug1 = new Link1Builder().setAdministrativeGroup(Long.valueOf(123))
            .setAdministrativeState(State.InService)
            .setAmplified(true)
            .setLinkLatency(Long.valueOf(123))
            .setLinkLength(BigDecimal.valueOf(123))
            .setLinkType(OpenroadmLinkType.ROADMTOROADM)
            .setOMSAttributes(new OMSAttributesBuilder()
                .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                .setOppositeLink(new LinkId("link 1"))
                .setSpan(new SpanBuilder().build())
                .setTEMetric(Long.valueOf(123)).build())
            .setOperationalState(State.InService).build();
        Augmentation<Link> aug2 = new Link1Builder().setAdministrativeGroup(Long.valueOf(123))
            .setAdministrativeState(State.InService)
            .setAmplified(true)
            .setLinkLatency(Long.valueOf(123))
            .setLinkLength(BigDecimal.valueOf(123))
            .setLinkType(OpenroadmLinkType.ROADMTOROADM)
            .setOMSAttributes(new OMSAttributesBuilder()
                .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                .setOppositeLink(new LinkId("link 1"))
                .setSpan(new SpanBuilder().build())
                .setTEMetric(Long.valueOf(123)).build())
            .setOperationalState(State.InService).build();

        List<SupportingLink> supportingLinks = new ArrayList<>();
        SupportingLink supportingLink = new SupportingLinkBuilder().setLinkRef("ref1")
            .setNetworkRef(new NetworkId("net1")).build();
        SupportingLink supportingLink2 = new SupportingLinkBuilder().setLinkRef("ref2")
            .setNetworkRef(new NetworkId("net1")).build();
        supportingLinks.add(supportingLink);
        supportingLinks.add(supportingLink2);
        List<Link> links = new ArrayList<>();
        Link link1 = new LinkBuilder().setLinkId(new LinkId("link 1"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 1"))
                .setDestTp("dest tp").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 2"))
                .setSourceTp("src tp").build())
            .setSupportingLink(supportingLinks)
            .addAugmentation(Link1.class, aug1)
            .addAugmentation(Link1.class, aug2).build();

        Link link2 = new LinkBuilder().setLinkId(new LinkId("link 2"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 3"))
                .setDestTp("dest tp").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 4"))
                .setSourceTp("src tp").build())
            .setSupportingLink(supportingLinks).build();
        links.add(link1);
        links.add(link2);
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
        List<CpToDegree> cpList = new ArrayList<>();
        CpToDegree cp1 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name")
            .setDegreeNumber(Long.valueOf(123))
            .build();
        CpToDegree cp2 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name")
            .setDegreeNumber(Long.valueOf(123))
            .build();
        cpList.add(cp1);
        cpList.add(cp2);
        List<Mapping> mappingList = new ArrayList<>();
        Mapping map1 = new MappingBuilder()
            .setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey((mappingKey != null) ? mappingKey : "null"))
            .build();
        mappingList.add(map1);
        Nodes nodes = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeType(NodeTypes.Xpdr)
            .setCpToDegree(cpList)
            .setMapping(mappingList)
            .build();
        return nodes;
    }

    public static Nodes getNodes2(String nodeId, String mappingKey) {
        List<CpToDegree> cpList = new ArrayList<>();
        CpToDegree cp1 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name")
            .setDegreeNumber(Long.valueOf(123))
            .build();
        CpToDegree cp2 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name")
            .setDegreeNumber(Long.valueOf(123))
            .build();
        cpList.add(cp1);
        cpList.add(cp2);
        List<Mapping> mappingList = new ArrayList<>();
        Mapping map1 = new MappingBuilder()
            .setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey((mappingKey != null) ? mappingKey : "null"))
            .build();
        mappingList.add(map1);
        Nodes nodes = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeType(null)
            .setCpToDegree(cpList)
            .setMapping(mappingList)
            .build();
        return nodes;
    }

    public static Nodes getNodes3(String nodeId, String mappingKey) {
        List<CpToDegree> cpList = new ArrayList<>();
        CpToDegree cp1 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name")
            .setDegreeNumber(Long.valueOf(123))
            .build();
        CpToDegree cp2 = new CpToDegreeBuilder()
            .setCircuitPackName("circuit name")
            .setDegreeNumber(Long.valueOf(123))
            .build();
        cpList.add(cp1);
        cpList.add(cp2);
        List<Mapping> mappingList = new ArrayList<>();
        Mapping map1 = new MappingBuilder()
            .setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey((mappingKey != null) ? mappingKey : "null"))
            .build();
        mappingList.add(map1);
        Nodes nodes = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeType(NodeTypes.Rdm)
            .setCpToDegree(cpList)
            .setMapping(mappingList)
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
