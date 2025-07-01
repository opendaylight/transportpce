/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.tapi.openroadm.topology.ORTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public interface OpenROADM {

    /**
     * Retrieves the OpenROADM topology from the specified datastore.
     *
     * @param configuration The logical datastore type to read from.
     * @param openroadmTopologId The identifier for the OpenROADM topology.
     * @return A set of entries representing the OpenROADM topology, where each entry contains a NodeKey and Node.
     */
    ORTopology openRoadmTopology(
            LogicalDatastoreType configuration, DataObjectIdentifier.WithKey<Network, NetworkKey> openroadmTopologId);

}
