/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.config;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link ExecutorPoolConfig}.
 *
 * <p>Each pool uses the default thread count and queue size unless it has sizes of its own. Invalid values (zero or
 * negative) are replaced with defaults, with a warning in the log, so a configuration mistake never prevents the
 * components that use the pools from starting.
 */
public class DefaultExecutorPoolConfig implements ExecutorPoolConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultExecutorPoolConfig.class);

    static final int DEFAULT_THREADS = 4;

    static final int DEFAULT_QUEUE_SIZE = 64;

    static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 20;

    private static final PoolSize DEFAULT_POOL_SIZE = new PoolSize(DEFAULT_THREADS, DEFAULT_QUEUE_SIZE);

    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);

    private final Duration shutDownTimeout;

    private final PoolSize defaultPoolSize;

    private final Map<ThreadPoolName, PoolSize> poolSizes;

    /**
     * Creates a configuration with the built-in defaults: {@value #DEFAULT_THREADS} threads and a queue size of
     * {@value #DEFAULT_QUEUE_SIZE} for every pool, and a shutdown timeout of {@value #DEFAULT_SHUTDOWN_TIMEOUT_SECONDS}
     * seconds.
     */
    public DefaultExecutorPoolConfig() {
        this(DEFAULT_SHUTDOWN_TIMEOUT, DEFAULT_POOL_SIZE, Map.of());
    }

    /**
     * Creates a configuration.
     *
     * @param shutDownTimeout maximum time to wait for the pools to terminate when they are shut down
     * @param defaultPoolSize sizes of the pools that have no sizes of their own
     * @param poolSizes sizes of individual pools; invalid values are replaced with those of {@code defaultPoolSize}
     */
    public DefaultExecutorPoolConfig(Duration shutDownTimeout, PoolSize defaultPoolSize,
            Map<ThreadPoolName, PoolSize> poolSizes) {
        if (shutDownTimeout == null || shutDownTimeout.isNegative() || shutDownTimeout.isZero()) {
            LOG.warn("Invalid shutdown timeout {}, using {}", shutDownTimeout, DEFAULT_SHUTDOWN_TIMEOUT);
            this.shutDownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
        } else {
            this.shutDownTimeout = shutDownTimeout;
        }

        this.defaultPoolSize = valid("", defaultPoolSize, DEFAULT_POOL_SIZE);

        Map<ThreadPoolName, PoolSize> sizes = new EnumMap<>(ThreadPoolName.class);
        poolSizes.forEach((name, size) ->
            sizes.put(name, valid(name.poolName() + ".", size, this.defaultPoolSize)));
        this.poolSizes = sizes;
    }

    private static PoolSize valid(String prefix, PoolSize size, PoolSize fallback) {
        if (size == null) {
            return fallback;
        }
        return new PoolSize(
            positiveOrDefault(prefix + "threads", size.threads(), fallback.threads()),
            positiveOrDefault(prefix + "queueSize", size.queueSize(), fallback.queueSize()));
    }

    private static int positiveOrDefault(String setting, int value, int defaultValue) {
        if (value > 0) {
            return value;
        }
        LOG.warn("Invalid {} {}, using {}", setting, value, defaultValue);
        return defaultValue;
    }

    @Override
    public Duration shutDownTimeout() {
        return shutDownTimeout;
    }

    @Override
    public PoolSize poolSize(ThreadPoolName name) {
        return poolSizes.getOrDefault(name, defaultPoolSize);
    }
}
