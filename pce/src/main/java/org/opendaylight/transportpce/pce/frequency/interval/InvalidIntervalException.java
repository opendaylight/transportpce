/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import org.opendaylight.transportpce.pce.frequency.FrequencySelectionException;

public class InvalidIntervalException extends FrequencySelectionException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public InvalidIntervalException(String message) {
        super(message);
    }
}
