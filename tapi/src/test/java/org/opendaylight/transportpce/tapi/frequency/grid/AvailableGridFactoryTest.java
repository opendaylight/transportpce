/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.grid;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

class AvailableGridFactoryTest {

    @Test
    void assignedFrequency() {
        AvailableGridFactory availableGridFactory = new AvailableGridFactory();
        byte[] frequency = {0, -1, -1, -1, -1, -1, -1, -1};
        byte[] expected =  {0, -1, -1, -1, -1, -1, -1, -1};

        assertArrayEquals(
                expected,
                availableGridFactory.fromAvailable(frequency).availableFrequencyRanges());
    }

    @Test
    void availableFrequency() {
        AvailableGridFactory availableGridFactory = new AvailableGridFactory();

        byte[] assignedFrequencies          =  {-1,  0,  0,  0,  0,  0,  0,  0};
        byte[] expectedAvailableFrequencies =  { 0, -1, -1, -1, -1, -1, -1, -1};

        Available bitMap = availableGridFactory.fromAssigned(assignedFrequencies);

        byte[] availableFrequencies = bitMap.availableFrequencyRanges();

        assertArrayEquals(
                expectedAvailableFrequencies,
                availableFrequencies);

        assertArrayEquals(
                availableFrequencies,
                bitMap.availableFrequencyRanges());

        assertEquals(
                BitSet.valueOf(assignedFrequencies),
                bitMap.assignedFrequencies());
    }
}