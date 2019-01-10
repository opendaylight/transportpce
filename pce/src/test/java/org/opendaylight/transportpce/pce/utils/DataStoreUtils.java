/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class DataStoreUtils {


    public static void writeTopologyIntoDataStore(DataBroker dataBroker, Network network)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier
            .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
        WriteTransaction writeOnlyTransaction = dataBroker.newWriteOnlyTransaction();
        writeOnlyTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, network);
        writeOnlyTransaction.submit().get();
    }

    public static Network getEmptyNetwork(){
        Augmentation<Network> aug = new Network1Builder().build();
        Network network = new NetworkBuilder()
            .setNetworkId(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .addAugmentation(Network1.class, aug)
            .setNetworkTypes(new NetworkTypesBuilder().build())
            .build();
        return network;
    }

    public static Network getNetwork1(){
        List<Node> nodes = new ArrayList<>();
        Node node1 = new NodeBuilder()
            .setNodeId(new NodeId("node 1"))
            .build();
        nodes.add(node1);
        Augmentation<Network> aug = new Network1Builder().build();
        Network network = new NetworkBuilder()
            .setNetworkId(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNode(nodes)
            .addAugmentation(Network1.class, aug)
            .setNetworkTypes(new NetworkTypesBuilder().build())
            .build();
        return network;
    }

    public static Network getNetwork2(){
        List<Node> nodes = new ArrayList<>();
        Augmentation<Network> aug = new Network1Builder().build();
        Network network = new NetworkBuilder()
            .setNetworkId(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNode(nodes)
            .addAugmentation(Network1.class, aug)
            .setNetworkTypes(new NetworkTypesBuilder().build())
            .build();
        return network;
    }

    public static Network getNetwork3(){
        List<Link> links = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        Augmentation<Network> aug = new Network1Builder().setLink(links).build();
        Network network = new NetworkBuilder()
            .setNetworkId(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNode(nodes)
            .addAugmentation(Network1.class, aug)
            .setNetworkTypes(new NetworkTypesBuilder().build())
            .build();
        return network;
    }

    public static Network getNetwork4(){
        List<Link> links = new ArrayList<>();
        Link link1 = new LinkBuilder()
            .setLinkId(new LinkId("link1"))
            .build();
        links.add(link1);
        List<Node> nodes = new ArrayList<>();
        Node node1 = null;
        nodes.add(node1);
        Augmentation<Network> aug = new Network1Builder().setLink(links).build();
        Network network = new NetworkBuilder()
            .setNetworkId(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNode(nodes)
            .addAugmentation(Network1.class, aug)
            .setNetworkTypes(new NetworkTypesBuilder().build())
            .build();
        return network;
    }
}
