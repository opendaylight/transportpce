/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import java.io.Serial;

/**
 * Thrown when data could not be read from a device: the device is not mounted, it could not be reached in time,
 * or the read itself failed.
 *
 * <p>This is distinct from the device having no data at the requested path, which
 * {@link DeviceTransactionManager#readFromDevice} reports as an empty {@link java.util.Optional}.
 */
public class DeviceReadException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceReadException(String message) {
        super(message);
    }

    public DeviceReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
