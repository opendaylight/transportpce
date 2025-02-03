/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.binding.DataObjectReference;

public final class InstanceIdentifiers {

    public static final WithKey<Topology, TopologyKey> NETCONF_TOPOLOGY_II = DataObjectIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
            .build();

    public static final DataObjectIdentifier<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
        .topology.context.Topology> TAPI_NETWORK_II = DataObjectIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context
                .Topology.class, new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                .topology.context.TopologyKey(new Uuid("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7")))
            .build();

    public static final WithKey<Network, NetworkKey> OPENROADM_NETWORK_II = DataObjectIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_NETWORK)))
            .build();

    public static final WithKey<Network, NetworkKey> OPENROADM_TOPOLOGY_II = DataObjectIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
            .build();

    public static final WithKey<Network, NetworkKey> OTN_NETWORK_II = DataObjectIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OTN_NETWORK)))
            .build();

    public static final WithKey<Network, NetworkKey> CLLI_NETWORK_II = DataObjectIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.CLLI_NETWORK)))
            .build();

    public static final DataObjectReference<Nodes> PORTMAPPING_NODE_II = DataObjectReference
            .builder(
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.Network.class)
            .child(Nodes.class)
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
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
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
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
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
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .build();
    }
}
