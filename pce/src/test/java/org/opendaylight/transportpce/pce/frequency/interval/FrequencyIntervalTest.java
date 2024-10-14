/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FrequencyIntervalTest {

    @Test
    void testEquals() {
        Interval i1 = new FrequencyInterval(BigDecimal.valueOf(191), BigDecimal.valueOf(192));
        Interval i2 = new FrequencyInterval(BigDecimal.valueOf(191), BigDecimal.valueOf(192));

        assertEquals(i1, i2);
    }

    @Test
    void testNotEquals() {
        Interval i1 = new FrequencyInterval(BigDecimal.valueOf(191), BigDecimal.valueOf(193));
        Interval i2 = new FrequencyInterval(BigDecimal.valueOf(191), BigDecimal.valueOf(192));

        assertNotEquals(i1, i2);
    }

    @Test
    void testHashCode() {
        Interval i1 = new FrequencyInterval(BigDecimal.valueOf(191), BigDecimal.valueOf(192));
        Interval i2 = new FrequencyInterval(BigDecimal.valueOf(191), BigDecimal.valueOf(192));

        assertEquals(i1.hashCode(), i2.hashCode());
    }
}
