/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;

class DefaultExecutorPoolConfigTest {

    private static final ThreadPoolName POOL = ThreadPoolName.OLM_GET_PM;

    @Test
    void testBuiltInDefaults() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig();

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
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

    @Test
    void testConfigurationIsApplied() {
        // Values from a .cfg file are strings
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5"));

        assertEquals(new PoolSize(2, 8), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(5), config.shutDownTimeout());
    }

    @Test
    void testMissingSettingsUseTheBuiltInDefaults() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Map.of());

        assertEquals(new PoolSize(4, 64), config.poolSize(POOL));
        assertEquals(Duration.ofSeconds(20), config.shutDownTimeout());
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
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "olm-get-pm.threads", "6",
            "olm-get-pm.queueSize", "16"));

        assertEquals(new PoolSize(6, 16), config.poolSize(POOL));
    }

    @Test
    void testPoolWithOnlyOneSizeUsesTheDefaultForTheOther() {
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties(2, 8, 5,
            "olm-get-pm.threads", 6));

        assertEquals(new PoolSize(6, 8), config.poolSize(POOL));
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
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(properties("2", "8", "5",
            "olm-get-pn.threads", "6"));

        assertEquals(new PoolSize(2, 8), config.poolSize(POOL));
    }
}
