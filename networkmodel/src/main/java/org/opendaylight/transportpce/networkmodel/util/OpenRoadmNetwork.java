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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev200529.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
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
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder node2Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder();

        /*
         * Recognize the node type: 1:ROADM, 2:XPONDER
         */
        switch (nodeInfo.getNodeType().getIntValue()) {
            case 1:
                node2Bldr.setNodeType(OpenroadmNodeType.ROADM);
                break;
            case 2:
                node2Bldr.setNodeType(OpenroadmNodeType.XPONDER);
                break;
            default:
                LOG.error("No correponsding type for the value: {}", nodeInfo.getNodeType().getName());
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
