/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

/**
 * Class to create exception to manage missing data for GNPy.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

@SuppressWarnings("serial")
public class GnpyException extends Exception {

    public GnpyException(String message) {
        super(message);
    }

    public GnpyException(String message, Throwable cause) {
        super(message, cause);
    }
}
