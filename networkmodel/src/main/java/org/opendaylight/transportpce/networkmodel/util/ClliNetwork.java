/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev190702.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev181130.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev181130.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev181130.networks.network.network.types.ClliNetworkBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClliNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(ClliNetwork.class);

    private ClliNetwork() {
        // utility class
    }

    /**
     * This public method creates the CLLI Layer and posts it to the controller.
     *
     * @param controllerdb   controller Databroker
     */
    public static void createClliLayer(DataBroker controllerdb) {
        try {
            Network clliNetwork = createNetwork();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)));
            WriteTransaction wrtx = controllerdb.newWriteOnlyTransaction();
            wrtx.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), clliNetwork);
            wrtx.commit().get(1, TimeUnit.SECONDS);
            LOG.info("CLLI-Network created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create CLLI-Network", e);
        }
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
        String clli = nodeInfo.getNodeClli();
        /*
         * Create node in the CLLI layer of the network model
         * with nodeId equal to the clli attribute in the device
         * model's info subtree
         */
        NodeBuilder nodeBldr = new NodeBuilder();
        NodeId nwNodeId = new NodeId(clli);
        nodeBldr.setNodeId(nwNodeId);
        nodeBldr.withKey(new NodeKey(nwNodeId));
        /*
         * create clli node augmentation
         * defined in openroadm-clli-network.yang
         */
        Node1Builder clliAugmentationBldr = new Node1Builder();
        clliAugmentationBldr.setClli(clli);
        nodeBldr.addAugmentation(Node1.class, clliAugmentationBldr.build());
        return nodeBldr.build();
    }

    /**
     * Create empty CLLI network.
     */
    private static Network createNetwork() {
        NetworkBuilder nwBuilder = new NetworkBuilder();
        NetworkId nwId = new NetworkId(NetworkUtils.CLLI_NETWORK_ID);
        nwBuilder.setNetworkId(nwId);
        nwBuilder.withKey(new NetworkKey(nwId));
        //set network type to clli
        NetworkTypes1Builder clliNetworkTypesBldr = new NetworkTypes1Builder();
        clliNetworkTypesBldr.setClliNetwork(new ClliNetworkBuilder().build());
        NetworkTypesBuilder nwTypeBuilder = new NetworkTypesBuilder();
        nwTypeBuilder.addAugmentation(NetworkTypes1.class, clliNetworkTypesBldr.build());
        nwBuilder.setNetworkTypes(nwTypeBuilder.build());
        return nwBuilder.build();
    }
}
