/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;

import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

public final class WaveLengthServiceUtils {

    private WaveLengthServiceUtils() {

    }

    private static InstanceIdentifierBuilder<TerminationPoint1> createTerminationPoint1IIDBuilder(
            String nodeId, String tpId) {
        return InstanceIdentifier
            .builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId)))
            .augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                .network.node.TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
            .augmentation(TerminationPoint1.class);
    }

    private static InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .TerminationPoint1> createTerminationPoint2IIDBuilder(
                String nodeId, String tpId) {
        return InstanceIdentifier
            .builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId)))
            .augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                .network.node.TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .TerminationPoint1.class);
    }

    public static void putTerminationPoint1ToDatastore(String nodeId, String tpId, TerminationPoint1 terminationPoint1,
            DeviceTransactionManager deviceTransactionManager)
            throws ExecutionException, InterruptedException {
        TransactionUtils
            .writeTransaction(deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION,
                createTerminationPoint1IIDBuilder(nodeId, tpId).build(), terminationPoint1);
    }

    public static void putTerminationPoint2ToDatastore(String nodeId, String tpId,
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1
                terminationPoint1, DeviceTransactionManager deviceTransactionManager)
            throws ExecutionException, InterruptedException {
        TransactionUtils
            .writeTransaction(deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION,
                createTerminationPoint2IIDBuilder(nodeId, tpId).build(), terminationPoint1);
    }

    public static TerminationPoint1 getTerminationPoint1FromDatastore(String nodeId, String tpId,
            DeviceTransactionManager deviceTransactionManager)
            throws ExecutionException, InterruptedException {
        InstanceIdentifier<TerminationPoint1> tpIID = createTerminationPoint1IIDBuilder(nodeId, tpId).build();
        return (TerminationPoint1) TransactionUtils
            .readTransaction(deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION, tpIID);
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .TerminationPoint1 getTerminationPoint2FromDatastore(String nodeId, String tpId,
            DeviceTransactionManager deviceTransactionManager)
            throws ExecutionException, InterruptedException {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1>
            tpIID = createTerminationPoint2IIDBuilder(nodeId, tpId).build();
        return (org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1)
             TransactionUtils
                .readTransaction(deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION, tpIID);
    }

    private static InstanceIdentifier<Node1> createNode1IID(String nodeId) {
        return InstanceIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId)))
            .augmentation(Node1.class)
            .build();
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
        .Node1> createNode2IID(String nodeId) {
        return InstanceIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId)))
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
            .build();
    }

    public static void putNode1ToDatastore(String nodeId, Node1 node1,
        DeviceTransactionManager deviceTransactionManager)
        throws ExecutionException, InterruptedException {
        InstanceIdentifier<Node1> nodeIID = createNode1IID(nodeId);
        TransactionUtils
            .writeTransaction(deviceTransactionManager, nodeId,
                LogicalDatastoreType.CONFIGURATION, nodeIID, node1);
    }

    public static void putNode2ToDatastore(String nodeId,
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1 node1,
            DeviceTransactionManager deviceTransactionManager)
           throws ExecutionException, InterruptedException {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1> nodeIID =
            createNode2IID(nodeId);
        TransactionUtils
            .writeTransaction(deviceTransactionManager, nodeId,
                LogicalDatastoreType.CONFIGURATION, nodeIID, node1);
    }

    public static Node1 getNode1FromDatastore(String nodeId, DeviceTransactionManager deviceTransactionManager)
            throws ExecutionException, InterruptedException {
        InstanceIdentifier<Node1> nodeIID = createNode1IID(nodeId);
        return (Node1) TransactionUtils
            .readTransaction(deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION, nodeIID);
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .Node1 getNode2FromDatastore(String nodeId, DeviceTransactionManager deviceTransactionManager)
            throws ExecutionException, InterruptedException {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .Node1> nodeIID = createNode2IID(nodeId);
        return (org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1) TransactionUtils
            .readTransaction(deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION, nodeIID);
    }
}
