/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import com.google.common.collect.ImmutableMap;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class OpenRoadmNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmNetwork.class);

    private OpenRoadmNetwork() {
        // utility class
    }

    /**
     * Create single node entry for OpenRoadmNetwork.
     *
     * @param nodeId node ID
     * @param nodeInfo some important and general data from device
     *
     * @return node
     */
    public static Node createNode(String nodeId, NodeInfo nodeInfo) {

        Node1Builder node1Bldr = new Node1Builder();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder node2Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder();

        // Added to differenciate whenther the node is 1.2.1/2.2.1 vs 7.1.0
        int nodeInfoValue;
        if (nodeInfo.getNodeTypeOnetwo() != null) {
            nodeInfoValue = nodeInfo.getNodeTypeOnetwo().getIntValue();
        }
        else {
            nodeInfoValue = nodeInfo.getNodeTypeBeyondh().getIntValue();
        }
        /*
         * Recognize the node type: 1:ROADM, 2:XPONDER
         */
        switch (nodeInfoValue) {
            case 1:
                node2Bldr.setNodeType(OpenroadmNodeType.ROADM);
                break;
            case 2:
                node2Bldr.setNodeType(OpenroadmNodeType.XPONDER);
                break;
            default:
                LOG.error("No corresponding type for the value: {}", nodeInfo.getNodeTypeOnetwo().getName());
                break;
        }

        // Sets IP, Model and Vendor information fetched from the deviceInfo
        if (nodeInfo.getNodeIpAddress() != null) {
            node1Bldr.setIp(nodeInfo.getNodeIpAddress());
        }
        if (nodeInfo.getNodeModel() != null) {
            node1Bldr.setModel(nodeInfo.getNodeModel());
        }
        if (nodeInfo.getNodeVendor() != null) {
            node1Bldr.setVendor(nodeInfo.getNodeVendor());
        }

        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
        SupportingNode supportingNode = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
            .setNodeRef(new NodeId(nodeInfo.getNodeClli()))
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID),
                new NodeId(nodeInfo.getNodeClli())))
            .build();

        return new NodeBuilder()
            .setNodeId(new NodeId(nodeId))
            .withKey(new NodeKey(new NodeId(nodeId)))
            .setSupportingNode(ImmutableMap.of(supportingNode.key(),supportingNode))
            .addAugmentation(node1Bldr.build())
            .addAugmentation(node2Bldr.build())
            .build();
    }
}
