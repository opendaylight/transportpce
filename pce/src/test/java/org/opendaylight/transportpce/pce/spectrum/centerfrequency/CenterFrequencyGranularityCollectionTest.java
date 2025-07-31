/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.centerfrequency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CenterFrequencyGranularityCollectionTest {

    @Test
    void addNull() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        assertFalse(centerFrequencyGranularityCollection.add(null));
    }

    @Test
    void testDefault() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        assertEquals(BigDecimal.valueOf(50), centerFrequencyGranularityCollection.leastCommonMultipleInGHz());
    }

    @Test
    void testDefaultSlots() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        assertEquals(8, centerFrequencyGranularityCollection.slots(6.25));
    }

    @Test
    void addInteger() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        assertTrue(centerFrequencyGranularityCollection.add(5));
        assertFalse(centerFrequencyGranularityCollection.add(5));
    }

    @Test
    void addFloat() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        assertTrue(centerFrequencyGranularityCollection.add(5.0));
        assertFalse(centerFrequencyGranularityCollection.add(5.0));
    }

    @Test
    void immutable() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(5);

        Set<BigDecimal> set = centerFrequencyGranularityCollection.set();
        assertEquals(1, set.size());

        set.add(new BigDecimal(6));

        assertEquals(1, centerFrequencyGranularityCollection.set().size());
    }

    @Test
    void leastCommonMultipleOnlyIntegersInGHz() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(6);
        centerFrequencyGranularityCollection.add(12);
        centerFrequencyGranularityCollection.add(18);

        assertEquals(BigDecimal.valueOf(36), centerFrequencyGranularityCollection.leastCommonMultipleInGHz());
    }

    @Test
    void leastCommonMultipleDecimalInGHz() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(50.0);
        centerFrequencyGranularityCollection.add(6.25);
        centerFrequencyGranularityCollection.add(75);
        centerFrequencyGranularityCollection.add(12.5);

        assertEquals(BigDecimal.valueOf(150), centerFrequencyGranularityCollection.leastCommonMultipleInGHz());
    }

    @Test
    void testSet() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(50.0);
        centerFrequencyGranularityCollection.add(6.25);
        centerFrequencyGranularityCollection.add(75);
        centerFrequencyGranularityCollection.add(12.5);

        Set<BigDecimal> expected = new LinkedHashSet<>(
            List.of(
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(75)
            )
        );

        assertEquals(expected, centerFrequencyGranularityCollection.set());
    }

    @Test
    void testSlots() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(50.0);
        centerFrequencyGranularityCollection.add(BigDecimal.valueOf(6.25));
        centerFrequencyGranularityCollection.add(75);
        centerFrequencyGranularityCollection.add(12.5);

        assertEquals(24, centerFrequencyGranularityCollection.slots(6.25));
    }

    @Test
    void testFrequencyGranularityIsDividableByCenterFrequencyGranularity() {
        // All nodes in the have a center frequency granularity = 6.25 / 2
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(3.125);

        // Since frequency granularity is evenly dividable by center frequency granularity,
        // we'll treat center frequency granularity as equal to frequency granularity.
        // Meaning it is possible to fit 3.125GHz on a 6.25GHz grid.
        assertEquals(1, centerFrequencyGranularityCollection.slots(6.25));
    }

    @Test
    void testFrequencyGranularityIsNotDividableByCenterFrequencyGranularity() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(2);

        // Even though 6.25GHz is not even dividable into m x 2GHz slots, there is a
        // "common ground" for these two frequencies at 50GHz. Meaning a node with
        // center frequency granularity of 2GHz should be able to support a service
        // using center frequencies at 193.1 ± n x 50GHz on a 6.25GHz grid.
        assertEquals(8, centerFrequencyGranularityCollection.slots(6.25));
    }

    @Test
    void testFrequencyGranularityIsNotDividableByCenterFrequencyGranularityTwo() {
        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        centerFrequencyGranularityCollection.add(4.6875);

        // Even though 6.25GHz is not even dividable into m x 4.6875GHz slots, there is a
        // "common ground" for these two frequencies at 18.75GHz. Meaning a node with
        // center frequency granularity of 4.6875GHz should be able to support a service
        // using center frequencies at 193.1 ± n x 18.75GHz on a 6.25GHz grid.
        assertEquals(3, centerFrequencyGranularityCollection.slots(6.25));
    }
}
