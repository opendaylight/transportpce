/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.range;

import java.math.BigDecimal;
import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;

class McCapabilityRangeTest {

    @Test
    void minAndMaxEqualTwoGridRange() {
        BigDecimal minFrequency = BigDecimal.valueOf(191.325);
        BigDecimal maxFrequency = BigDecimal.valueOf(196.125);

        FrequencyRange frequencyRange = new McCapabilityRange(minFrequency, maxFrequency);

        BitSet expected = new BitSet();
        expected.set(0, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                frequencyRange.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void minLargerThanGridRangeMinAndMaxEqualGridRangeMax() {
        BigDecimal minFrequency = BigDecimal.valueOf(191.326);
        BigDecimal maxFrequency = BigDecimal.valueOf(196.125);

        FrequencyRange frequencyRange = new McCapabilityRange(minFrequency, maxFrequency);

        BitSet expected = new BitSet();
        expected.set(1, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                frequencyRange.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void minLargerThanGridRangeMinAndMaxLessThanGridRangeMax() {
        BigDecimal minFrequency = BigDecimal.valueOf(191.326);
        BigDecimal maxFrequency = BigDecimal.valueOf(196.124);

        FrequencyRange frequencyRange = new McCapabilityRange(minFrequency, maxFrequency);

        BitSet expected = new BitSet();
        expected.set(1, 767);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                frequencyRange.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void minSmallerThanGridRangeMinAndMaxLargerThanGridRangeMax() {
        BigDecimal minFrequency = BigDecimal.valueOf(191.324);
        BigDecimal maxFrequency = BigDecimal.valueOf(196.126);

        FrequencyRange frequencyRange = new McCapabilityRange(minFrequency, maxFrequency);

        BitSet expected = new BitSet();
        expected.set(0, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                frequencyRange.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void fromBothNullReturnsEntireGridRange() {
        FrequencyRange result = McCapabilityRange.from(null, null, 6.25, 191.325, 768);
        Assertions.assertInstanceOf(EntireGridRange.class, result);
    }

    @Test
    void fromMinNullDefaultsToGridEdge() {
        FrequencyRange result = McCapabilityRange.from(
                null,
                FrequencyTHz.getDefaultInstance("196.125"),
                6.25, 191.325, 768);

        BitSet expected = new BitSet();
        expected.set(0, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                result.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void fromMaxNullDefaultsToGridMax() {
        FrequencyRange result = McCapabilityRange.from(
                FrequencyTHz.getDefaultInstance("191.326"),
                null,
                6.25, 191.325, 768);

        BitSet expected = new BitSet();
        expected.set(1, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                result.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void fromBothPresentMinLessThanMax() {
        FrequencyRange result = McCapabilityRange.from(
                FrequencyTHz.getDefaultInstance("191.325"),
                FrequencyTHz.getDefaultInstance("196.125"),
                6.25, 191.325, 768);

        BitSet expected = new BitSet();
        expected.set(0, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                result.gridRange(6.25, 191.325, 768).toByteArray());
    }

    @Test
    void fromSwapsMinAndMaxWhenMinLargerThanMax() {
        FrequencyRange result = McCapabilityRange.from(
                FrequencyTHz.getDefaultInstance("196.125"),
                FrequencyTHz.getDefaultInstance("191.325"),
                6.25, 191.325, 768);

        BitSet expected = new BitSet();
        expected.set(0, 768);
        Assertions.assertArrayEquals(
                expected.toByteArray(),
                result.gridRange(6.25, 191.325, 768).toByteArray());
    }
}
