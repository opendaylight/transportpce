/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import java.io.Serial;

/**
 * Thrown when a request names a node that is not in the port mapping with a supported OpenROADM version, for
 * example a node that was removed.
 *
 * <p>No device is read in this case, so it is distinct from
 * {@link org.opendaylight.transportpce.common.device.DeviceReadException}.
 */
public class UnknownNodeException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnknownNodeException(String message) {
        super(message);
    }
}
