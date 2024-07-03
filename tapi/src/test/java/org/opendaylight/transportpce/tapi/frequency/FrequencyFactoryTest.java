/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;


import org.junit.Assert;
import org.junit.jupiter.api.Test;

class FrequencyFactoryTest {

    @Test
    void assignedFrequencyThrowsException() {
        FrequencyFactory frequencyFactory = new FrequencyFactory();
        byte[] frequency = {0, 1, 1, 1, 1, 1, 1, 1};

        Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> {
            frequencyFactory.assigned(frequency, (byte) 0, (byte) -1).assignedFrequencyRanges();
        });
    }

    @Test
    void assignedFrequency() {
        FrequencyFactory frequencyFactory = new FrequencyFactory();
        byte[] frequency = {0, 1, 1, 1, 1, 1, 1, 1};
        byte[] expected = {0, 1, 1, 1, 1, 1, 1, 1};

        Assert.assertArrayEquals(
                expected,
                frequencyFactory.assigned(frequency, (byte) 1, (byte) 0).assignedFrequencyRanges()
        );
    }

    @Test
    void assignedFrequencyConversion() {
        FrequencyFactory frequencyFactory = new FrequencyFactory();
        byte[] frequency = {-1, 0, 0, 0, 0, 0, 0, 0};
        byte[] expected = {0, 1, 1, 1, 1, 1, 1, 1};

        Assert.assertArrayEquals(
                expected,
                frequencyFactory.assigned(frequency, (byte) 0, (byte) -1).assignedFrequencyRanges()
        );

    }
}