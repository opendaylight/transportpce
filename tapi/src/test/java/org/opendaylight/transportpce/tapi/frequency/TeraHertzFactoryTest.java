/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;

class TeraHertzFactoryTest {

    @Test
    void testFirstBitEqualsStartEdgeFrequency() {

        Factory lightFactory = new TeraHertzFactory();

        Frequency light = lightFactory.frequency(GridConstant.START_EDGE_FREQUENCY, GridConstant.GRANULARITY, 0);

        Assertions.assertTrue(BigDecimal.valueOf(191.325).equals(light.teraHertz()));
    }

    @Test
    void testLastBitEqualsEndFrequency() {
        Factory lightFactory = new TeraHertzFactory();

        Frequency light = lightFactory.frequency(GridConstant.START_EDGE_FREQUENCY, GridConstant.GRANULARITY, 768);

        Assertions.assertTrue(BigDecimal.valueOf(196.125).equals(light.teraHertz()));
    }

    @Test
    void testCenterFrequencyLower() {
        Factory lightFactory = new TeraHertzFactory();

        Frequency light = lightFactory.lower(193.73125, 6.25);

        Assertions.assertTrue(BigDecimal.valueOf(193.728125).equals(light.teraHertz()));
    }

    @Test
    void testCenterFrequencyUpper() {
        Factory lightFactory = new TeraHertzFactory();

        Frequency light = lightFactory.upper(193.73125, 6.25);

        Assertions.assertTrue(BigDecimal.valueOf(193.734375).equals(light.teraHertz()));
    }
}