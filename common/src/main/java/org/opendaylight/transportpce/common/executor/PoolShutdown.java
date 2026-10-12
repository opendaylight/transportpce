/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import java.util.List;

/**
 * Shuts down a group of {@link Pool}s.
 */
public interface PoolShutdown {

    /**
     * Shuts down the pools.
     *
     * <p>Running tasks are interrupted and queued tasks are cancelled, so callers waiting on their futures are not
     * left hanging. Returns once the pools have terminated, or when the implementation stops waiting for them.
     *
     * @param pools the pools to shut down
     */
    void shutdown(List<Pool> pools);
}
