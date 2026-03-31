/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;

/**
 * Factory for extracting {@link LinkTerminationPoints} from a given {@link Link}.
 *
 * <p>This abstraction encapsulates the logic required to resolve source and destination
 * {@link org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId}
 * instances from a network topology {@link Link}, using the provided {@link Network}
 * context.
 */
public interface LinkTerminationPointsFactory {

    /**
     * Creates {@link LinkTerminationPoints} from the given {@link Link}.
     *
     * @param link the topology link from which to extract termination points
     * @param network the network context used for resolving termination point types
     * @return a {@link LinkTerminationPoints} instance containing resolved source and
     *         destination termination points.
     */
    LinkTerminationPoints fromLink(Link link, Network network);

}
