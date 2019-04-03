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
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.network.types.OpenroadmTopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeKey;
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
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)));
            WriteTransaction wrtx = controllerdb.newWriteOnlyTransaction();
            wrtx.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), openRoadmNetwork);
            wrtx.submit().get(1, TimeUnit.SECONDS);
            LOG.info("OpenRoadm-Network created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create OpenRoadm-Network", e);
        }
    }

    /**
     * Create single node entry for OpenRoadmNetwork.
     *
     * @param nodeId node ID
     * @param deviceTransactionManager device transaction manager
     *
     * @return node builder status
     */
    public static Node createNode(String nodeId, DeviceTransactionManager deviceTransactionManager,
                                  String openRoadmVersion) {

        InfoSubtree infoSubtree = new InfoSubtree(openRoadmVersion);
        String clli;
        String vendor;
        String model;
        IpAddress ipAddress;
        int nodeType;

        if (infoSubtree.getDeviceInfo(nodeId, deviceTransactionManager)) {

            clli = infoSubtree.getClli();

            /**
             * TODO : Uncomment when real when testing on real device
             * vendor = infoSubtree.getVendor();
             * model = infoSubtree.getModel();
             **/
            vendor = infoSubtree.getVendor();
            clli = infoSubtree.getClli();
            model = infoSubtree.getModel();
            ipAddress = infoSubtree.getIpAddress();
            nodeType = infoSubtree.getNodeType();

        } else {
            return null;
        }

        // Uses the Node Builder to set the nodeId and Key
        NodeBuilder nodeBldr = new NodeBuilder();
        NodeId nwNodeId = new NodeId(nodeId);
        nodeBldr.setNodeId(nwNodeId);
        nodeBldr.withKey(new NodeKey(nwNodeId));
        Node1Builder node1bldr = new Node1Builder();

        /*
         * Recognize the node type: 1:ROADM, 2:XPONDER
         */
        switch (nodeType) {
            case 1:
                node1bldr.setNodeType(OpenroadmNodeType.ROADM);
                break;
            case 2:
                node1bldr.setNodeType(OpenroadmNodeType.XPONDER);
                break;
            default:
                LOG.error("No correponsding type for the value: {}", nodeType);
                break;
        }


        // Sets IP, Model and Vendor information fetched from the deviceInfo
        node1bldr.setIp(ipAddress);
        node1bldr.setModel(model);
        node1bldr.setVendor(vendor);

        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute

        SupportingNodeBuilder supportbldr = new SupportingNodeBuilder();
        supportbldr.withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID), new NodeId(clli)));
        supportbldr.setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID));
        supportbldr.setNodeRef(new NodeId(clli));
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
        openRoadmNetworkTypesBldr.setOpenroadmTopology(new OpenroadmTopologyBuilder().build());
        NetworkTypesBuilder openrdmnwTypeBuilder = new NetworkTypesBuilder();
        openrdmnwTypeBuilder.addAugmentation(NetworkTypes1.class, openRoadmNetworkTypesBldr.build());
        openrdmnwBuilder.setNetworkTypes(openrdmnwTypeBuilder.build());
        return openrdmnwBuilder.build();
    }
}
