/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.BitSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NumericFrequencyTest {

    @Test
    void assignedFrequency() {
        BitSet assigned = new BitSet(768);
        assigned.set(760, 768);

        Available used = mock(Available.class);
        when(used.assignedFrequencies()).thenReturn(assigned);

        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        Map<Double, Double> expected = Map.of(196.075, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        assertEquals(expected, frequencyService.assignedFrequency(used));
    }

    @Test
    void assignedStartAndEndFrequencyRanges() {
        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(8)).thenReturn(191.375);
        when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet assigned = new BitSet(768);
        assigned.set(0, 8);
        assigned.set(760, 768);

        Available used = mock(Available.class);
        when(used.assignedFrequencies()).thenReturn(assigned);

        Map<Double, Double> expected = Map.of(191.325, 191.375, 196.075, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        assertEquals(expected, frequencyService.assignedFrequency(used));
    }

    @Test
    void assignedMiddleFrequencyRanges() {
        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(32)).thenReturn(191.525);
        when(math.getStartFrequencyFromIndex(40)).thenReturn(191.575);

        BitSet assigned = new BitSet(768);
        assigned.set(32, 40);

        Available used = mock(Available.class);
        when(used.assignedFrequencies()).thenReturn(assigned);

        Map<Double, Double> expected = Map.of(191.525, 191.575);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        assertEquals(expected, frequencyService.assignedFrequency(used));
    }

    @Test
    void assignedStartMiddleAndEndFrequencyRanges() {
        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(8)).thenReturn(191.375);
        when(math.getStartFrequencyFromIndex(32)).thenReturn(191.525);
        when(math.getStartFrequencyFromIndex(40)).thenReturn(191.575);
        when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet assigned = new BitSet(768);
        assigned.set(0, 8);
        assigned.set(32, 40);
        assigned.set(760, 768);

        Available used = mock(Available.class);
        when(used.assignedFrequencies()).thenReturn(assigned);

        Map<Double, Double> expected = Map.of(191.325, 191.375, 191.525, 191.575, 196.075, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        assertEquals(expected, frequencyService.assignedFrequency(used));
    }

    @Test
    void availableFrequency() {
        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);

        BitSet available = new BitSet(768);
        available.set(0, 760);

        Available bitMap = mock(Available.class);
        when(bitMap.availableFrequencies()).thenReturn(available);

        Map<Double, Double> expected = Map.of(191.325, 196.075);
        assertEquals(expected, frequencyService.availableFrequency(bitMap));
    }

    @Test
    void availableSplitByAssignedMiddleFrequencyRanges() {
        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(32)).thenReturn(191.525);
        when(math.getStartFrequencyFromIndex(40)).thenReturn(191.575);
        when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet available = new BitSet(768);
        available.set(0, 32);
        available.set(40, 768);

        Available used = mock(Available.class);
        when(used.availableFrequencies()).thenReturn(available);

        Map<Double, Double> expected = Map.of(191.325, 191.525, 191.575, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        assertEquals(expected, frequencyService.availableFrequency(used));
    }

    @Test
    void flexGrid() {
        Math math = mock(Math.class);
        when(math.getStartFrequencyFromIndex(5)).thenReturn(191.35625);
        when(math.getStartFrequencyFromIndex(11)).thenReturn(191.39374999999998);
        when(math.getStartFrequencyFromIndex(19)).thenReturn(191.44375);
        when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        BitSet available = new BitSet(768);
        available.set(5, 11);
        available.set(19, 768);

        Available used = mock(Available.class);
        when(used.availableFrequencies()).thenReturn(available);

        Map<Double, Double> expected = Map.of(191.35625, 191.39374999999998, 191.44375, 196.125);

        Numeric frequencyService = new NumericFrequency(191.325, 768, math);
        assertEquals(expected, frequencyService.availableFrequency(used));
    }
}