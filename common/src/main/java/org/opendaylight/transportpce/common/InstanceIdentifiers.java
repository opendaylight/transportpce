/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240611.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.Builder.WithKey;

public final class InstanceIdentifiers {

    public static final WithKey<Topology, TopologyKey> NETCONF_TOPOLOGY_II = DataObjectIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public static final DataObjectIdentifier<Network> UNDERLAY_NETWORK_II = DataObjectIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
            .build();

    public static final DataObjectIdentifier.Builder<Network> OVERLAY_NETWORK_II = DataObjectIdentifier
        .builder(Networks.class)
        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));

    public static final DataObjectIdentifier<Network> OTN_NETWORK_II = DataObjectIdentifier
        .builder(Networks.class)
        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
        .build();

    public static final DataObjectIdentifier<Network> CLLI_NETWORK_II = DataObjectIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
            .build();

    private InstanceIdentifiers() {
        // Instance should be not created
    }

    /**
     * Get an instance identifier related to network termination point.
     * @param nodeId String
     * @param tpId String
     * @return InstanceIdentifier
     */
    public static DataObjectIdentifier<TerminationPoint1> createNetworkTerminationPoint1IIDBuilder(
            String nodeId, String tpId) {
        return DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.node.TerminationPoint.class,
                    new TerminationPointKey(new TpId(tpId)))
                .augmentation(TerminationPoint1.class)
                .build();
    }

    public static DataObjectIdentifier<TerminationPoint> createNetworkTerminationPointIIDBuilder(
            String nodeId, String tpId) {
        return DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.node.TerminationPoint.class,
                    new TerminationPointKey(new TpId(tpId)))
                .build();
    }


    /**
     * Get an instance identifier related to network termination point.
     * @param nodeId String
     * @return InstanceIdentifier
     */
    public static DataObjectIdentifier<Node1> createNodeIIDBuilder(String nodeId) {
        return DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .build();
    }

}
