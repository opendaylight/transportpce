/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.Objects;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;

/**
 * Value object representing an OpenROADM termination point and its associated node identity.
 *
 * <p>This record is typically used as an intermediate representation when translating
 * OpenROADM topology elements into their corresponding TAPI model objects.
 *
 * <p>The {@code supportingNodeId} represents the parent OpenROADM node (e.g. the ROADM shelf),
 * while {@code nodeId} refers to a specific OpenROADM topology node instance (e.g. a degree or SRG node).
 *
 * @param supportingNodeId
 *     the supporting OpenROADM node identifier (e.g. {@code ROADM-A})
 * @param nodeId
 *     the OpenROADM topology node identifier (e.g. {@code ROADM-A-SRG1})
 * @param tpId
 *     the termination point identifier on the node (e.g. {@code SRG1-PP1-TXRX})
 * @param openRoadmTpType
 *     the OpenROADM termination point type (e.g. {@code SRGTXRXPP})
 */
public record TerminationPointId(
        String supportingNodeId,
        String nodeId,
        String tpId,
        OpenroadmTpType openRoadmTpType) {

    public TerminationPointId {
        Objects.requireNonNull(supportingNodeId, "supportingNodeId");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(tpId, "tpId");
        Objects.requireNonNull(openRoadmTpType, "openRoadmTpType");

        if (supportingNodeId.isBlank()) {
            throw new IllegalArgumentException("supportingNodeId must not be blank");
        }
        if (nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        if (tpId.isBlank()) {
            throw new IllegalArgumentException("tpId must not be blank");
        }
    }
}
