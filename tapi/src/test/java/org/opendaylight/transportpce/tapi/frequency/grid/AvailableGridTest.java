/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.grid;

import java.util.Arrays;
import java.util.BitSet;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class AvailableGridTest {

    @Test
    void testSize() {
        byte[] frequencies = {0, -1, -1, 0, 0, 0, 0, -1};

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(64, availableGrid.assignedFrequencies().size());
        Assert.assertEquals(64, availableGrid.availableFrequencies().size());
    }

    @Test
    void availableFrequencyRanges() {
        byte[] frequencies = {0, -1, -1, 0, 0, 0, 0, -1};
        byte[] expected = {0, -1, -1, 0, 0, 0, 0, -1};

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertArrayEquals(expected, availableGrid.availableFrequencyRanges());
    }

    @Test
    void availableFrequencyRangesAreInMutable() {
        byte[] frequencies = {0, -1, -1, 0, 0, 0, 0, -1};
        byte[] expected = {0, -1, -1, 0, 0, 0, 0, -1};

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        //This change shouldn't affect the contents of the assignedFrequencies object
        frequencies[0] = -1;

        //The arrays are different
        Assert.assertFalse(Arrays.equals(expected, frequencies));

        //The contents of the assignedFrequencies object are still the same
        Assert.assertArrayEquals(expected, availableGrid.availableFrequencyRanges());
    }


    @Test
    void assignedFrequenciesStart() {
        byte[] frequencies = {-1, 0, 0, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(0, 8);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesMiddle() {
        byte[] frequencies = {0, -1, 0, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(8, 16);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesConsecutive() {
        byte[] frequencies = {0, -1, -1, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(8, 24);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesEnd() {
        byte[] frequencies = {0, 0, 0, 0, 0, 0, 0, -1};
        BitSet expected = new BitSet();
        expected.set(56, 64);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesPartial() {
        byte[] frequencies = {0, 0, 0, 0, 0, 0, 0, 56};
        BitSet expected = new BitSet();
        expected.set(59, 62);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesJoined() {
        byte[] frequencies = {0, -128, 7, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(15, 19);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesAll() {
        byte[] frequencies = {-1, -1, -1, -1, -1, -1, -1, -1};
        BitSet expected = new BitSet();
        expected.set(0, 64);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void assignedFrequenciesNone() {
        byte[] frequencies = {0, 0, 0, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        Assert.assertEquals(expected, availableGrid.availableFrequencies());
    }

    @Test
    void availableFrequencies() {
        byte[] frequencies = {0, -1, 0, 0, 0, 0, 0, 0};

        BitSet expected = new BitSet();
        expected.set(0, 8);
        expected.set(16, 64);

        AvailableGrid availableGrid = new AvailableGrid(frequencies);

        BitSet assigned = availableGrid.assignedFrequencies();
        Assert.assertEquals(expected, assigned);

        BitSet available = availableGrid.availableFrequencies();
        Assert.assertEquals(BitSet.valueOf(frequencies), available);

        Assert.assertNotEquals(available, assigned);
    }

}