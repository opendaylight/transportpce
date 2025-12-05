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
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TopologyNodeId must not be blank");
        }
    }

    public static TopologyNodeId fromNodeAndTpId(String nodeId, String tpId) {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(tpId, "tpId");

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
