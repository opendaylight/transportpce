/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common;

public final class NetworkUtils {

    private NetworkUtils() {
    }

    public static final String CLLI_NETWORK_ID = "clli-network";

    public static final String UNDERLAY_NETWORK_ID = "openroadm-network";

    public static final String OVERLAY_NETWORK_ID = "openroadm-topology";

    public enum Operation {
        CREATE,
        DELETE
    }

}
