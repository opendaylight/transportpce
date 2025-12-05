/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Intended as a value object representing a node id in an OpenROADM topology.
 * A typical node id in this context is "ROADM-A-SRG1".
 */
public record TopologyNodeId(String value) {
    public TopologyNodeId {
        Objects.requireNonNull(value, "TopologyNodeId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TopologyNodeId must not be blank");
        }
    }

    /**
     * Creates a {@code TopologyNodeId} from an OpenROADM node ID and a
     * termination point (TP) ID.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * nodeId = "ROADM-A2"
     * tpId   = "SRG1-PP2-TXRX"
     *
     * result = "ROADM-A2-SRG1"
     * }</pre>
     *
     * @param nodeId the OpenROADM node identifier (must not be {@code null})
     * @param tpId the termination point identifier (must not be {@code null}
     *             and must contain a non-empty prefix before {@code '-'})
     * @return a new {@code TopologyNodeId}
     * @throws NullPointerException if {@code nodeId} or {@code tpId} is {@code null}
     * @throws IllegalArgumentException if {@code tpId} does not contain a
     *         non-empty prefix before {@code '-'}
     */
    public static TopologyNodeId fromNodeAndTpId(String nodeId, String tpId) {
        Objects.requireNonNull(nodeId, "'nodeId' must not be null");
        Objects.requireNonNull(tpId, "'tpId' must not be null");

        String tpPrefix = tpPrefix(tpId); // domain rule: part before first '-'
        return new TopologyNodeId("%s-%s".formatted(nodeId, tpPrefix));
    }

    static String tpPrefix(String tpId) {
        int dash = tpId.indexOf('-');
        if (dash <= 0) { // dash==0 => empty prefix, dash==-1 => no dash
            throw new IllegalArgumentException("tpId must contain a non-empty prefix before '-': " + tpId);
        }
        return tpId.substring(0, dash);
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
