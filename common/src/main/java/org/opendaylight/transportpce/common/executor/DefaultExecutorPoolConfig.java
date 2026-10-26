/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.transportpce.common.executor.pool.OperationQueueName;
import org.opendaylight.transportpce.common.executor.pool.RequestLimitName;
import org.opendaylight.transportpce.common.executor.pool.ThreadPoolName;
import org.opendaylight.transportpce.common.executor.pool.config.PoolSize;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link ExecutorPoolConfig}.
 *
 * <p>Each pool uses the default thread count and queue size unless it has sizes of its own. A pool with a built-in
 * queue size of its own, see {@link ThreadPoolName#defaultQueueSize(int)}, uses that instead of the default queue
 * size. Invalid values (not numbers, zero or negative) are replaced with defaults, with a warning in the log, so a
 * configuration mistake never prevents the components that use the pools from starting.
 *
 * <p>In OSGi, the configuration is read from {@code org.opendaylight.transportpce.executorpool.cfg}. Besides the
 * settings of {@link Configuration}, the file can set the sizes of individual pools with the keys
 * {@code <pool>.threads} and {@code <pool>.queueSize}, where {@code <pool>} is a {@link ThreadPoolName#poolName()}.
 *
 * <p>A pool that sets only one of them uses the default for the other. It can also set the maximum number of requests
 * in progress for an RPC limited by {@code RequestLimit} with the key {@code <rpc>.maxRequests}, where {@code <rpc>}
 * is a {@link RequestLimitName#limitName()}; unlike the pools, there is no shared default for this, since a single
 * number doesn't split into a "threads" and a "queueSize" to default separately - an RPC without its own setting
 * simply keeps the built-in default of {@value #DEFAULT_MAX_REQUESTS}. A queue of operations that run one at a time
 * sets how many operations may wait for their turn with the key {@code <queue>.queueSize}, where {@code <queue>} is an
 * {@link OperationQueueName#queueName()}; a queue without its own setting keeps its built-in default, see
 * {@link OperationQueueName#defaultQueueSize()}, rather than the default queue size of the pools.
 *
 * <p>Changes to the configuration take effect when TransportPCE is restarted. Applying them at once would restart
 * the components that use the pools, interrupting the service operations they are running, which can leave devices
 * partly configured. Instead, this component logs the change and keeps its settings, see {@link #modified(Map)}.
 *
 * <p>The component starts only once the configuration exists, and stops if it is deleted, together with the
 * components that use it.
 */
@Designate(ocd = DefaultExecutorPoolConfig.Configuration.class)
// REQUIRE, because the configuration can arrive after the bundle has started: the odl-transportpce feature's
// <configfile> only copies the file to etc/, and FileInstall hands it to ConfigAdmin on its next poll of etc/. On a
// fresh install the feature's bundles are already starting by then. With the default OPTIONAL policy this component
// would be activated with the built-in defaults, and since modified() does not apply changes, the file would have
// no effect until TransportPCE is restarted. REQUIRE makes DS wait for the configuration instead.
@Component(
    service = ExecutorPoolConfig.class,
    configurationPid = "org.opendaylight.transportpce.executorpool",
    configurationPolicy = ConfigurationPolicy.REQUIRE)
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
            + "threads. Changes take effect when TransportPCE is restarted.")
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

    private static final String MAX_REQUESTS = "maxRequests";

    private static final String SHUTDOWN_TIMEOUT_SECONDS = "shutDownTimeoutSeconds";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultExecutorPoolConfig.class);

    static final int DEFAULT_THREADS = 4;

    static final int DEFAULT_QUEUE_SIZE = 64;

    static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 20;

    static final int DEFAULT_MAX_REQUESTS = 16;

    private static final PoolSize DEFAULT_POOL_SIZE = new PoolSize(DEFAULT_THREADS, DEFAULT_QUEUE_SIZE);

    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);

    private final Duration shutDownTimeout;

    private final PoolSize defaultPoolSize;

    private final Map<ThreadPoolName, PoolSize> poolSizes;

    private final Map<RequestLimitName, Integer> maxRequests;

    private final Map<OperationQueueName, Integer> operationQueueSizes;

    /**
     * Creates the configuration from the OSGi configuration service.
     *
     * @param properties all configuration properties: the settings of {@link Configuration}, the sizes of
     *     individual pools, and the request limits of individual RPCs
     */
    @Activate
    public DefaultExecutorPoolConfig(Map<String, ?> properties) {
        this(
            properties,
            new PoolSize(
                intValue(THREADS, properties.get(THREADS), DEFAULT_THREADS),
                intValue(QUEUE_SIZE, properties.get(QUEUE_SIZE), DEFAULT_QUEUE_SIZE)));
    }

    private DefaultExecutorPoolConfig(Map<String, ?> properties, PoolSize defaultPoolSize) {
        this(
            Duration.ofSeconds(
                intValue(
                    SHUTDOWN_TIMEOUT_SECONDS,
                    properties.get(SHUTDOWN_TIMEOUT_SECONDS),
                    DEFAULT_SHUTDOWN_TIMEOUT_SECONDS)),
            defaultPoolSize,
            poolSizes(properties, defaultPoolSize),
            maxRequestsOf(properties),
            operationQueueSizesOf(properties));
    }

    /**
     * Creates a configuration with the built-in defaults: {@value #DEFAULT_THREADS} threads and a queue size of
     * {@value #DEFAULT_QUEUE_SIZE} for every pool without a built-in queue size of its own, a shutdown timeout of
     * {@value #DEFAULT_SHUTDOWN_TIMEOUT_SECONDS} seconds, a request limit of {@value #DEFAULT_MAX_REQUESTS} for
     * every limited RPC, and the built-in queue size of every operation queue.
     *
     * <p>For use outside OSGi, such as in lighty and in tests. The configuration file and its environment variables
     * are only read by the OSGi configuration service, so they have no effect on a configuration created this way.
     */
    public DefaultExecutorPoolConfig() {
        this(DEFAULT_SHUTDOWN_TIMEOUT, DEFAULT_POOL_SIZE, Map.of());
    }

    /**
     * Creates a configuration with every RPC's request limit left at the built-in default.
     *
     * @param shutDownTimeout maximum time to wait for the pools to terminate when they are shut down
     * @param defaultPoolSize sizes of the pools that have no sizes of their own
     * @param poolSizes sizes of individual pools; invalid values are replaced with those of {@code defaultPoolSize},
     *     or with the pool's built-in queue size
     */
    public DefaultExecutorPoolConfig(
            Duration shutDownTimeout,
            PoolSize defaultPoolSize,
            Map<ThreadPoolName, PoolSize> poolSizes) {

        this(shutDownTimeout, defaultPoolSize, poolSizes, Map.of());
    }

    /**
     * Creates a configuration with every operation queue left at its built-in queue size.
     *
     * @param shutDownTimeout maximum time to wait for the pools to terminate when they are shut down
     * @param defaultPoolSize sizes of the pools that have no sizes of their own
     * @param poolSizes sizes of individual pools; invalid values are replaced with those of {@code defaultPoolSize},
     *     or with the pool's built-in queue size
     * @param maxRequests request limits of individual RPCs; invalid values are replaced with
     *     {@value #DEFAULT_MAX_REQUESTS}
     */
    public DefaultExecutorPoolConfig(
            Duration shutDownTimeout,
            PoolSize defaultPoolSize,
            Map<ThreadPoolName, PoolSize> poolSizes,
            Map<RequestLimitName, Integer> maxRequests) {

        this(shutDownTimeout, defaultPoolSize, poolSizes, maxRequests, Map.of());
    }

    /**
     * Creates a configuration.
     *
     * @param shutDownTimeout maximum time to wait for the pools to terminate when they are shut down
     * @param defaultPoolSize sizes of the pools that have no sizes of their own
     * @param poolSizes sizes of individual pools; invalid values are replaced with those of {@code defaultPoolSize},
     *     or with the pool's built-in queue size
     * @param maxRequests request limits of individual RPCs; invalid values are replaced with
     *     {@value #DEFAULT_MAX_REQUESTS}
     * @param operationQueueSizes queue sizes of individual operation queues; invalid values are replaced with the
     *     queue's built-in queue size
     */
    public DefaultExecutorPoolConfig(
            Duration shutDownTimeout,
            PoolSize defaultPoolSize,
            Map<ThreadPoolName, PoolSize> poolSizes,
            Map<RequestLimitName, Integer> maxRequests,
            Map<OperationQueueName, Integer> operationQueueSizes) {

        if (shutDownTimeout == null || shutDownTimeout.isNegative() || shutDownTimeout.isZero()) {
            LOG.warn("Invalid shutdown timeout {}, using {}", shutDownTimeout, DEFAULT_SHUTDOWN_TIMEOUT);
            this.shutDownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
        } else {
            this.shutDownTimeout = shutDownTimeout;
        }

        this.defaultPoolSize = valid("", defaultPoolSize, DEFAULT_POOL_SIZE);

        Map<ThreadPoolName, PoolSize> sizes = new EnumMap<>(ThreadPoolName.class);
        for (Map.Entry<ThreadPoolName, PoolSize> entry : poolSizes.entrySet()) {
            ThreadPoolName name = entry.getKey();
            sizes.put(name, valid(name.poolName() + ".", entry.getValue(), defaultPoolSize(name)));
        }
        this.poolSizes = sizes;

        Map<RequestLimitName, Integer> limits = new EnumMap<>(RequestLimitName.class);
        for (Map.Entry<RequestLimitName, Integer> entry : maxRequests.entrySet()) {
            RequestLimitName name = entry.getKey();
            Integer configuredMaxRequests = entry.getValue();
            int value = configuredMaxRequests == null ? DEFAULT_MAX_REQUESTS : configuredMaxRequests;
            limits.put(name, positiveOrDefault(name.limitName() + "." + MAX_REQUESTS, value, DEFAULT_MAX_REQUESTS));
        }
        this.maxRequests = limits;

        Map<OperationQueueName, Integer> queueSizes = new EnumMap<>(OperationQueueName.class);
        for (Map.Entry<OperationQueueName, Integer> entry : operationQueueSizes.entrySet()) {
            OperationQueueName name = entry.getKey();
            Integer configuredQueueSize = entry.getValue();
            int value = configuredQueueSize == null ? name.defaultQueueSize() : configuredQueueSize;
            queueSizes.put(name,
                positiveOrDefault(name.queueName() + "." + QUEUE_SIZE, value, name.defaultQueueSize()));
        }
        this.operationQueueSizes = queueSizes;
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
            String prefix = name.poolName();
            Optional<Integer> threads = overrideValue(prefix, THREADS, properties, defaultPoolSize.threads());
            Optional<Integer> queueSize = overrideValue(
                prefix, QUEUE_SIZE, properties, name.defaultQueueSize(defaultPoolSize.queueSize()));
            if (threads.isPresent() || queueSize.isPresent()) {
                sizes.put(
                    name,
                    new PoolSize(
                        threads.orElseGet(defaultPoolSize::threads),
                        queueSize.orElseGet(() -> name.defaultQueueSize(defaultPoolSize.queueSize()))));
            }
        }
        // A misspelled pool name would otherwise be silently ignored
        for (String key : properties.keySet()) {
            poolOf(key).ifPresent(pool -> {
                // An operation queue has a queueSize, but no threads
                if (!ThreadPoolName.isValid(pool)
                        && !(key.endsWith("." + QUEUE_SIZE) && OperationQueueName.isValid(pool))) {
                    LOG.warn("Ignoring {}: there is no pool named {}. Known pools are {}", key, pool,
                        Arrays.toString(ThreadPoolName.values()));
                }
            });
        }
        return sizes;
    }

    /**
     * Reads the request limits of individual RPCs from the configuration properties.
     *
     * <p>An RPC without its own {@code <rpc>.maxRequests} setting keeps the built-in default
     * ({@value #DEFAULT_MAX_REQUESTS}). Values that are not numbers are replaced with the default, with a warning
     * in the log.
     */
    private static Map<RequestLimitName, Integer> maxRequestsOf(Map<String, ?> properties) {
        Map<RequestLimitName, Integer> limits = new EnumMap<>(RequestLimitName.class);
        for (RequestLimitName name : RequestLimitName.values()) {
            overrideValue(name.limitName(), MAX_REQUESTS, properties, DEFAULT_MAX_REQUESTS)
                .ifPresent(value -> limits.put(name, value));
        }
        // A misspelled RPC name would otherwise be silently ignored
        for (String key : properties.keySet()) {
            limitOf(key).ifPresent(limit -> {
                if (!RequestLimitName.isValid(limit)) {
                    LOG.warn("Ignoring {}: there is no request limit named {}. Known limits are {}", key, limit,
                        Arrays.toString(RequestLimitName.values()));
                }
            });
        }
        return limits;
    }

    /**
     * Reads the queue sizes of individual operation queues from the configuration properties.
     *
     * <p>A queue without its own {@code <queue>.queueSize} setting keeps its built-in queue size. Values that are not
     * numbers are replaced with it, with a warning in the log. Unknown names are reported by
     * {@link #poolSizes(Map, PoolSize)}, as the keys look like those of the pools.
     */
    private static Map<OperationQueueName, Integer> operationQueueSizesOf(Map<String, ?> properties) {
        Map<OperationQueueName, Integer> queueSizes = new EnumMap<>(OperationQueueName.class);
        for (OperationQueueName name : OperationQueueName.values()) {
            overrideValue(name.queueName(), QUEUE_SIZE, properties, name.defaultQueueSize())
                .ifPresent(value -> queueSizes.put(name, value));
        }
        return queueSizes;
    }

    /**
     * Reads one overridable setting of a pool, RPC, or operation queue, keyed as {@code <name>.<suffix>}, parsing it
     * like {@link #intValue(String, Object, int)} when it is set.
     *
     * @param name the pool, RPC, or operation queue name, such as a {@link ThreadPoolName#poolName()}
     * @param suffix the setting, such as {@value #THREADS} or {@value #QUEUE_SIZE}
     * @param properties the configuration properties
     * @param defaultValue the value to fall back to if the setting is present but not a valid number
     * @return the setting's value, or empty if it is not set
     */
    private static Optional<Integer> overrideValue(
            String name, String suffix, Map<String, ?> properties, int defaultValue) {
        String key = name + "." + suffix;
        Object value = properties.get(key);
        return value == null ? Optional.empty() : Optional.of(intValue(key, value, defaultValue));
    }

    /**
     * Returns the pool that a per-pool key such as {@code olm-get-pm.threads} refers to ({@code olm-get-pm}),
     * or an empty {@link Optional} for other keys.
     */
    private static Optional<String> poolOf(String key) {
        for (String setting : new String[] {THREADS, QUEUE_SIZE}) {
            if (key.endsWith("." + setting)) {
                return Optional.of(key.substring(0, key.length() - setting.length() - 1));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the RPC that a per-RPC key such as {@code service-delete.maxRequests} refers to ({@code service-delete}),
     * or an empty {@link Optional} for other keys.
     */
    private static Optional<String> limitOf(String key) {
        if (key.endsWith("." + MAX_REQUESTS)) {
            return Optional.of(key.substring(0, key.length() - MAX_REQUESTS.length() - 1));
        }
        return Optional.empty();
    }

    private static int intValue(String setting, Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
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
            positiveOrDefault(prefix + THREADS, size.threads(), fallback.threads()),
            positiveOrDefault(prefix + QUEUE_SIZE, size.queueSize(), fallback.queueSize()));
    }

    private static int positiveOrDefault(String setting, int value, int defaultValue) {
        if (value > 0) {
            return value;
        }
        LOG.warn("Invalid {} {}, using {}", setting, value, defaultValue);
        return defaultValue;
    }

    /**
     * Logs a change to the configuration and keeps the current settings.
     *
     * <p>Having this method makes DS pass changes to it rather than restart this component, and with it the components
     * that use the pools. Their pools are sized when they start, so applying a change would mean restarting them,
     * which interrupts the service operations they are running and can leave devices partly configured. The new
     * settings are read when TransportPCE is restarted.
     *
     * @param properties the changed configuration properties, not applied
     */
    @Modified
    void modified(Map<String, ?> properties) {
        LOG.warn("The executor pool configuration has changed. The new settings take effect when TransportPCE is "
            + "restarted; until then the current settings are kept");
    }

    @Override
    public Duration shutDownTimeout() {
        return shutDownTimeout;
    }

    @Override
    public PoolSize poolSize(ThreadPoolName name) {
        return poolSizes.getOrDefault(name, defaultPoolSize(name));
    }

    /**
     * Returns the sizes of {@code name} when it has no sizes of its own: the default thread count, and its built-in
     * queue size if it has one, otherwise the default queue size.
     */
    private PoolSize defaultPoolSize(ThreadPoolName name) {
        return new PoolSize(defaultPoolSize.threads(), name.defaultQueueSize(defaultPoolSize.queueSize()));
    }

    @Override
    public int maxRequests(RequestLimitName name) {
        return maxRequests.getOrDefault(name, DEFAULT_MAX_REQUESTS);
    }

    @Override
    public int queueSize(OperationQueueName name) {
        return operationQueueSizes.getOrDefault(name, name.defaultQueueSize());
    }
}
