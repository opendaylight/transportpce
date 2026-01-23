/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

/**
 * Signals a failure while accessing or updating the datastore-backed
 * {@link org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint}.
 *
 * <p>This is an unchecked exception to avoid forcing callers to handle low-level
 * transaction failures, while still providing a precise exception type for
 * error handling and testing.
 */
public class OwnedNodeEdgePointRepositoryException extends RuntimeException {

    public OwnedNodeEdgePointRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public OwnedNodeEdgePointRepositoryException(String message) {
        super(message);
    }
}
