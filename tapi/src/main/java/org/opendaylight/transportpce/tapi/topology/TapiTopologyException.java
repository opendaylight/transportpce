/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;


@SuppressWarnings("serial")
public class TapiTopologyException extends Exception {

    public TapiTopologyException(String message) {
        super(message);
    }

    public TapiTopologyException(String message, Throwable cause) {
        super(message, cause);
    }
}
