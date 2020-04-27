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



public class PceOpticalNodeTest extends AbstractTest {

    private PceOpticalNode pceOpticalNode;
    private Node node;

    @Before
    public void setUp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXRXPP);
        node = node1Builder.build();
    }

    @Test
    public void isValidTest() {
        OpenroadmNodeType nodeType = OpenroadmNodeType.ROADM;
        ServiceFormat serviceFormat = ServiceFormat.Ethernet;
        String pceNodeType = "pceNodeType";
        pceOpticalNode = new PceOpticalNode(node, nodeType, new NodeId("node_test"), serviceFormat, pceNodeType);
        Assert.assertTrue(pceOpticalNode.isValid());
    }

    @Test
    public void testInitSrgTps() {

        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.initXndrTps();
        pceOpticalNode.initWLlist();
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
        Assert.assertNull(pceOpticalNode.getAvailableTribPorts());
        Assert.assertNull(pceOpticalNode.getAvailableTribPorts());
        Assert.assertNull(pceOpticalNode.getXpdrClient("test"));
    }

    @Test
    public void testInitXndrTpSrgTypes() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(specificNode,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initWLlist();
    }

    @Test
    public void testInitXndrTpDegTypes() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.DEGREE, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initWLlist();
    }

    @Test
    public void testInitXndrTpXpondrTypes() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.XPONDER, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initWLlist();
    }

    @Test
    public void testInitWLlist() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initXndrTps();
    }

    @Test
    public void testGetRdmSrgClient() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXRXPP);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(specificNode,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.getRdmSrgClient("7");
    }

    @Test
    public void testGetRdmSrgClientDeg() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXRXPP);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(specificNode,
                OpenroadmNodeType.DEGREE, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.getRdmSrgClient("7");
    }

    @Test
    public void testGetRdmSrgClientsrgtxcp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXCP);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(specificNode,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.getRdmSrgClient("5");
    }

    @Test
    public void testGetRdmSrgClientDegreerxtpp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.DEGREERXTTP);
        node = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.getRdmSrgClient("2");
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
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder();
        xpdrTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1.class,
                createAnotherTerminationPoint().build());
        SrgAttributes srgAttr = getSrgAttributes();
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1(srgAttr);
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
            .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 getNode1(SrgAttributes srgAttr) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder()
                .setSrgAttributes(srgAttr)
                .setDegreeAttributes(
                        (new DegreeAttributesBuilder())
                                .setAvailableWavelengths(
                                        Collections.singletonList(new AvailableWavelengthsBuilder()
                                                .setIndex(20L)
                                                .build()))
                                .build())
                .build();
    }

    private SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailableWavelengths(create96AvalWaveSrg()).build();
    }

    private TerminationPointBuilder getTerminationPointBuilder() {
        return new TerminationPointBuilder()
                .setTpId(new TpId("2"));
    }

    private TerminationPoint1Builder getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {

        return new TerminationPoint1Builder()
                .setTpType(openroadmTpType)
                ;
    }

    private org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder createAnotherTerminationPoint() {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.XPONDERNETWORK)
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

    private static List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node
            .attributes.AvailableWavelengths> create96AvalWaveSrg() {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                .AvailableWavelengths> waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengthsBuilder avalBldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.srg
                    .rev181130.srg.node.attributes.AvailableWavelengthsBuilder()
                    .setIndex(Uint32.valueOf(i))
                    .withKey(new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                            .AvailableWavelengthsKey(Uint32.valueOf(i)));
            waveList.add(avalBldr.build());
        }
        return waveList;
    }
}
