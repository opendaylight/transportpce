/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;



public class PcePathDescriptionTest extends AbstractTest {

    private List<PceLink> pathAtoZ = null;
    private List<PceLink> pathZtoA = null;
    private PceResult rc;
    private Map<LinkId, PceLink> allPceLinks = null;
    private static PcePathDescription pcePathDescription = null;
    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private PceSendingPceRPCs pceSendingPceRPCs;

    @Before
    public void setup() {

        Link link = createLink("srcNode", "destNode", "srcTp", "destTp").build();


        /*PceOtnNode pceOtnNode =
        new PceOtnNode(node, OpenroadmNodeType.DEGREE, new NodeId("optical"), ServiceFormat.OTM.getName(), "test");*/
        //PceLink pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);
        /* rc = new PceResult();

        pcePathDescription = new PcePathDescription(List.of(pceLink), new HashMap<>(), rc);*/
    }


    @Test
    public void buildPceOpticalNodeRoadmTest() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.initXndrTps();
        pceOpticalNode.initWLlist();
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    private List<SupportingNode> geSupportingNodes() {
        List<SupportingNode> supportingNodes1 = new ArrayList<>();

        supportingNodes1
                .add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 2"))
                        .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                        .build());
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(List<SupportingNode> supportingNodes1) {
        return new NodeBuilder()
                .setNodeId(new NodeId("node 1"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .setSupportingNode(supportingNodes1);
    }

    private static LinkBuilder createLink(String srcNode, String destNode, String srcTp, String destTp) {
        //create source link
        SourceBuilder ietfSrcLinkBldr = new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr = new DestinationBuilder().setDestNode(new NodeId(destNode))
                .setDestTp(destTp);
        LinkId linkId = new LinkId(String.format(LINK_ID_FORMAT, srcNode, srcTp, destNode, destTp));
        return new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(linkId)
                .withKey(new LinkKey(linkId));
    }
}
