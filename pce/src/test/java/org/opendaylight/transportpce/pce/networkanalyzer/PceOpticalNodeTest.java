/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengths;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
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
        Assert.assertTrue(pceOpticalNode.isValid());
        Assert.assertTrue(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testInitXndrTpDegTypes() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.DEGREE, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initWLlist();
        Assert.assertTrue(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testInitXndrTpXpondrTypes() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.XPONDER, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initWLlist();
        Assert.assertTrue(pceOpticalNode.isValid());
        Assert.assertTrue(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testInitWLlist() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initXndrTps();
        pceOpticalNode.initWLlist();
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testGetRdmSrgClient() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        Assert.assertNull(pceOpticalNode.getRdmSrgClient("7"));
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testGetRdmSrgClientEmpty() {
        NodeBuilder node1Builder = getNodeBuilderEmpty(geSupportingNodes(), OpenroadmTpType.SRGTXRXPP);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(specificNode,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.initWLlist();
        pceOpticalNode.initXndrTps();
        Assert.assertNull(pceOpticalNode.getRdmSrgClient("7"));
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testGetRdmSrgClientDeg() {
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.DEGREE, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        Assert.assertNull(pceOpticalNode.getRdmSrgClient("7"));
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test
    public void testGetRdmSrgClientsrgtxcp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.SRGTXCP);
        Node specificNode = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(specificNode,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
        Assert.assertNull(pceOpticalNode.getRdmSrgClient("5"));
    }

    @Test
    public void testGetRdmSrgClientDegreerxtpp() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.DEGREERXTTP);
        node = node1Builder.build();
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        Assert.assertNull(pceOpticalNode.getRdmSrgClient("2"));
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    private Map<SupportingNodeKey,SupportingNode> geSupportingNodes() {
        Map<SupportingNodeKey,SupportingNode> supportingNodes1 = new HashMap<>();
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                .build();
        supportingNodes1
                .put(supportingNode1.key(),supportingNode1);

        SupportingNode supportingNode2 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 2"))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        supportingNodes1
                .put(supportingNode2.key(),supportingNode2);
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
            OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder();
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint().build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .setSupportingNode(supportingNodes1);
    }

    private NodeBuilder getNodeBuilderEmpty(Map<SupportingNodeKey,SupportingNode>  supportingNodes1,
            OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder();
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint().build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1Empty();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
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
        AvailableWavelengths aval = new AvailableWavelengthsBuilder().setIndex(Uint32.valueOf(20)).build();
        return (new DegreeAttributesBuilder())
                .setAvailableWavelengths(Map.of(aval.key(),aval))
                .build();
    }

    private SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailableWavelengths(TransactionUtils.create96AvalWaveSrg()).build();
    }

    private DegreeAttributes getEmptyDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailableWavelengths(Map.of())
                .build();
    }

    private SrgAttributes getEmptySrgAttributes() {
        return new SrgAttributesBuilder().setAvailableWavelengths(Map.of()).build();
    }

    private TerminationPointBuilder getTerminationPointBuilder() {
        return new TerminationPointBuilder().setTpId(new TpId("2"));
    }

    private TerminationPoint1Builder getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {

        return new TerminationPoint1Builder().setTpType(openroadmTpType);

    }

    private org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder createAnotherTerminationPoint() {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.XPONDERNETWORK)
                .setCtpAttributes((new CtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setCpAttributes((new CpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(Map.of()).build())
                .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
                .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build())
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId("destNode" + "--" + "destTp").build());
    }

}
