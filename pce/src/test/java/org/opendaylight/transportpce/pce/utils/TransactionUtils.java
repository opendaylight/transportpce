/*
 * Copyright © 2020 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.pce.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes.AmplifiedLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.AmplifiedLinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementKey;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.binding.Augmentation;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class TransactionUtils {

    private TransactionUtils() {
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    public static void writeTransaction(DataBroker dataBroker, DataObjectIdentifier instanceIdentifier,
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
            .addAugmentation(new LinkConcatenation1Builder()
                .setFiberType(FiberType.Truewave)
                .setPmd(new FiberPmd(Decimal64.valueOf("1")))
                .build())
            .setSRLGId(Uint32.ONE)
            .setSRLGLength(Decimal64.valueOf("1"))
            .build();
        LinkConcatenation linkConcatenation2 = new LinkConcatenationBuilder()
            .addAugmentation(new LinkConcatenation1Builder()
                .setFiberType(FiberType.Truewave)
                .setPmd(new FiberPmd(Decimal64.valueOf("1")))
                .build())
            .setSRLGId(Uint32.ONE)
            .setSRLGLength(Decimal64.valueOf("1"))
            .build();
        linkConcentationValues.put(linkConcatenation.key(),linkConcatenation);
        linkConcentationValues.put(linkConcatenation2.key(),linkConcatenation2);
        // create 2 openroadm-topology degree nodes, end points of the link to be
        // measured
        SupportingNode supportingNodeA = new SupportingNodeBuilder().setNetworkRef(new NetworkId("openroadm-network"))
                .setNodeRef(new NodeId("ROADM-A1")).build();
        Node ietfNodeA = new NodeBuilder().setNodeId(new NodeId("ROADM-A1-DEG2"))
                .setSupportingNode(Map.of(supportingNodeA.key(),supportingNodeA))
                .addAugmentation(getNode1AugImpl()).build();
        Map<SupportingNodeKey,SupportingNode> supportingNodeListC = new HashMap<>();
        SupportingNode supportingNode = new SupportingNodeBuilder().setNetworkRef(new NetworkId("openroadm-network"))
                .setNodeRef(new NodeId("ROADM-C1")).build();
        supportingNodeListC.put(supportingNode.key(),supportingNode);
        SupportingNode supportingNode2 = new SupportingNodeBuilder().setNetworkRef(new NetworkId("clli-network"))
                .setNodeRef(new NodeId("ROADM-C2")).build();
        supportingNodeListC.put(supportingNode2.key(),supportingNode2);

        Node ietfNodeC = new NodeBuilder().setNodeId(new NodeId("ROADM-C1-DEG1")).setSupportingNode(supportingNodeListC)
                .addAugmentation(getNode1AugImpl())
                .addAugmentation(getNode1())
                .build();
        Map<NodeKey,Node> ietfNodeList = new HashMap<>();
        ietfNodeList.put(ietfNodeA.key(),ietfNodeA);
        ietfNodeList.put(ietfNodeC.key(),ietfNodeC);

        Map<AmplifiedLinkKey,AmplifiedLink> amplifiedLinkValues = new HashMap<>();
        var al = new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes
                .AmplifiedLinkBuilder()
            .setSectionElement(new SectionElementBuilder()
                .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link
                        .attributes.amplified.link.section.element.section.element.SpanBuilder()
                    .setSpan(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link
                            .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                        .setAdministrativeState(AdminStates.InService)
                        .setAutoSpanloss(true)
                        .setEngineeredSpanloss(new RatioDB(Decimal64.valueOf("1")))
                        .setLinkConcatenation(linkConcentationValues)
                        .setSpanlossBase(new RatioDB(Decimal64.valueOf("1")))
                        .setSpanlossCurrent(new RatioDB(Decimal64.valueOf("1")))
                        .build())
                    .build())
                .build())
            .setSectionEltNumber(Uint16.ONE)
            .build();
        var al2 = new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes
                .AmplifiedLinkBuilder()
            .setSectionElement(new SectionElementBuilder()
                .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link
                        .attributes.amplified.link.section.element.section.element.SpanBuilder()
                    .setSpan(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link
                            .attributes.amplified.link.section.element.section.element.span.SpanBuilder()
                        .setAdministrativeState(AdminStates.InService)
                        .setAutoSpanloss(true)
                        .setEngineeredSpanloss(new RatioDB(Decimal64.valueOf("1")))
                        .setLinkConcatenation(linkConcentationValues)
                        .setSpanlossBase(new RatioDB(Decimal64.valueOf("1")))
                        .setSpanlossCurrent(new RatioDB(Decimal64.valueOf("1")))
                        .build())
                    .build())
                .build())
            .setSectionEltNumber(Uint16.ONE)
            .build();
        amplifiedLinkValues.put(al.key(),al);
        amplifiedLinkValues.put(al2.key(),al2);
        Augmentation<Link> aug11 = new Link1Builder()
                .setAmplified(true)
                .setOMSAttributes(new OMSAttributesBuilder()
                        .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues).build())
                        .setSpan(new SpanBuilder().build())
                        .build())
                .build();
        var aug12 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder()
            .setOppositeLink(new LinkId("link 1"))
            .setLinkType(OpenroadmLinkType.ROADMTOROADM)
            .build();
        Augmentation<Link> aug21 = new Link1Builder()
            .setAmplified(true)
            .setOMSAttributes(new OMSAttributesBuilder()
                .setAmplifiedLink(new AmplifiedLinkBuilder()
                    .setAmplifiedLink(amplifiedLinkValues)
                    .build())
                .setSpan(new SpanBuilder().build())
                .build())
            .build();
        var aug22 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder()
            .setLinkType(OpenroadmLinkType.ROADMTOROADM)
            .build();
        // create the roadm-to-roadm link to be measured
        Link roadm2roadmLink = new LinkBuilder().setLinkId(new LinkId("ROADM-A1-to-ROADM-C1"))
                .setSource(new SourceBuilder().setSourceNode(ietfNodeA.getNodeId())
                        .setSourceTp(new TpId("DEG2-TTP-TXRX")).build())
                .setDestination(new DestinationBuilder().setDestNode(ietfNodeC.getNodeId())
                        .setDestTp(new TpId("DEG1-TTP-TXRX")).build())
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
                .setNode(ietfNodeList)
                .addAugmentation(openroadmAugmToIetfNetwork);

        return ietfNetworkBldr.build();
    }

    private static Augmentation<Node> getNode1AugImpl() {
        return  new Node1Builder().setNodeType(OpenroadmNodeType.DEGREE).build();
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
                .setPmParameterValue(new PmDataType(Decimal64.valueOf("-3.5")))
                .setValidity(Validity.Complete)
                .build();
        CurrentPm cpA = new CurrentPmBuilder()
                .setType(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum
                        .OpticalPowerOutput)
                .setMeasurement(Map.of(measurementA.key(),measurementA))
                .build();
        DataObjectIdentifier<Interface> interfaceIIDA = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey("OTS-DEG2-TTP-TXRX"))
            .build();
        CurrentPmEntry currentPmEntryA = new CurrentPmEntryBuilder()
                .setCurrentPm(Map.of(cpA.key(),cpA))
                .setPmResourceInstance(interfaceIIDA)
                .setPmResourceType(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum
                                .Interface)
                .setPmResourceTypeExtension("")
                .setRetrievalTime(new DateAndTime("2018-06-07T13:22:58+00:00"))
                .build();
        return Optional.of(new CurrentPmListBuilder()
                .setCurrentPmEntry(Map.of(currentPmEntryA.key(),currentPmEntryA)).build());
    }

    public static Optional<CurrentPmList> getCurrentPmListC() {
        Measurement measurementC = new MeasurementBuilder()
                .setGranularity(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity._15min)
                .setPmParameterValue(new PmDataType(Decimal64.valueOf("-18.1")))
                .setValidity(Validity.Complete)
                .build();
        Map<MeasurementKey,Measurement> measurementListC = new HashMap<>();
        measurementListC.put(measurementC.key(),measurementC);
        CurrentPm cpC = new CurrentPmBuilder()
                .setType(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum
                        .OpticalPowerInput)
                .setMeasurement(measurementListC)
                .build();
        DataObjectIdentifier<Interface> interfaceIIDC = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey("OTS-DEG1-TTP-TXRX"))
            .build();
        CurrentPmEntry currentPmEntryC = new CurrentPmEntryBuilder()
                .setCurrentPm(Map.of(cpC.key(),cpC))
                .setPmResourceInstance(interfaceIIDC)
                .setPmResourceType(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum
                                .Interface)
                .setPmResourceTypeExtension("")
                .setRetrievalTime(new DateAndTime("2018-06-07T13:22:58+00:00"))
                .build();
        return Optional.of(new CurrentPmListBuilder()
                .setCurrentPmEntry(Map.of(currentPmEntryC.key(),currentPmEntryC)).build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private static DegreeAttributes getDegAttributes() {
        return new DegreeAttributesBuilder()
                .setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available())
                .build();
    }

    private static  SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available()).build();
    }


}
