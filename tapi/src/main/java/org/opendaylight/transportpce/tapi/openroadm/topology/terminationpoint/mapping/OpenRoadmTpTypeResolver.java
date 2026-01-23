/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;

/**
 * Resolves OpenROADM termination point identity information from an OpenROADM topology model.
 *
 * <p>Implementations typically inspect the provided OpenROADM {@link Network} model to locate
 * a node and termination point and extract:
 * <ul>
 *   <li>the resolved {@link OpenroadmTpType}</li>
 *   <li>the supporting node reference</li>
 *   <li>the effective {@code nodeId + tpId} identifiers</li>
 * </ul>
 */
public interface OpenRoadmTpTypeResolver {

    /**
     * Resolves a {@link TerminationPointId} for a termination point on a given OpenROADM node.
     *
     * <p>The provided {@link Network} is treated as the source of truth for the resolution.
     *
     * @param nodeId OpenROADM node id
     * @param tpId termination point id on that node
     * @param network OpenROADM topology snapshot to resolve from
     * @return a resolved {@link TerminationPointId}; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @throws TpTypeResolutionException if the node or termination point does not exist, or required
     *                                   augmentations / TP type are missing
     */
    TerminationPointId terminationPointId(String nodeId, String tpId, Network network);

}
