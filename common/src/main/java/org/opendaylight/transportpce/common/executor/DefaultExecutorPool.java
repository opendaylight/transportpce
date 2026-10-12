/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultExecutorPool implements Pool {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultExecutorPool.class);

    private final String name;

    private final ListeningExecutorService executorService;

    public DefaultExecutorPool(String name, ListeningExecutorService executorService) {
        Objects.requireNonNull(executorService, "executor service cannot be null");

        String tmpName = name;
        if (name == null || name.isEmpty()) {
            tmpName = "executor-pool";
        }

        this.name = tmpName;
        this.executorService = executorService;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Future<?>> stop() {
        LOG.info("Shutting down executor {}", name);
        List<Future<?>> queued = new ArrayList<>();
        for (Runnable task : executorService.shutdownNow()) {
            if (task instanceof Future<?> future) {
                queued.add(future);
            }
        }
        return queued;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        boolean terminated = executorService.awaitTermination(timeout, unit);
        if (!terminated) {
            LOG.warn("Executor {} did not terminate in time", name);
        }
        return terminated;
    }

    @Override
    public ListeningExecutorService executorService() {
        return executorService;
    }
}
