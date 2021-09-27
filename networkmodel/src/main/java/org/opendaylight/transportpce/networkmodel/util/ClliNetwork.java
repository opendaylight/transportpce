/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClliNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(ClliNetwork.class);

    private ClliNetwork() {
    }

    /**
     * Create single node entry for CLLI topology.
     *
     * @param deviceId device ID
     * @param nodeInfo Some important and general data from device
     *
     * @return node builder status
     */
    public static Node createNode(String deviceId, NodeInfo nodeInfo) {
        /*
         * Create node in the CLLI layer of the network model
         * with nodeId equal to the clli attribute in the device
         * model's info subtree
         */
        NodeBuilder nodeBldr = new NodeBuilder().withKey(new NodeKey(new NodeId("node1")));
        /*
         * create clli node augmentation
         * defined in openroadm-clli-network.yang
         */

        if (nodeInfo.getNodeClli() != null) {
            nodeBldr.setNodeId(new NodeId(nodeInfo.getNodeClli()))
                .withKey(new NodeKey(new NodeId(nodeInfo.getNodeClli())));
            Node1 clliAugmentation = new Node1Builder()
                .setClli(nodeInfo.getNodeClli())
                .build();
            nodeBldr.addAugmentation(clliAugmentation);
        } else {
            LOG.warn("No CLLI configured in configuration of {}", deviceId);
        }
        return nodeBldr.build();
    }
}
