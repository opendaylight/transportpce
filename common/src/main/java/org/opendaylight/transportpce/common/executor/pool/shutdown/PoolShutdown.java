/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.pool.shutdown;

import java.util.List;
import org.opendaylight.transportpce.common.executor.pool.Pool;

/**
 * Shuts down a group of {@link Pool}s.
 */
public interface PoolShutdown {

    /**
     * Shuts down the pools.
     *
     * <p>New submissions are rejected. What happens to running and queued tasks is up to the implementation.
     * Returns once the pools have terminated, or when the implementation stops waiting for them.
     *
     * @param pools the pools to shut down
     */
    void shutdown(List<Pool> pools);
}
