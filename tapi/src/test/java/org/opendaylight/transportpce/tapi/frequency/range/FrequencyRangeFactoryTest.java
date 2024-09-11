/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;


class FrequencyRangeFactoryTest {

    @Test
    void effectiveRange() {
        RangeFactory rangeFactory = new FrequencyRangeFactory();

        Range range = rangeFactory.effectiveRange(191.325,  6.25, 768);

        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(191.325), new TeraHertz(196.125));

        Assertions.assertEquals(expected, range.ranges());
    }

    @Test
    void range() {

        RangeFactory rangeFactory = new FrequencyRangeFactory();

        Range range = rangeFactory.range(191.35,  50.0);

        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(191.325), new TeraHertz(191.375));

        Assertions.assertEquals(expected, range.ranges());

    }
}