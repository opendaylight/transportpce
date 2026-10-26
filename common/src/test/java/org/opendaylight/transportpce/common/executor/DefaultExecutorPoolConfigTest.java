/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.executor.pool.OperationQueueName;
import org.opendaylight.transportpce.common.executor.pool.RequestLimitName;
import org.opendaylight.transportpce.common.executor.pool.ThreadPoolName;
import org.opendaylight.transportpce.common.executor.pool.config.PoolSize;

class DefaultExecutorPoolConfigTest {

    private static final ThreadPoolName POOL = ThreadPoolName.OLM_GET_PM;

    private static final RequestLimitName LIMIT = RequestLimitName.SERVICE_DELETE;

    private static final OperationQueueName QUEUE = OperationQueueName.SERVICE_CREATE;

    @Test
    void testBuiltInDefaults() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig();

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
        assertEquals(16, config.maxRequests(LIMIT));
        assertEquals(8, config.queueSize(QUEUE));
    }

    @Test
    void testPoolWithoutSizesOfItsOwnUsesTheDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8), Map.of());

        assertEquals(new PoolSize(2, 8), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(5), config.shutDownTimeout());
    }

    @Test
    void testPoolSizesOverrideTheDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8),
            Map.of(POOL, new PoolSize(6, 16)));

        assertEquals(new PoolSize(6, 16), config.poolSize(POOL));
    }

    @Test
    void testInvalidDefaultsFallBackToTheBuiltInDefaults() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(null, new PoolSize(0, -1), Map.of());

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
    }

    @Test
    void testInvalidPoolSizesFallBackToTheDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8),
            Map.of(POOL, new PoolSize(0, 16)));

        // Only the invalid value is replaced
        assertEquals(new PoolSize(2, 16), config.poolSize(POOL));
    }

    @Test
    void testBuiltInQueueSizeReplacesTheDefaultQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8), Map.of());

        assertEquals(new PoolSize(4, 1024), new DefaultExecutorPoolConfig().poolSize(ThreadPoolName.RENDERER));
        assertEquals(new PoolSize(2, 1024), config.poolSize(ThreadPoolName.RENDERER));
    }

    @Test
    void testInvalidQueueSizeFallsBackToTheBuiltInQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8),
            Map.of(ThreadPoolName.RENDERER, new PoolSize(6, 0)));

        assertEquals(new PoolSize(6, 1024), config.poolSize(ThreadPoolName.RENDERER));
    }

    /**
     * Returns the properties of a configuration file with the given defaults and further settings.
     */
    private static Map<String, Object> properties(Object threads, Object queueSize, Object timeoutSeconds,
            Object... more) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("threads", threads);
        properties.put("queueSize", queueSize);
        properties.put("shutDownTimeoutSeconds", timeoutSeconds);
        for (int i = 0; i < more.length; i += 2) {
            properties.put((String) more[i], more[i + 1]);
        }
        return properties;
    }

    /**
     * Returns what the given action logs. slf4j-simple, the logger used by the tests, writes to the current
     * {@link System#err}.
     */
    private String logOf(Runnable action) {
        PrintStream err = System.err;
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        System.setErr(new PrintStream(log, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setErr(err);
        }
        return log.toString(StandardCharsets.UTF_8);
    }

    @Test
    void testConfigurationIsApplied() {
        // Values from a .cfg file are strings
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5"));

        assertEquals(new PoolSize(2, 8), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(5), config.shutDownTimeout());
    }

    @Test
    void testChangesAreNotAppliedUntilRestart() {
        DefaultExecutorPoolConfig config = new DefaultExecutorPoolConfig(
            properties("2", "8", "5", "service-delete.maxRequests", "4"));

        config.modified(properties("6", "16", "30", "service-delete.maxRequests", "12"));

        assertEquals(new PoolSize(2, 8), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(5), config.shutDownTimeout());
        assertEquals(4, config.maxRequests(LIMIT));
    }

    @Test
    void testMissingSettingsUseTheBuiltInDefaults() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Map.of());

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
        assertEquals(16, config.maxRequests(LIMIT));
    }

    @Test
    void testDefaultSettingsThatAreNotNumbersFallBackToTheBuiltInDefaults() {
        // The typed configuration getters would throw here and stop the components that use the pools from starting
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("many", "lots", "forever"));

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
    }

    @Test
    void testInvalidDefaultSettingsFallBackToTheBuiltInDefaults() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("0", "-1", "-5"));

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
    }

    @Test
    void testPoolSizesAreReadFromTheProperties() {
        ExecutorPoolConfig[] config = new ExecutorPoolConfig[1];
        String log = logOf(() -> config[0] = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "olm-get-pm.threads", "6",
            "olm-get-pm.queueSize", "16")));

        assertEquals(new PoolSize(6, 16), config[0].poolSize(POOL));
        assertFalse(log.contains("there is no pool named"), log);
    }

    @Test
    void testPoolWithOnlyOneSizeUsesTheDefaultForTheOther() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties(2, 8, 5,
            "olm-get-pm.threads", 6));

        assertEquals(new PoolSize(6, 8), config.poolSize(POOL));
    }

    @Test
    void testPoolWithOnlyThreadsUsesItsBuiltInQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "renderer.threads", "6"));

        assertEquals(new PoolSize(6, 1024), config.poolSize(ThreadPoolName.RENDERER));
    }

    @Test
    void testQueueSizeOverridesTheBuiltInQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "renderer.queueSize", "32"));

        assertEquals(new PoolSize(2, 32), config.poolSize(ThreadPoolName.RENDERER));
    }

    @Test
    void testPoolSizesThatAreNotNumbersFallBackToTheDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "olm-get-pm.threads", "many",
            "olm-get-pm.queueSize", "16"));

        assertEquals(new PoolSize(2, 16), config.poolSize(POOL));
    }

    @Test
    void testPoolSizesFallBackToTheBuiltInDefaultsWhenTheDefaultsAreInvalid() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("many", "0", "5",
            "olm-get-pm.queueSize", "16"));

        assertEquals(new PoolSize(4, 16), config.poolSize(POOL));
    }

    @Test
    void testUnknownPoolsAreIgnored() {
        ExecutorPoolConfig[] config = new ExecutorPoolConfig[1];
        String log = logOf(() -> config[0] = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "olm-get-pn.threads", "6")));

        assertEquals(new PoolSize(2, 8), config[0].poolSize(POOL));
        assertTrue(log.contains("Ignoring olm-get-pn.threads: there is no pool named olm-get-pn. Known pools are"),
            log);
    }

    @Test
    void testMaxRequestsOverridesTheBuiltInDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8),
            Map.of(), Map.of(LIMIT, 4));

        assertEquals(4, config.maxRequests(LIMIT));
    }

    @Test
    void testInvalidMaxRequestsFallsBackToTheBuiltInDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 8),
            Map.of(), Map.of(LIMIT, 0));

        assertEquals(16, config.maxRequests(LIMIT));
    }

    @Test
    void testMaxRequestsAreReadFromTheProperties() {
        ExecutorPoolConfig[] config = new ExecutorPoolConfig[1];
        String log = logOf(() -> config[0] = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            LIMIT.limitName() + ".maxRequests", "4")));

        assertEquals(4, config[0].maxRequests(LIMIT));
        assertFalse(log.contains("there is no request limit named"), log);
    }

    @Test
    void testMaxRequestsThatAreNotNumbersFallBackToTheBuiltInDefault() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            LIMIT.limitName() + ".maxRequests", "many"));

        assertEquals(16, config.maxRequests(LIMIT));
    }

    @Test
    void testUnknownRequestLimitsAreIgnored() {
        ExecutorPoolConfig[] config = new ExecutorPoolConfig[1];
        String log = logOf(() -> config[0] = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "service-delet.maxRequests", "4")));

        assertEquals(16, config[0].maxRequests(LIMIT));
        assertTrue(log.contains(
            "Ignoring service-delet.maxRequests: there is no request limit named service-delet. Known limits are"),
            log);
    }

    @Test
    void testOperationQueueSizeOverridesTheBuiltInQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 64),
            Map.of(), Map.of(), Map.of(QUEUE, 3));

        assertEquals(3, config.queueSize(QUEUE));
    }

    @Test
    void testInvalidOperationQueueSizeFallsBackToTheBuiltInQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(2, 64),
            Map.of(), Map.of(), Map.of(QUEUE, 0));

        assertEquals(8, config.queueSize(QUEUE));
    }

    @Test
    void testOperationQueueSizesAreReadFromTheProperties() {
        ExecutorPoolConfig[] config = new ExecutorPoolConfig[1];
        String log = logOf(() -> config[0] = new DefaultExecutorPoolConfig(properties("2", "64", "5",
            QUEUE.queueName() + ".queueSize", "3")));

        assertEquals(3, config[0].queueSize(QUEUE));
        assertFalse(log.contains("there is no pool named"), log);
    }

    @Test
    void testOperationQueueDoesNotUseTheDefaultQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "64", "5"));

        assertEquals(8, config.queueSize(QUEUE));
    }

    @Test
    void testOperationQueueSizesThatAreNotNumbersFallBackToTheBuiltInQueueSize() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "64", "5",
            QUEUE.queueName() + ".queueSize", "many"));

        assertEquals(8, config.queueSize(QUEUE));
    }

    @Test
    void testOperationQueueHasNoThreads() {
        String log = logOf(() -> new DefaultExecutorPoolConfig(properties("2", "64", "5",
            QUEUE.queueName() + ".threads", "3")));

        assertTrue(log.contains("Ignoring service-create.threads: there is no pool named service-create."), log);
    }
}
