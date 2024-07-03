/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;


import java.util.BitSet;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class AssignedGridFactoryTest {

    @Test
    void assignedFrequency() {
        AssignedGridFactory assignedGridFactory = new AssignedGridFactory();
        byte[] frequency = {0, -1, -1, -1, -1, -1, -1, -1};
        byte[] expected =  {0, -1, -1, -1, -1, -1, -1, -1};

        Assert.assertArrayEquals(
                expected,
                assignedGridFactory.fromAssigned(frequency).assignedFrequencyRanges()
        );
    }

    @Test
    void availableFrequency() {
        AssignedGridFactory assignedGridFactory = new AssignedGridFactory();

        byte[] availableFrequencies        =  {-1,  0,  0,  0,  0,  0,  0,  0};
        byte[] expectedAssignedFrequencies =  { 0, -1, -1, -1, -1, -1, -1, -1};

        Assigned bitMap = assignedGridFactory.fromAvailable(availableFrequencies);

        byte[] assignedFrequencies = bitMap.assignedFrequencyRanges();

        Assert.assertArrayEquals(
                expectedAssignedFrequencies,
                assignedFrequencies
        );

        Assert.assertArrayEquals(
                assignedFrequencies,
                bitMap.assignedFrequencyRanges()
        );

        Assert.assertEquals(
                BitSet.valueOf(availableFrequencies),
                bitMap.availableFrequencies()
        );

    }
}