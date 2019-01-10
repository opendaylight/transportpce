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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PmDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.layerrate.rev161014.LayerRateEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.oms.attributes.AmplifiedLinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlistBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.LayerRateBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Measurements;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.measurements.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmMeasurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.pm.measurement.PmParameterNameBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

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

    public static boolean readTransaction(DataBroker dataBroker,InstanceIdentifier instanceIdentifier)
        throws ExecutionException, InterruptedException {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        Object object =  transaction.read(LogicalDatastoreType.CONFIGURATION, instanceIdentifier).get();
        return true;
    }

    public static boolean writeTransaction2(DataBroker dataBroker, InstanceIdentifier instanceIdentifier,
        DataObject object) {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, object, true);
        transaction.commit();// submit(Timeouts.DATASTORE_WRITE, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT).get();
        return true;
    }


    public static boolean writeNodeTransaction222(DataBroker dataBroker,
        KeyedInstanceIdentifier<Node, NodeKey> instanceIdentifier, Node object)
        throws ExecutionException, InterruptedException {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        transaction.submit();
        Thread.sleep(500);
        transaction = dataBroker.newReadWriteTransaction();
        Node node = transaction.read(LogicalDatastoreType.CONFIGURATION, instanceIdentifier).get().get();
        //        com.google.common.base.Optional<Node> realNode = transaction.read(LogicalDatastoreType.CONFIGURATION,
        ////            instanceIdentifier).get();
        return true;
    }

    public static boolean writeNetworkTransaction(DataBroker dataBroker,
        InstanceIdentifier<Network1> instanceIdentifier, Network1 object)
        throws ExecutionException, InterruptedException {
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        transaction.submit();
        Thread.sleep(5000);
        transaction = dataBroker.newReadWriteTransaction();
        Network1 network1 = transaction.read(LogicalDatastoreType.CONFIGURATION, instanceIdentifier).get().get();
        //        com.google.common.base.Optional<Node> realNode = transaction.read(LogicalDatastoreType.CONFIGURATION,
        ////            instanceIdentifier).get();
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
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)).build();
        SupportingLink supportingLink2 = new SupportingLinkBuilder().setLinkRef("ref2")
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)).build();
        supportingLinks.add(supportingLink);
        supportingLinks.add(supportingLink2);
        List<Link> links = new ArrayList<>();
        Link link1 = new LinkBuilder().setLinkId(new LinkId("link 1"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 1"))
                .setDestTp("dest").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 2"))
                .setSourceTp("src").build())
            .setSupportingLink(supportingLinks)
            .addAugmentation(Link1.class, aug1)
            .addAugmentation(Link1.class, aug2).build();

        Link link2 = new LinkBuilder().setLinkId(new LinkId("link 2"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 3"))
                .setDestTp("dest").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 4"))
                .setSourceTp("src").build())
            .setSupportingLink(supportingLinks).build();
        links.add(link1);
        links.add(link2);
        Network1 network = new Network1Builder().setLink(links).build();
        Optional.of(network);
        return network;
    }

    public static Network1 getNetwork3() {
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
            .setLinkType(OpenroadmLinkType.ADDLINK)
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
            .setLinkType(OpenroadmLinkType.EXPRESSLINK)
            .setOMSAttributes(new OMSAttributesBuilder()
                .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                .setOppositeLink(new LinkId("link 1"))
                .setSpan(new SpanBuilder().build())
                .setTEMetric(Long.valueOf(123)).build())
            .setOperationalState(State.InService).build();

        List<SupportingLink> supportingLinks = new ArrayList<>();
        SupportingLink supportingLink = new SupportingLinkBuilder().setLinkRef("ref1")
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)).build();
        SupportingLink supportingLink2 = new SupportingLinkBuilder().setLinkRef("ref2")
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)).build();
        supportingLinks.add(supportingLink);
        supportingLinks.add(supportingLink2);
        List<Link> links = new ArrayList<>();
        Link link1 = new LinkBuilder().setLinkId(new LinkId("link 1"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 1"))
                .setDestTp("dest").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 2"))
                .setSourceTp("src").build())
            .setSupportingLink(supportingLinks)
            .addAugmentation(Link1.class, aug1)
            .addAugmentation(Link1.class, aug2).build();

        Link link2 = new LinkBuilder().setLinkId(new LinkId("link 2"))
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("node 3"))
                .setDestTp("dest").build())
            .setSource(new SourceBuilder().setSourceNode(new NodeId("node 4"))
                .setSourceTp("src").build())
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
        NodeId node2 = new NodeId("node 2");
        NodeId node3 = new NodeId("node 3");
        NodeId node4 = new NodeId("node 4");
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

    public static void writeNodeTransaction(String nodeId, DataBroker dataBroker, String mappingKey)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        Nodes nodes = getNodes(nodeId, mappingKey);
        writeTransaction(dataBroker, nodesIID, nodes);
    }

    public static void writeNodeTransaction2(String nodeId, DataBroker dataBroker, String mappingKey)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        Nodes nodes = getNodes2(nodeId, mappingKey);
        writeTransaction(dataBroker, nodesIID, nodes);
    }

    public static void writeNodeTransaction3(String nodeId, DataBroker dataBroker, String mappingKey)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        Nodes nodes = getNodes3(nodeId, mappingKey);
        writeTransaction(dataBroker, nodesIID, nodes);
    }


    public static  <T extends DataObject> void putAndSubmit(DeviceTransactionManagerImpl deviceTxManager,
        String deviceId, LogicalDatastoreType store, InstanceIdentifier<T> path, T data)
        throws ExecutionException, InterruptedException {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTxManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx = deviceTxFuture.get().get();
        deviceTx.put(store, path, data);
        deviceTx.submit(1000, TimeUnit.MILLISECONDS);
    }

    public static void prepareTestDataInStore(DataBroker dataBroker, MountPointService mountPointService)
        throws ExecutionException, InterruptedException {
        NodeId n5 = new NodeId("node 5");
        SupportingNode sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(n5).build();
        List<SupportingNode> snl = new ArrayList<>();
        snl.add(sn);
        Node node = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
            .network.NodeBuilder().setNodeId(n5).setSupportingNode(snl).build();
        KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII2 =
            InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(n5));
        TransactionUtils.writeTransaction(dataBroker, mappedNodeII2, node);
        Thread.sleep(500);
        InstanceIdentifier<Mapping> portMappingIID2 = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
            .child(Nodes.class, new NodesKey("node 5"))
            .child(Mapping.class, new MappingKey("dest")).build();
        Mapping map2 = new MappingBuilder().setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey("dest"))
            .build();
        TransactionUtils.writeTransaction(dataBroker, portMappingIID2, map2);
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (int i = 0; i < nodes.size(); i++) {
            if (i != 0) {
                sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .setNodeRef(nodes.get(i - 1)).build();
            } else {
                sn = new SupportingNodeBuilder().setNodeRef(n5).setNetworkRef(
                    new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)).build();
            }
            snl = new ArrayList<>();
            snl.add(sn);
            node = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                .network.NodeBuilder().setNodeId(nodes.get(i)).setSupportingNode(snl).build();
            KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII =
                InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(nodes.get(i)));
            TransactionUtils.writeTransaction(dataBroker, mappedNodeII, node);
            InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
                .child(Nodes.class, new NodesKey(nodes.get(i).getValue()))
                .child(Mapping.class, new MappingKey("src")).build();
            Mapping map = new MappingBuilder().setLogicalConnectionPoint("point")
                .setSupportingCircuitPackName("circuit name")
                .setSupportingOms("oms")
                .setSupportingOts("ots")
                .setSupportingPort("port")
                .withKey(new MappingKey("src"))
                .build();
            TransactionUtils.writeTransaction(dataBroker, portMappingIID, map);
        }


        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network.class,
            overlayTopologyKey).augmentation(Network1.class).build();
        Network1 network = TransactionUtils.getNetwork2();
        TransactionUtils.writeTransaction(dataBroker, networkIID, network);

        InstanceIdentifier<CurrentPmlist> currentPmsIID = InstanceIdentifier.create(CurrentPmlist.class);
        Measurements m0 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerOutput")
                    .setType(PmNamesEnum.OpticalPowerOutput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.ONE))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll = new ArrayList<>();
        ll.add(m0);
        CurrentPm c0 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                .build()).setDevice(new DeviceBuilder().setNodeId("node 1").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll)
            .build();
        List<CurrentPm> cl = new ArrayList<>();
        cl.add(c0);
        CurrentPmlist l0 = new CurrentPmlistBuilder().setCurrentPm(cl).build();
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 1", LogicalDatastoreType.OPERATIONAL, currentPmsIID, l0);
        Measurements m2 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerInput")
                    .setType(PmNamesEnum.OpticalPowerInput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.ONE))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll2 = new ArrayList<>();
        ll2.add(m0);
        ll2.add(m2);
        CurrentPm c2 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c2")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                .build()).setDevice(new DeviceBuilder().setNodeId("node 5").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll2)
            .build();
        List<CurrentPm> cl2 = new ArrayList<>();
        cl2.add(c2);
        CurrentPmlist l2 = new CurrentPmlistBuilder().setCurrentPm(cl2).build();
        InstanceIdentifier<CurrentPmlist> currentPmsIID2 = InstanceIdentifier.create(CurrentPmlist.class);
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 5", LogicalDatastoreType.OPERATIONAL, currentPmsIID2, l2);
        Thread.sleep(1000);
    }


    public static void prepareTestDataInStore2(DataBroker dataBroker, MountPointService mountPointService)
        throws ExecutionException, InterruptedException {
        List<SupportingNode> snl;
        NodeId n5 = new NodeId("node 5");
        SupportingNode sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(n5).build();
        snl = new ArrayList<>();
        snl.add(sn);
        Node n0 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
            .network.NodeBuilder().setNodeId(n5).setSupportingNode(snl).build();
        KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII2 =
            InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(n5));
        TransactionUtils.writeTransaction(dataBroker, mappedNodeII2, n0);
        Thread.sleep(500);
        InstanceIdentifier<Mapping> portMappingIID2 = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
            .child(Nodes.class, new NodesKey("node 5"))
            .child(Mapping.class, new MappingKey("dest")).build();
        Mapping map2 = new MappingBuilder().setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey("dest"))
            .build();
        TransactionUtils.writeTransaction(dataBroker, portMappingIID2, map2);
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (int i = 0; i < nodes.size(); i++) {
            if (i != 0) {
                sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .setNodeRef(nodes.get(i - 1)).build();
            } else {
                sn = new SupportingNodeBuilder().setNodeRef(n5).setNetworkRef(
                    new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)).build();
            }
            snl = new ArrayList<>();
            snl.add(sn);
            n0 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                .network.NodeBuilder().setNodeId(nodes.get(i)).setSupportingNode(snl).build();
            KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII =
                InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(nodes.get(i)));
            TransactionUtils.writeTransaction(dataBroker, mappedNodeII, n0);
            InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
                .child(Nodes.class, new NodesKey(nodes.get(i).getValue()))
                .child(Mapping.class, new MappingKey("src")).build();
            Mapping map = new MappingBuilder().setLogicalConnectionPoint("point")
                .setSupportingCircuitPackName("circuit name")
                .setSupportingOms("oms")
                .setSupportingOts("ots")
                .setSupportingPort("port")
                .withKey(new MappingKey("src"))
                .build();
            TransactionUtils.writeTransaction(dataBroker, portMappingIID, map);

        }


        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network.class,
            overlayTopologyKey).augmentation(Network1.class).build();
        Network1 network = TransactionUtils.getNetwork2();
        TransactionUtils.writeTransaction(dataBroker, networkIID, network);

        InstanceIdentifier<CurrentPmlist> currentPmsIID = InstanceIdentifier.create(CurrentPmlist.class);
        Measurements m0 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerOutput")
                    .setType(PmNamesEnum.OpticalPowerOutput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.TEN))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll = new ArrayList<>();
        ll.add(m0);
        CurrentPm c0 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                .build()).setDevice(new DeviceBuilder().setNodeId("node 1").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll)
            .build();
        List<CurrentPm> cl = new ArrayList<>();
        cl.add(c0);
        CurrentPmlist l0 = new CurrentPmlistBuilder().setCurrentPm(cl).build();
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 1", LogicalDatastoreType.OPERATIONAL, currentPmsIID, l0);

        Measurements m2 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerInput")
                    .setType(PmNamesEnum.OpticalPowerInput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.ONE))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll2 = new ArrayList<>();
        ll2.add(m0);
        ll2.add(m2);
        CurrentPm c2 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c2")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                    .build()).setDevice(new DeviceBuilder().setNodeId("node 5").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll2)
            .build();
        List<CurrentPm> cl2 = new ArrayList<>();
        cl2.add(c2);
        CurrentPmlist l2 = new CurrentPmlistBuilder().setCurrentPm(cl2).build();
        InstanceIdentifier<CurrentPmlist> currentPmsIID2 = InstanceIdentifier.create(CurrentPmlist.class);
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 5", LogicalDatastoreType.OPERATIONAL, currentPmsIID2, l2);

        Thread.sleep(1000);
    }

    public static void prepareTestDataInStore3(DataBroker dataBroker, MountPointService mountPointService)
        throws ExecutionException, InterruptedException {
        List<SupportingNode> snl;
        NodeId n5 = new NodeId("node 5");
        SupportingNode sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(n5).build();
        snl = new ArrayList<>();
        snl.add(sn);
        Node n0 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
            .network.NodeBuilder().setNodeId(n5).setSupportingNode(snl).build();
        KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII2 =
            InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(n5));
        TransactionUtils.writeTransaction(dataBroker, mappedNodeII2, n0);
        Thread.sleep(500);
        InstanceIdentifier<Mapping> portMappingIID2 = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
            .child(Nodes.class, new NodesKey("node 5"))
            .child(Mapping.class, new MappingKey("dest")).build();
        Mapping map2 = new MappingBuilder().setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey("dest"))
            .build();
        TransactionUtils.writeTransaction(dataBroker, portMappingIID2, map2);
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (int i = 0; i < nodes.size(); i++) {
            if (i != 0) {
                sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .setNodeRef(nodes.get(i - 1)).build();
            } else {
                sn = new SupportingNodeBuilder().setNodeRef(n5).setNetworkRef(
                    new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)).build();
            }
            snl = new ArrayList<>();
            snl.add(sn);
            n0 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                .network.NodeBuilder().setNodeId(nodes.get(i)).setSupportingNode(snl).build();
            KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII =
                InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(nodes.get(i)));
            TransactionUtils.writeTransaction(dataBroker, mappedNodeII, n0);

            InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
                .child(Nodes.class, new NodesKey(nodes.get(i).getValue()))
                .child(Mapping.class, new MappingKey("src")).build();
            Mapping map = new MappingBuilder().setLogicalConnectionPoint("point")
                .setSupportingCircuitPackName("circuit name")
                .setSupportingOms("oms")
                .setSupportingOts("ots")
                .setSupportingPort("port")
                .withKey(new MappingKey("src"))
                .build();
            TransactionUtils.writeTransaction(dataBroker, portMappingIID, map);

        }

        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network.class,
            overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNetwork2();
        TransactionUtils.writeTransaction(dataBroker, networkIID, network);

        InstanceIdentifier<CurrentPmlist> currentPmsIID = InstanceIdentifier.create(CurrentPmlist.class);
        Measurements m0 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerOutput")
                    .setType(PmNamesEnum.OpticalPowerOutput).build())
                .setPmParameterValue(new PmDataType(new BigDecimal(30)))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll = new ArrayList<>();
        ll.add(m0);
        CurrentPm c0 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                .build()).setDevice(new DeviceBuilder().setNodeId("node 1").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll)
            .build();
        List<CurrentPm> cl = new ArrayList<>();
        cl.add(c0);
        CurrentPmlist l0 = new CurrentPmlistBuilder().setCurrentPm(cl).build();
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 1", LogicalDatastoreType.OPERATIONAL, currentPmsIID, l0);
        Measurements m2 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerInput")
                    .setType(PmNamesEnum.OpticalPowerInput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.ONE))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll2 = new ArrayList<>();
        ll2.add(m0);
        ll2.add(m2);
        CurrentPm c2 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c2")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                    .build())
                .setDevice(new DeviceBuilder().setNodeId("node 5").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll2)
            .build();
        List<CurrentPm> cl2 = new ArrayList<>();
        cl2.add(c2);
        CurrentPmlist l2 = new CurrentPmlistBuilder().setCurrentPm(cl2).build();
        InstanceIdentifier<CurrentPmlist> currentPmsIID2 = InstanceIdentifier.create(CurrentPmlist.class);
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 5", LogicalDatastoreType.OPERATIONAL, currentPmsIID2, l2);

        Thread.sleep(1000);
    }

    public static void prepareTestDataInStore4(DataBroker dataBroker, MountPointService mountPointService)
        throws ExecutionException, InterruptedException {
        List<SupportingNode> snl;
        NodeId n5 = new NodeId("node 5");
        SupportingNode sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(n5).build();
        snl = new ArrayList<>();
        snl.add(sn);
        Node n0 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
            .network.NodeBuilder().setNodeId(n5).setSupportingNode(snl).build();
        KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII2 =
            InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(n5));
        TransactionUtils.writeTransaction(dataBroker, mappedNodeII2, n0);
        Thread.sleep(500);
        InstanceIdentifier<Mapping> portMappingIID2 = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
            .child(Nodes.class, new NodesKey("node 5"))
            .child(Mapping.class, new MappingKey("dest")).build();
        Mapping map2 = new MappingBuilder().setLogicalConnectionPoint("point")
            .setSupportingCircuitPackName("circuit name")
            .setSupportingOms("oms")
            .setSupportingOts("ots")
            .setSupportingPort("port")
            .withKey(new MappingKey("dest"))
            .build();
        TransactionUtils.writeTransaction(dataBroker, portMappingIID2, map2);
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (int i = 0; i < nodes.size(); i++) {
            if (i != 0) {
                sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                    .setNodeRef(nodes.get(i - 1)).build();
            } else {
                sn = new SupportingNodeBuilder().setNodeRef(n5).setNetworkRef(
                    new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)).build();
            }
            snl = new ArrayList<>();
            snl.add(sn);
            n0 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                .network.NodeBuilder().setNodeId(nodes.get(i)).setSupportingNode(snl).build();
            KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII =
                InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(nodes.get(i)));
            TransactionUtils.writeTransaction(dataBroker, mappedNodeII, n0);

            InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network.class)
                .child(Nodes.class, new NodesKey(nodes.get(i).getValue()))
                .child(Mapping.class, new MappingKey("src")).build();
            Mapping map = new MappingBuilder().setLogicalConnectionPoint("point")
                .setSupportingCircuitPackName("circuit name")
                .setSupportingOms("oms")
                .setSupportingOts("ots")
                .setSupportingPort("port")
                .withKey(new MappingKey("src"))
                .build();
            TransactionUtils.writeTransaction(dataBroker, portMappingIID, map);

        }

        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network.class,
            overlayTopologyKey).augmentation(Network1.class).build();
        Network1 network = TransactionUtils.getNetwork2();
        TransactionUtils.writeTransaction(dataBroker, networkIID, network);

        Measurements m0 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerOutput")
                    .setType(PmNamesEnum.OpticalPowerOutput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.TEN))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll = new ArrayList<>();
        ll.add(m0);
        CurrentPm c0 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                .build()).setDevice(new DeviceBuilder().setNodeId("node 1").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll)
            .build();
        List<CurrentPm> cl = new ArrayList<>();
        cl.add(c0);
        CurrentPmlist l0 = new CurrentPmlistBuilder().setCurrentPm(cl).build();
        InstanceIdentifier<CurrentPmlist> currentPmsIID = InstanceIdentifier.create(CurrentPmlist.class);
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 1", LogicalDatastoreType.OPERATIONAL, currentPmsIID, l0);
        Measurements m2 = new MeasurementsBuilder()
            .setMeasurement(new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder().setExtension("OpticalPowerInput")
                    .setType(PmNamesEnum.OpticalPowerInput).build())
                .setPmParameterValue(new PmDataType(BigDecimal.ONE))
                .setPmParameterUnit("unit")
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Bidirectional)
                .build()).build();
        List<Measurements> ll2 = new ArrayList<>();
        ll2.add(m0);
        ll2.add(m2);
        CurrentPm c2 = new CurrentPmBuilder()
            .setLayerRate(new LayerRateBuilder().setExtension("ext").setType(LayerRateEnum.Layer2).build())
            .setId("c2")
            .setGranularity(PmGranularity._15min)
            .setResource(new ResourceBuilder()
                .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                    .ResourceBuilder().setResource(new InterfaceBuilder().setInterfaceName("ots").build()).build())
                .setResourceType(new ResourceTypeBuilder().setType(ResourceTypeEnum.Interface).setExtension("ext")
                    .build()).setDevice(new DeviceBuilder().setNodeId("node 5").build()).build())
            .setRetrievalTime(new DateAndTime("2019-01-01T12:00:00.456449+06:00"))
            .setMeasurements(ll2)
            .build();
        List<CurrentPm> cl2 = new ArrayList<>();
        cl2.add(c2);
        CurrentPmlist l2 = new CurrentPmlistBuilder().setCurrentPm(cl2).build();
        InstanceIdentifier<CurrentPmlist> currentPmsIID2 = InstanceIdentifier.create(CurrentPmlist.class);
        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 5", LogicalDatastoreType.OPERATIONAL, currentPmsIID2, l2);

        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey("ots"));
        Interface i0 = new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp
            .InterfaceBuilder()
            .setName("ots")
            .build();

        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 1", LogicalDatastoreType.CONFIGURATION, interfacesIID, i0);

        Thread.sleep(1000);
    }

}
