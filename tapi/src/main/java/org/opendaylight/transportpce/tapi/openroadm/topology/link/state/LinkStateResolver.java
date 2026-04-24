/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.state;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;

/**
 * Resolves TAPI {@link LinkStateAttributes} for a given OpenROADM {@link Link}.
 *
 * <p>This interface defines a strategy for deriving administrative and operational
 * state based on information available in the provided network topology. Implementations
 * may consider both the given link and its associated opposite link (if present)
 * when determining the resulting state.</p>
 *
 * <p>The resolved state is mapped to TAPI representations by the configured
 * {@link LinkStateMapper}.
 */
public interface LinkStateResolver {

    /**
     * Resolves the TAPI link state for the given OpenROADM link.
     *
     * @param link the OpenROADM link for which state should be resolved
     * @param topology the network topology containing the link and its relationships
     * @return resolved {@link LinkStateAttributes} containing administrative and operational state
     */
    LinkStateAttributes resolve(Link link, Network topology);

}
