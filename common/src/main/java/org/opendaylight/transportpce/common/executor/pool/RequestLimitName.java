/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.pool;

import java.util.Arrays;

/**
 * Names an RPC whose number of requests in progress is limited, as counted by
 * {@code RequestLimit} in the renderer module.
 *
 * <p>Unlike {@link ThreadPoolName}, this names a plain in-progress counter, not a thread pool: there is no
 * {@code queueSize}, only a single {@code maxRequests}.
 */
public enum RequestLimitName {
    SERVICE_IMPLEMENTATION_REQUEST("service-implementation-request"),
    SERVICE_DELETE("service-delete");

    private final String name;

    RequestLimitName(String name) {
        this.name = name;
    }

    public String limitName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean isValid(String limitName) {
        return Arrays.stream(RequestLimitName.values()).anyMatch(n -> n.limitName().equals(limitName));
    }
}
