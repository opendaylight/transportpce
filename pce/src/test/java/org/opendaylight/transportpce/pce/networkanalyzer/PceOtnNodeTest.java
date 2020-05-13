/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;



public class PceOtnNodeTest extends AbstractTest {

    private PceOtnNode pceOtnNode;
    private Node node;

    @Before
    public void setUp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK);
        node = node1Builder.build();

    }

    @Test
    public void testInitXndrTpsODU4() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), "ODU4");
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertTrue(pceOtnNode.isValid());
        Assert.assertFalse(pceOtnNode.checkWL(5L));
        Assert.assertNotNull(pceOtnNode.getAvailableTribPorts());
    }

    @Test
    public void testInitXndrTps10GE() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), "10GE");
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse(pceOtnNode.isValid());
    }

    @Test
    public void testInitXndrTps10GXponderClient1() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT);
        node = node1Builder.build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.ROADM,
                new NodeId("optical"), ServiceFormat.OMS.getName(), "10GE");
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse(pceOtnNode.isValid());
    }

    @Test
    public void testInitXndrTps1GXponderClient() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT);
        node = node1Builder.build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), "1GE");
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse(pceOtnNode.isValid());
    }

    @Test
    public void testInitXndrTps10GXponderClient() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), "10GE");
        pceOtnNode.validateXponder("optical", "sl");
        pceOtnNode.validateXponder("not optical", "sl");
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse(pceOtnNode.isValid());
        Assert.assertFalse(pceOtnNode.checkTP("tp"));

    }

    private List<SupportingNode> geSupportingNodes() {
        List<SupportingNode> supportingNodes1 = new ArrayList<>();
        supportingNodes1
                .add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 1"))
                        .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                        .build());

        supportingNodes1
                .add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 2"))
                        .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                        .build());
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(List<SupportingNode> supportingNodes1, OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());

        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1.class,
                createAnother2TerminationPoint(openroadmTpType).build());
        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1.class,
                createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(ImmutableList.of(xpdrTpBldr.build()))
                        .build();


        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeIetf =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1Builder()
                        .setTerminationPoint(ImmutableList.of(xpdrTpBldr.build()))
                        .build();

        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                        Node1.class, node1Rev180226)
                .addAugmentation(
                        org.opendaylight
                                .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1.class,
                        node1)
                .addAugmentation(
                        org.opendaylight
                                .yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                        nodeIetf
                )
                .setSupportingNode(supportingNodes1);
    }

    private NodeBuilder getNodeBuilderEmpty(List<SupportingNode> supportingNodes1, OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1.class,
                createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1Empty();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(ImmutableList.of(xpdrTpBldr.build()))
                        .build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                        Node1.class, node1Rev180226)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1.class,
                        node1)
                .setSupportingNode(supportingNodes1);
    }

    private org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 getNode1Empty() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder()
                .setSrgAttributes(getEmptySrgAttributes())
                .setDegreeAttributes(getEmptyDegAttributes())
                .build();
    }

    private DegreeAttributes getDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailableWavelengths(
                        Collections.singletonList(new AvailableWavelengthsBuilder()
                                .setIndex(20L)
                                .build()))
                .build();
    }

    private SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailableWavelengths(create96AvalWaveSrg()).build();
    }

    private DegreeAttributes getEmptyDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailableWavelengths(
                        new ArrayList<>())
                .build();
    }

    private SrgAttributes getEmptySrgAttributes() {
        List<org.opendaylight.yang.gen
                .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths>
                waveList = new ArrayList<>();
        return new SrgAttributesBuilder().setAvailableWavelengths(waveList).build();
    }

    private TerminationPointBuilder getTerminationPointBuilder(OpenroadmTpType openroadmTpType) {
        return new TerminationPointBuilder()
                .setTpId(new TpId("2"))
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130
                                .TerminationPoint1.class,
                        createOTNTerminationPoint(openroadmTpType).build());
    }

    private TerminationPoint1Builder getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {

        return new TerminationPoint1Builder()
                .setTpType(openroadmTpType);

    }

    private org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder createAnotherTerminationPoint(
            OpenroadmTpType openroadmTpType
    ) {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder()
                .setTpType(openroadmTpType)
                .setCtpAttributes((new CtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setCpAttributes((new CpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(new ArrayList<>()).build())
                .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
                .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build())
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId("destNode" + "--" + "destTp").build());
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130
            .TerminationPoint1Builder createOTNTerminationPoint(OpenroadmTpType openroadmTpType) {

        SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(IfOCHOTU4ODU4.class)
                .build();

        SupportedInterfaceCapability supIfCapa1 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If100GEODU4.class)
                .build();
        SupportedInterfaceCapability supIfCapa2 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If10GEODU2e.class)
                .build();
        SupportedInterfaceCapability supIfCapa3 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If1GEODU0.class)
                .build();

        List<SupportedInterfaceCapability> supIfCapaList = new ArrayList<>();
        supIfCapaList.add(supIfCapa);
        supIfCapaList.add(supIfCapa1);
        supIfCapaList.add(supIfCapa2);
        supIfCapaList.add(supIfCapa3);

        TpSupportedInterfaces tpSupIf = new TpSupportedInterfacesBuilder()
                .setSupportedInterfaceCapability(supIfCapaList)
                .build();

        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130
                .TerminationPoint1Builder()
                .setTpType(openroadmTpType)
                .setTpSupportedInterfaces(tpSupIf)
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build());
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1Builder createAnother2TerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1Builder()
                .setTpType(openroadmTpType);
    }

    private static List<org.opendaylight.yang.gen
            .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths> create96AvalWaveSrg() {

        List<org.opendaylight.yang.gen
                .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths>
                waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            org.opendaylight.yang.gen
                    .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengthsBuilder
                    avalBldr = new org.opendaylight.yang.gen
                    .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengthsBuilder()
                    .setIndex(Uint32.valueOf(i))
                    .withKey(new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                            .AvailableWavelengthsKey(Uint32.valueOf(i)));
            waveList.add(avalBldr.build());
        }
        return waveList;
    }

}
