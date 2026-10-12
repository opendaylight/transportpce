/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link ExecutorPoolConfig}.
 *
 * <p>Each pool uses the default thread count and queue size unless it has sizes of its own. Invalid values (not
 * numbers, zero or negative) are replaced with defaults, with a warning in the log, so a configuration mistake never
 * prevents the components that use the pools from starting.
 *
 * <p>In OSGi, the configuration is read from {@code org.opendaylight.transportpce.executorpool.cfg}. Besides the
 * settings of {@link Configuration}, the file can set the sizes of individual pools with the keys
 * {@code <pool>.threads} and {@code <pool>.queueSize}, where {@code <pool>} is a {@link ThreadPoolName#poolName()}.
 * A pool that sets only one of them uses the default for the other.
 *
 * <p>Changing the configuration restarts this component, and with it the components that use it. They shut down
 * their executors, which cancels the requests they are processing, and start again with new executors.
 */
@Designate(ocd = DefaultExecutorPoolConfig.Configuration.class)
@Component(service = ExecutorPoolConfig.class, configurationPid = "org.opendaylight.transportpce.executorpool")
public class DefaultExecutorPoolConfig implements ExecutorPoolConfig {

    /**
     * Default settings for all pools, read from {@code org.opendaylight.transportpce.executorpool.cfg}.
     *
     * <p>This describes the settings to the metatype service. The component reads them from the raw properties
     * instead, since the typed getters fail on values that are not numbers, before they can be replaced with
     * defaults.
     */
    @ObjectClassDefinition(
        name = "TransportPCE executor pools",
        description = "Thread pools used to run blocking work, such as device operations, off the RESTCONF request "
            + "threads. Changing these settings restarts the components that use the pools.")
    public @interface Configuration {
        @AttributeDefinition(description = "Number of threads in each pool without a <pool>.threads setting.",
            min = "1")
        int threads() default DEFAULT_THREADS;

        @AttributeDefinition(description = "Maximum number of tasks waiting for a free thread in each pool without a "
            + "<pool>.queueSize setting. When the queue is full, new requests are rejected.", min = "1")
        int queueSize() default DEFAULT_QUEUE_SIZE;

        @AttributeDefinition(description = "Maximum time in seconds to wait for a component's pools to terminate "
            + "when it stops. Pools still running after this time are left to finish on their own.", min = "1")
        int shutDownTimeoutSeconds() default DEFAULT_SHUTDOWN_TIMEOUT_SECONDS;
    }

    private static final String THREADS = "threads";

    private static final String QUEUE_SIZE = "queueSize";

    private static final String SHUTDOWN_TIMEOUT_SECONDS = "shutDownTimeoutSeconds";

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
     * Creates the configuration from the OSGi configuration service.
     *
     * @param properties all configuration properties: the settings of {@link Configuration} and the sizes of
     *     individual pools
     */
    @Activate
    public DefaultExecutorPoolConfig(Map<String, ?> properties) {
        this(properties, new PoolSize(
            intValue(THREADS, properties.get(THREADS), DEFAULT_THREADS),
            intValue(QUEUE_SIZE, properties.get(QUEUE_SIZE), DEFAULT_QUEUE_SIZE)));
    }

    private DefaultExecutorPoolConfig(Map<String, ?> properties, PoolSize defaultPoolSize) {
        this(
            Duration.ofSeconds(intValue(SHUTDOWN_TIMEOUT_SECONDS, properties.get(SHUTDOWN_TIMEOUT_SECONDS),
                DEFAULT_SHUTDOWN_TIMEOUT_SECONDS)),
            defaultPoolSize,
            poolSizes(properties, defaultPoolSize));
    }

    /**
     * Creates a configuration with the built-in defaults: {@value #DEFAULT_THREADS} threads and a queue size of
     * {@value #DEFAULT_QUEUE_SIZE} for every pool, and a shutdown timeout of {@value #DEFAULT_SHUTDOWN_TIMEOUT_SECONDS}
     * seconds.
     *
     * <p>For use outside OSGi, such as in lighty and in tests. The configuration file and its environment variables
     * are only read by the OSGi configuration service, so they have no effect on a configuration created this way.
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

    /**
     * Reads the sizes of individual pools from the configuration properties.
     *
     * <p>A pool that sets only one of its sizes uses the default for the other. Values that are not numbers are
     * replaced with the default, with a warning in the log.
     */
    private static Map<ThreadPoolName, PoolSize> poolSizes(Map<String, ?> properties, PoolSize defaultPoolSize) {
        Map<ThreadPoolName, PoolSize> sizes = new EnumMap<>(ThreadPoolName.class);
        for (ThreadPoolName name : ThreadPoolName.values()) {
            String prefix = name.poolName() + ".";
            Object threads = properties.get(prefix + THREADS);
            Object queueSize = properties.get(prefix + QUEUE_SIZE);
            if (threads != null || queueSize != null) {
                sizes.put(name, new PoolSize(
                    intValue(prefix + THREADS, threads, defaultPoolSize.threads()),
                    intValue(prefix + QUEUE_SIZE, queueSize, defaultPoolSize.queueSize())));
            }
        }
        // A misspelled pool name would otherwise be silently ignored
        for (String key : properties.keySet()) {
            String pool = poolOf(key);
            if (pool != null && Arrays.stream(ThreadPoolName.values()).noneMatch(n -> n.poolName().equals(pool))) {
                LOG.warn("Ignoring {}: there is no pool named {}. Known pools are {}", key, pool,
                    Arrays.toString(ThreadPoolName.values()));
            }
        }
        return sizes;
    }

    /**
     * Returns the pool that a per-pool key such as {@code olm-get-pm.threads} refers to, or null for other keys.
     */
    private static String poolOf(String key) {
        for (String setting : new String[] {THREADS, QUEUE_SIZE}) {
            if (key.endsWith("." + setting)) {
                return key.substring(0, key.length() - setting.length() - 1);
            }
        }
        return null;
    }

    private static int intValue(String setting, Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            LOG.warn("Invalid {} {}, using {}", setting, value, defaultValue);
            return defaultValue;
        }
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
