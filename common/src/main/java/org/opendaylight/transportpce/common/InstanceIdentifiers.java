/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InstanceIdentifiers {

    public static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_II =
            InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(
                    new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public static final InstanceIdentifier<Network> UNDERLAY_NETWORK_II = InstanceIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))).build();

    public static final InstanceIdentifier<Network> OVERLAY_NETWORK_II = InstanceIdentifier
        .builder(Networks.class)
        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();

    public static final InstanceIdentifier<Network> OTN_NETWORK_II = InstanceIdentifier
        .builder(Networks.class)
        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID))).build();

    public static final InstanceIdentifier<Network> CLLI_NETWORK_II = InstanceIdentifier
            .builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))).build();

    private InstanceIdentifiers() {
        // Instance should be not created
    }

}
