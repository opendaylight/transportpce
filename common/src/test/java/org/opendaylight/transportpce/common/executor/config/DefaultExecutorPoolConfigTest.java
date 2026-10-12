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
}
