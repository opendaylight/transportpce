/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

class McCapabilityCollectionTest {

    @Test
    void add() {

        McCapability mcCapability = mock(McCapability.class);

        CapabilityCollection slotCollection = new McCapabilityCollection();
        assertTrue(slotCollection.add(mcCapability));
    }

    @Test
    void emptyCollectionIsCompatible() {

        CapabilityCollection slotCollection = new McCapabilityCollection();
        assertTrue(slotCollection.isCompatibleService(6.25, 8));

    }

    @Test
    void isCompatibleService() {

        McCapability mcCapability = mock(McCapability.class);
        when(mcCapability.isCompatibleWithServiceFrequency(any(), any())).thenReturn(true);

        CapabilityCollection slotCollection = new McCapabilityCollection();
        slotCollection.add(mcCapability);
        assertTrue(slotCollection.isCompatibleService(6.25, 8));

    }

    @Test
    void isNotCompatibleService() {

        McCapability mcCapability = mock(McCapability.class);
        when(mcCapability.isCompatibleWithServiceFrequency(any())).thenReturn(false);

        CapabilityCollection slotCollection = new McCapabilityCollection();
        slotCollection.add(mcCapability);
        assertFalse(slotCollection.isCompatibleService(6.25, 8));

    }

    @Test
    void multipleWhereOneIsFalseReturnsFalse() {

        CapabilityCollection slotCollection = new McCapabilityCollection();

        McCapability mcCapabilityOne = mock(McCapability.class);
        when(mcCapabilityOne.isCompatibleWithServiceFrequency(any(), any())).thenReturn(true);
        assertTrue(slotCollection.add(mcCapabilityOne));

        McCapability mcCapabilityTwo = mock(McCapability.class);
        when(mcCapabilityTwo.isCompatibleWithServiceFrequency(any(), any())).thenReturn(true);
        assertTrue(slotCollection.add(mcCapabilityTwo));

        assertTrue(slotCollection.isCompatibleService(6.25, 8));

        McCapability mcCapabilityThree = mock(McCapability.class);
        when(mcCapabilityThree.isCompatibleWithServiceFrequency(any())).thenReturn(false);
        assertTrue(slotCollection.add(mcCapabilityThree));

        assertFalse(slotCollection.isCompatibleService(6.25, 8));

    }

    @Test
    void emptyCollectionReturnsAvailableGridUnchanged() {
        BitSet available = new BitSet(768);
        available.set(0, 768);

        CapabilityCollection collection = new McCapabilityCollection();

        BitSet expected = new BitSet(768);
        expected.set(0, 768);
        assertArrayEquals(expected.toByteArray(),
                collection.usableFrequencyRange(available, 6.25, 191.325, 768).toByteArray());
    }

    @Test
    void singleCapabilityWithFullRangeReturnsAvailableGridUnchanged() {
        BitSet available = new BitSet(768);
        available.set(0, 768);

        BitSet fullRange = new BitSet(768);
        fullRange.set(0, 768);

        McCapability mcCapability = mock(McCapability.class);
        when(mcCapability.supportableFrequencyRange(6.25, 191.325, 768)).thenReturn(fullRange);

        CapabilityCollection collection = new McCapabilityCollection();
        collection.add(mcCapability);

        BitSet expected = new BitSet(768);
        expected.set(0, 768);
        assertArrayEquals(expected.toByteArray(),
                collection.usableFrequencyRange(available, 6.25, 191.325, 768).toByteArray());
    }

    @Test
    void singleCapabilityNarrowsAvailableGrid() {
        BitSet available = new BitSet(768);
        available.set(0, 768);

        BitSet restricted = new BitSet(768);
        restricted.set(100, 668);

        McCapability mcCapability = mock(McCapability.class);
        when(mcCapability.supportableFrequencyRange(6.25, 191.325, 768)).thenReturn(restricted);

        CapabilityCollection collection = new McCapabilityCollection();
        collection.add(mcCapability);

        BitSet expected = new BitSet(768);
        expected.set(100, 668);
        assertArrayEquals(expected.toByteArray(),
                collection.usableFrequencyRange(available, 6.25, 191.325, 768).toByteArray());
    }

    @Test
    void multipleCapabilitiesNarrowAvailableGrid() {
        BitSet available = new BitSet(768);
        available.set(0, 768);

        BitSet rangeOne = new BitSet(768);
        rangeOne.set(0, 500);

        BitSet rangeTwo = new BitSet(768);
        rangeTwo.set(200, 768);

        McCapability capabilityOne = mock(McCapability.class);
        when(capabilityOne.supportableFrequencyRange(6.25, 191.325, 768)).thenReturn(rangeOne);

        McCapability capabilityTwo = mock(McCapability.class);
        when(capabilityTwo.supportableFrequencyRange(6.25, 191.325, 768)).thenReturn(rangeTwo);

        CapabilityCollection collection = new McCapabilityCollection();
        collection.add(capabilityOne);
        collection.add(capabilityTwo);

        // intersection of [0,500) and [200,768) is [200,500)
        BitSet expected = new BitSet(768);
        expected.set(200, 500);
        assertArrayEquals(expected.toByteArray(),
                collection.usableFrequencyRange(available, 6.25, 191.325, 768).toByteArray());
    }

    @Test
    void availableFrequencyGridIsNotModified() {
        BitSet available = new BitSet(768);
        available.set(0, 768);

        BitSet restricted = new BitSet(768);
        restricted.set(100, 668);

        McCapability mcCapability = mock(McCapability.class);
        when(mcCapability.supportableFrequencyRange(6.25, 191.325, 768)).thenReturn(restricted);

        CapabilityCollection collection = new McCapabilityCollection();
        collection.add(mcCapability);
        collection.usableFrequencyRange(available, 6.25, 191.325, 768);

        BitSet expected = new BitSet(768);
        expected.set(0, 768);
        assertArrayEquals(expected.toByteArray(), available.toByteArray());
    }

    @Test
    void capabilityExcludingAllSlotsReturnsEmptyBitSet() {
        BitSet available = new BitSet(768);
        available.set(0, 768);

        McCapability mcCapability = mock(McCapability.class);
        when(mcCapability.supportableFrequencyRange(6.25, 191.325, 768)).thenReturn(new BitSet(768));

        CapabilityCollection collection = new McCapabilityCollection();
        collection.add(mcCapability);

        assertTrue(collection.usableFrequencyRange(available, 6.25, 191.325, 768).isEmpty());
    }

}
