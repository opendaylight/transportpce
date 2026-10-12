/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.config;

import java.time.Duration;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;

/**
 * Sizes of the executor pools, and the time allowed for shutting them down.
 */
public interface ExecutorPoolConfig {

    /**
     * Returns the maximum time to wait for the pools to terminate when they are shut down.
     *
     * @return the shutdown timeout
     */
    Duration shutDownTimeout();

    /**
     * Returns the sizes of the given pool: its own sizes if it has any, otherwise the default sizes.
     *
     * @param name the pool
     * @return the thread count and queue size of the pool
     */
    PoolSize poolSize(ThreadPoolName name);
}
