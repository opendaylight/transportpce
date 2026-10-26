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
 * Names a queue of operations that run one at a time, such as the service handler's service creations, and that wait
 * their turn in it.
 *
 * <p>Unlike {@link ThreadPoolName}, this names no thread pool: there are no {@code threads}, only a {@code queueSize},
 * the number of operations that may wait for their turn.
 */
public enum OperationQueueName {
    // A creation takes minutes, and its PCE request must not see the spectrum of the creations before it as free
    SERVICE_CREATE("service-create", 8);

    private final String name;

    private final int defaultQueueSize;

    OperationQueueName(String name, int defaultQueueSize) {
        this.name = name;
        this.defaultQueueSize = defaultQueueSize;
    }

    public String queueName() {
        return name;
    }

    /**
     * Returns the number of operations that may wait for their turn when the queue has no {@code <queue>.queueSize}
     * setting.
     */
    public int defaultQueueSize() {
        return defaultQueueSize;
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean isValid(String queueName) {
        return Arrays.stream(OperationQueueName.values()).anyMatch(n -> n.queueName().equals(queueName));
    }
}
