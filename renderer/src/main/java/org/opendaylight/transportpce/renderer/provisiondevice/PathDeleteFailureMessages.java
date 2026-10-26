/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.slf4j.Logger;

/**
 * Describes and logs why a service path delete failed on a node, for the service-path and otn-service-path deletes
 * to report these failures the same way.
 */
final class PathDeleteFailureMessages {

    private final Logger log;

    /**
     * Creates the messages for a delete implementation.
     *
     * @param log the caller's logger, so failures are logged under the class that hit them
     */
    PathDeleteFailureMessages(Logger log) {
        this.log = requireNonNull(log);
    }

    /**
     * Logs why the delete failed on the node, and returns the reason to report to the caller.
     *
     * @param nodeId the node
     * @param cause a {@link DeviceReadException} if the node could not be read to find what to delete, otherwise
     *     why the delete failed
     * @return the reason, as logged
     */
    @SuppressFBWarnings(
            value = "SLF4J_FORMAT_SHOULD_BE_CONST",
            justification = "The message is formatted once, for the log and for the result returned to the caller")
    String logNodeFailure(String nodeId, Exception cause) {
        String result = cause instanceof DeviceReadException
            ? "Could not read node %s to find what to delete: %s".formatted(nodeId, cause.getMessage())
            : "Failed to delete service path on node %s: %s".formatted(nodeId, cause.getMessage());
        log.error(result, cause);
        return result;
    }
}
