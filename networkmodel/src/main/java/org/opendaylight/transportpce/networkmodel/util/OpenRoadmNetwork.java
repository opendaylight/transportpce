/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev190702.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class OpenRoadmNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmNetwork.class);

    private OpenRoadmNetwork() {
        // utility class
    }

    /**
     * This public method creates the OpenRoadmNetwork Layer and posts it to the
     * controller.
     *
     * @param controllerdb controller databroker
     */
    public static void createOpenRoadmNetworkLayer(DataBroker controllerdb) {
        try {
            Network openRoadmNetwork = createOpenRoadmNetwork();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)));
            WriteTransaction wrtx = controllerdb.newWriteOnlyTransaction();
            wrtx.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), openRoadmNetwork);
            wrtx.commit().get(1, TimeUnit.SECONDS);
            LOG.info("OpenRoadm-Network created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create OpenRoadm-Network", e);
        }
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

        NodeBuilder nodeBldr = new NodeBuilder();
        NodeId nwNodeId = new NodeId(nodeId);
        nodeBldr.setNodeId(nwNodeId);
        nodeBldr.withKey(new NodeKey(nwNodeId));
        Node1Builder node1bldr = new Node1Builder();

        /*
         * Recognize the node type: 1:ROADM, 2:XPONDER
         */
        switch (nodeInfo.getNodeType().getIntValue()) {
            case 1:
                node1bldr.setNodeType(OpenroadmNodeType.ROADM);
                break;
            case 2:
                node1bldr.setNodeType(OpenroadmNodeType.XPONDER);
                break;
            default:
                LOG.error("No correponsding type for the value: {}", nodeInfo.getNodeType().getName());
                break;
        }

        // Sets IP, Model and Vendor information fetched from the deviceInfo
        if (nodeInfo.getNodeIpAddress() != null) {
            node1bldr.setIp(nodeInfo.getNodeIpAddress());
        }
        if (nodeInfo.getNodeModel() != null) {
            node1bldr.setModel(nodeInfo.getNodeModel());
        }
        if (nodeInfo.getNodeVendor() != null) {
            node1bldr.setVendor(nodeInfo.getNodeVendor());
        }

        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
        SupportingNodeBuilder supportbldr = new SupportingNodeBuilder();
        supportbldr.withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID),
            new NodeId(nodeInfo.getNodeClli())));
        supportbldr.setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID));
        supportbldr.setNodeRef(new NodeId(nodeInfo.getNodeClli()));
        nodeBldr.setSupportingNode(ImmutableList.of(supportbldr.build()));

        // Augment to the main node builder
        nodeBldr.addAugmentation(Node1.class, node1bldr.build());
        return nodeBldr.build();
    }

    /**
     * Create empty OpenROADM network.
     */
    private static Network createOpenRoadmNetwork() {
        NetworkBuilder openrdmnwBuilder = new NetworkBuilder();
        NetworkId nwId = new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID);
        openrdmnwBuilder.setNetworkId(nwId);
        openrdmnwBuilder.withKey(new NetworkKey(nwId));
        // sets network type to OpenRoadmNetwork
        NetworkTypes1Builder openRoadmNetworkTypesBldr = new NetworkTypes1Builder();
        openRoadmNetworkTypesBldr.setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build());
        NetworkTypesBuilder openrdmnwTypeBuilder = new NetworkTypesBuilder();
        openrdmnwTypeBuilder.addAugmentation(NetworkTypes1.class, openRoadmNetworkTypesBldr.build());
        openrdmnwBuilder.setNetworkTypes(openrdmnwTypeBuilder.build());
        return openrdmnwBuilder.build();
    }
}
