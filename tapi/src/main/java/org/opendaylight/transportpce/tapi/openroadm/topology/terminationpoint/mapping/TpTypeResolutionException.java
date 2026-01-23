/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

public class TpTypeResolutionException extends RuntimeException {
    public enum Reason {
        NODE_NOT_FOUND,
        SUPPORTING_NODE_NOT_FOUND,
        NODE_AUGMENTATION_MISSING,
        TP_NOT_FOUND,
        TP_AUGMENTATION_MISSING,
        TP_TYPE_MISSING
    }

    private final Reason reason;
    private final String nodeId;
    private final String tpId;

    public TpTypeResolutionException(Reason reason, String nodeId, String tpId, String message) {
        super(message);
        this.reason = reason;
        this.nodeId = nodeId;
        this.tpId = tpId;
    }

    public Reason reason() {
        return reason;
    }

    public String nodeId() {
        return nodeId;
    }

    public String tpId() {
        return tpId;
    }
}
