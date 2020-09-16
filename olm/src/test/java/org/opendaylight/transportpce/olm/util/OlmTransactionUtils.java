/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.oms.attributes.AmplifiedLinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.Validity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class OlmTransactionUtils {

    private OlmTransactionUtils() {
    }

    @SuppressWarnings("unchecked")
    public static void writeTransaction(DataBroker dataBroker, InstanceIdentifier instanceIdentifier,
                                        DataObject object) {
        @NonNull
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        transaction.commit();
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks
            .Network getNetworkForSpanLoss() {

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
        // create 2 openroadm-topology degree nodes, end points of the link to be
        // measured;
        SupportingNode supportingNode4RoadmA = new SupportingNodeBuilder()
                .setNetworkRef(new NetworkId("openroadm-network"))
                .setNodeRef(new NodeId("ROADM-A1")).build();
        Node ietfNodeA = new NodeBuilder().setNodeId(new NodeId("ROADM-A1-DEG2"))
                .setSupportingNode(Map.of(supportingNode4RoadmA.key(),supportingNode4RoadmA))
                .build();
        SupportingNode supportingNode4RoadmC = new SupportingNodeBuilder()
                .setNetworkRef(new NetworkId("openroadm-network"))
                .setNodeRef(new NodeId("ROADM-C1")).build();
        Node ietfNodeC = new NodeBuilder().setNodeId(new NodeId("ROADM-C1-DEG1"))
                .setSupportingNode(Map.of(supportingNode4RoadmC.key(),supportingNode4RoadmC))
                .build();
        Map<NodeKey,Node> ietfNodeMap = new HashMap<>();
        ietfNodeMap.put(ietfNodeA.key(),ietfNodeA);
        ietfNodeMap.put(ietfNodeC.key(),ietfNodeC);
        Map<AmplifiedLinkKey,AmplifiedLink>
                amplifiedLinkValues = new HashMap<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink al =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes
            .AmplifiedLinkBuilder()
                .setSectionElement(new SectionElementBuilder().setSectionElement(
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes
                            .amplified.link.section.element.section.element.SpanBuilder().setSpan(
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link
                                    .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                                        .setAdministrativeState(AdminStates.InService)
                                        .setAutoSpanloss(true)
                                        .setClfi("clfi")
                                        .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                        .setLinkConcatenation(linkConcentationValues)
                                        .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                        .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                        .build())
                                .build())
                        .build())
                .setSectionEltNumber(Uint16.valueOf(1))
                .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink al2 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes
            .AmplifiedLinkBuilder()
                .setSectionElement(new SectionElementBuilder().setSectionElement(
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes
                            .amplified.link.section.element.section.element.SpanBuilder().setSpan(
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link
                                    .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                                        .setAdministrativeState(AdminStates.InService)
                                        .setAutoSpanloss(true)
                                        .setClfi("clfi")
                                        .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                        .setLinkConcatenation(linkConcentationValues)
                                        .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                        .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                        .build())
                                .build())
                        .build())
                .setSectionEltNumber(Uint16.valueOf(1))
                .build();
        amplifiedLinkValues.put(al.key(),al);
        amplifiedLinkValues.put(al2.key(),al2);
        Map<Class<? extends Augmentation<Link>>, Augmentation<Link>> map = Collections.emptyMap();
        Augmentation<Link> aug11 = new Link1Builder().setAdministrativeGroup(Uint32.valueOf(123))
                .setAdministrativeState(State.InService)
                .setAmplified(true)
                .setLinkLatency(Uint32.valueOf(123))
                .setLinkLength(BigDecimal.valueOf(123))
                .setOMSAttributes(new OMSAttributesBuilder()
                        .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                        .setOppositeLink(new LinkId("link 1"))
                        .setSpan(new SpanBuilder().build())
                        .setTEMetric(Uint32.valueOf(123)).build())
                .setOperationalState(State.InService).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1 aug12 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder()
                .setLinkType(OpenroadmLinkType.ROADMTOROADM).build();
        Augmentation<Link> aug21 = new Link1Builder()
                .setAdministrativeGroup(Uint32.valueOf(123))
                .setAdministrativeState(State.InService)
                .setAmplified(true)
                .setLinkLatency(Uint32.valueOf(123))
                .setLinkLength(BigDecimal.valueOf(123))
                .setOMSAttributes(new OMSAttributesBuilder()
                        .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                        .setOppositeLink(new LinkId("link 1"))
                        .setSpan(new SpanBuilder().build())
                        .setTEMetric(Uint32.valueOf(123)).build())
                .setOperationalState(State.InService).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1 aug22 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder()
                .setLinkType(OpenroadmLinkType.ROADMTOROADM).build();
        // create the roadm-to-roadm link to be measured
        Link roadm2roadmLink = new LinkBuilder().setLinkId(new LinkId("ROADM-A1-to-ROADM-C1"))
            .setSource(new SourceBuilder().setSourceNode(ietfNodeA.getNodeId()).setSourceTp("DEG2-TTP-TXRX").build())
            .setDestination(
                new DestinationBuilder().setDestNode(ietfNodeC.getNodeId()).setDestTp("DEG1-TTP-TXRX").build())
            .addAugmentation(aug11)
            .addAugmentation(aug12)
            .addAugmentation(aug21)
            .addAugmentation(aug22)
            .build();
        // create the ietf network
        Network1 openroadmAugmToIetfNetwork = new Network1Builder()
                .setLink(Map.of(roadm2roadmLink.key(),roadm2roadmLink)).build();
        // openroadm Topology builder
        NetworkBuilder ietfNetworkBldr = new NetworkBuilder()
                .setNetworkId(new NetworkId("openroadm-topology"))
                .setNode(ietfNodeMap)
                .addAugmentation(openroadmAugmToIetfNetwork);

        return ietfNetworkBldr.build();
    }

    public static Mapping getMapping1() {
        return new MappingBuilder().setLogicalConnectionPoint("DEG2-TTP-TXRX").setSupportingOts("OTS-DEG2-TTP-TXRX")
                .build();
    }

    public static Mapping getMapping2() {
        return new MappingBuilder().setLogicalConnectionPoint("DEG1-TTP-TXRX").setSupportingOts("OTS-DEG1-TTP-TXRX")
                .build();
    }

    public static Optional<CurrentPmList> getCurrentPmListA() {
        Measurement measurementA = new MeasurementBuilder()
                .setGranularity(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity._15min)
                .setPmParameterValue(new PmDataType(new BigDecimal("-3.5")))
                .setValidity(Validity.Complete)
                .build();
        CurrentPm cpA = new CurrentPmBuilder()
            .setType(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum.OpticalPowerOutput)
            .setMeasurement(Map.of(measurementA.key(),measurementA))
            .build();
        InstanceIdentifier<Interface> interfaceIIDA = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey("OTS-DEG2-TTP-TXRX"));
        CurrentPmEntry currentPmEntryA = new CurrentPmEntryBuilder()
                .setCurrentPm(Map.of(cpA.key(),cpA))
                .setPmResourceInstance(interfaceIIDA)
                .setPmResourceType(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum.Interface)
                .setPmResourceTypeExtension("")
                .setRetrievalTime(new DateAndTime("2018-06-07T13:22:58+00:00"))
                .build();
        return Optional.of(new CurrentPmListBuilder()
                .setCurrentPmEntry(Map.of(currentPmEntryA.key(),currentPmEntryA)).build());
    }

    public static Optional<CurrentPmList> getCurrentPmListC() {
        Measurement measurementC = new MeasurementBuilder()
                .setGranularity(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity._15min)
                .setPmParameterValue(new PmDataType(new BigDecimal("-18.1")))
                .setValidity(Validity.Complete)
                .build();
        CurrentPm cpC = new CurrentPmBuilder()
            .setType(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum.OpticalPowerInput)
            .setMeasurement(Map.of(measurementC.key(),measurementC))
            .build();
        InstanceIdentifier<Interface> interfaceIIDC = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey("OTS-DEG1-TTP-TXRX"));
        CurrentPmEntry currentPmEntryC = new CurrentPmEntryBuilder()
                .setCurrentPm(Map.of(cpC.key(),cpC))
                .setPmResourceInstance(interfaceIIDC)
                .setPmResourceType(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum.Interface)
                .setPmResourceTypeExtension("")
                .setRetrievalTime(new DateAndTime("2018-06-07T13:22:58+00:00"))
                .build();
        return Optional.of(new CurrentPmListBuilder()
                .setCurrentPmEntry(Map.of(currentPmEntryC.key(),currentPmEntryC)).build());
    }
}
