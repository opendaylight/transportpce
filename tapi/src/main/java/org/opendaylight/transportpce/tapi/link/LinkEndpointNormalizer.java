/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.link;

import org.opendaylight.transportpce.tapi.openroadm.topology.link.LinkTerminationPoints;

public interface LinkEndpointNormalizer {

    /**
     * Normalizes a pair of OpenROADM termination points into a source/destination
     * nodeId/terminationPointId representation.
     *
     * <p>This interface defines the contract for transforming {@link LinkTerminationPoints},
     * which contain rich OpenROADM-specific termination point information, into a simplified
     * and normalized form suitable for TAPI link modeling.
     *
     * <p>The normalization process typically involves resolving which node identifier to use
     * (e.g. {@code nodeId} vs {@code supportingNodeId}) based on the
     * {@link org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType}
     * of each termination point.
     *
     * <p>Implementations may apply different strategies or rules depending on the termination
     * point type and the intended TAPI representation.
     *
     * @param linkTerminationPoints
     *     the source and destination OpenROADM termination points to normalize
     * @return a {@link LinkEndpoints} containing the resolved source and destination
     *     nodeId/terminationPointId pairs
     */
    LinkEndpoints normalize(LinkTerminationPoints linkTerminationPoints);

}
