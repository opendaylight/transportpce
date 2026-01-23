/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Read-only repository for retrieving OpenROADM topology {@link Network} objects from a datastore.
 *
 * <p>This abstraction encapsulates the datastore lookup for an OpenROADM {@link Network} and returns the
 * result as an {@link Optional}. Implementations may wrap datastore-related failures in runtime exceptions.
 *
 * <p>Note: Despite the name, this repository is not responsible for interpreting or modifying the topology;
 * it only provides access to the stored {@link Network} object.
 */
public interface OpenRoadmTopologyRepository {

    /**
     * Reads an OpenROADM {@link Network} from the specified logical datastore.
     *
     * @param store the logical datastore type to read from (e.g. CONFIGURATION or OPERATIONAL)
     * @param iid the instance identifier of the {@link Network} to read
     * @return an {@link Optional} containing the {@link Network} if present, otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code store} or {@code iid} is {@code null}
     * @throws IllegalStateException if the read fails or the calling thread is interrupted
     */
    Optional<Network> read(LogicalDatastoreType store, DataObjectIdentifier<Network> iid);

}
