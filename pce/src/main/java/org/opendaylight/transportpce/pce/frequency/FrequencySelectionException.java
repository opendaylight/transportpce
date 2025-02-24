/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency;

public class FrequencySelectionException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public FrequencySelectionException(String message) {
        super(message);
    }
}
