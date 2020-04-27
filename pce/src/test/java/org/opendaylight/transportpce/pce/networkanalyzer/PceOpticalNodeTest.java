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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
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
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
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
        pceOpticalNode = new PceOpticalNode(node,
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
        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.getRdmSrgClient("pceNodeType");
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

    private NodeBuilder getNodeBuilder(List<SupportingNode> supportingNodes1) {

        //update tp of nodes
        TerminationPointBuilder xpdrTpBldr = new TerminationPointBuilder()
                .setTpId(new TpId("2"));
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.SRGTXRXPP);

        tp1Bldr.setTpType(OpenroadmTpType.SRGTXRXPP);
        xpdrTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        SrgAttributes srgAttr = new SrgAttributesBuilder().setAvailableWavelengths(create96AvalWaveSrg()).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder()
                        .setSrgAttributes(srgAttr)
                        .setDegreeAttributes(
                                (new DegreeAttributesBuilder())
                                        .setAvailableWavelengths(new ArrayList<>()).build())
                        .build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder().setTerminationPoint(ImmutableList.of(xpdrTpBldr.build())).build();



        NodeId nodeId = new NodeId("node_test");

        return new NodeBuilder()
                .setNodeId(nodeId)
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                        Node1.class, node1Rev180226)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1.class ,
                         node1)
                .setSupportingNode(supportingNodes1);
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
