/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.pool;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * Identifies an executor pool with a configurable thread count and queue size.
 *
 * <p>The value returned by {@link #poolName()} is the prefix for the pool's configuration properties,
 * {@code <pool>.threads} and {@code <pool>.queueSize}, for example {@code olm-get-pm.threads}.
 * Each property overrides the corresponding shared default independently; omitted properties use that default.
 * A pool can have a built-in queue size of its own, which replaces the shared default for that pool.
 */
public enum ThreadPoolName {
    OLM_GET_PM("olm-get-pm"),
    OLM_SPANLOSS("olm-spanloss"),
    SERVICE_PATH("service-path"),
    OTN_SERVICE_PATH("otn-service-path"),
    // Services wait here for minutes each while devices are configured, and the service handler can hand over many
    // at once, so the shared default is too short for it
    RENDERER("renderer", 1024),
    // Bounds the service handler's deletions in progress, as well as their queue: each queues at most one step
    SERVICE_DELETION("service-deletion", 512),
    // As for service-deletion, for its creations and feasibility checks
    SERVICE_CREATION("service-creation", 512);

    private final String name;

    private final OptionalInt defaultQueueSize;

    ThreadPoolName(String name) {
        this.name = name;
        this.defaultQueueSize = OptionalInt.empty();
    }

    ThreadPoolName(String name, int defaultQueueSize) {
        this.name = name;
        this.defaultQueueSize = OptionalInt.of(defaultQueueSize);
    }

    public String poolName() {
        return name;
    }

    /**
     * Returns the queue size of this pool when it has no {@code <pool>.queueSize} setting: its built-in queue size if
     * it has one, otherwise {@code sharedDefault}.
     *
     * @param sharedDefault the queue size of the pools without a setting or built-in queue size of their own
     */
    public int defaultQueueSize(int sharedDefault) {
        return defaultQueueSize.orElse(sharedDefault);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Determines whether {@code poolName} is a valid name as defined by this Enum.
     */
    public static boolean isValid(String poolName) {
        return Arrays.stream(ThreadPoolName.values()).anyMatch(n -> n.poolName().equals(poolName));
    }
}
