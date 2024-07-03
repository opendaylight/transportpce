/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.util.BitSet;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NumericFrequencyTest {

    @Test
    void assignedFrequency() {
        BitSet assigned = new BitSet(768);
        assigned.set(760, 768);

        BitMap used = Mockito.mock(BitMap.class);
        Mockito.when(used.assignedFrequencies()).thenReturn(assigned);

        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        Mockito.when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        Map<Double, Double> expected = Map.of(196.075, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        Assert.assertEquals(expected, frequencyService.assignedFrequency(used));

    }

    @Test
    void assignedStartAndEndFrequencyRanges() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(8)).thenReturn(191.375);
        Mockito.when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        Mockito.when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet assigned = new BitSet(768);
        assigned.set(0, 8);
        assigned.set(760, 768);

        BitMap used = Mockito.mock(BitMap.class);
        Mockito.when(used.assignedFrequencies()).thenReturn(assigned);

        Map<Double, Double> expected = Map.of(191.325, 191.375, 196.075, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        Assert.assertEquals(expected, frequencyService.assignedFrequency(used));

    }

    @Test
    void assignedMiddleFrequencyRanges() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(32)).thenReturn(191.525);
        Mockito.when(math.getStartFrequencyFromIndex(40)).thenReturn(191.575);

        BitSet assigned = new BitSet(768);
        assigned.set(32, 40);

        BitMap used = Mockito.mock(BitMap.class);
        Mockito.when(used.assignedFrequencies()).thenReturn(assigned);

        Map<Double, Double> expected = Map.of(191.525, 191.575);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        Assert.assertEquals(expected, frequencyService.assignedFrequency(used));

    }

    @Test
    void assignedStartMiddleAndEndFrequencyRanges() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(8)).thenReturn(191.375);
        Mockito.when(math.getStartFrequencyFromIndex(32)).thenReturn(191.525);
        Mockito.when(math.getStartFrequencyFromIndex(40)).thenReturn(191.575);
        Mockito.when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        Mockito.when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet assigned = new BitSet(768);
        assigned.set(0, 8);
        assigned.set(32, 40);
        assigned.set(760, 768);

        BitMap used = Mockito.mock(BitMap.class);
        Mockito.when(used.assignedFrequencies()).thenReturn(assigned);

        Map<Double, Double> expected = Map.of(191.325, 191.375, 191.525, 191.575, 196.075, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        Assert.assertEquals(expected, frequencyService.assignedFrequency(used));

    }

    @Test
    void availableFrequency() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);

        BitSet available = new BitSet(768);
        available.set(0, 760);

        BitMap bitMap = Mockito.mock(BitMap.class);
        Mockito.when(bitMap.availableFrequencies()).thenReturn(available);

        Map<Double, Double> expected = Map.of(191.325, 196.075);
        Assert.assertEquals(expected, frequencyService.availableFrequency(bitMap));
    }

    @Test
    void availableSplitByAssignedMiddleFrequencyRanges() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(32)).thenReturn(191.525);
        Mockito.when(math.getStartFrequencyFromIndex(40)).thenReturn(191.575);
        Mockito.when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet available = new BitSet(768);
        available.set(0, 32);
        available.set(40, 768);

        BitMap used = Mockito.mock(BitMap.class);
        Mockito.when(used.availableFrequencies()).thenReturn(available);

        Map<Double, Double> expected = Map.of(191.325, 191.525, 191.575, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        Assert.assertEquals(expected, frequencyService.availableFrequency(used));

    }
}