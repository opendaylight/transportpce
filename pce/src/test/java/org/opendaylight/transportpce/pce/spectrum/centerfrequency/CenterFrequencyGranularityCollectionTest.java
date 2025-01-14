/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.centerfrequency;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CenterFrequencyGranularityCollectionTest {

    @Test
    void addNull() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        Assertions.assertFalse(collection.add(null));
    }

    @Test
    void testDefault() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        Assertions.assertEquals(BigDecimal.valueOf(50), collection.leastCommonMultipleInGHz());
    }

    @Test
    void addInteger() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        Assertions.assertTrue(collection.add(5));
        Assertions.assertFalse(collection.add(5));
    }

    @Test
    void addFloat() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        Assertions.assertTrue(collection.add(5.0));
        Assertions.assertFalse(collection.add(5.0));
    }

    @Test
    void addString() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        Assertions.assertTrue(collection.add(5.0));
        Assertions.assertFalse(collection.add(5.0));
    }

    @Test
    void immutable() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        collection.add(5);

        Set<BigDecimal> set = collection.set();
        Assertions.assertEquals(1, set.size());

        set.add(new BigDecimal(6));

        Assertions.assertEquals(1, collection.set().size());

    }

    @Test
    void leastCommonMultipleOnlyIntegersInGHz() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        collection.add(6);
        collection.add(12);
        collection.add(18);

        Assertions.assertEquals(BigDecimal.valueOf(36), collection.leastCommonMultipleInGHz());
    }

    @Test
    void leastCommonMultipleDecimalInGHz() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        collection.add(50.0);
        collection.add(6.25);
        collection.add(75);
        collection.add(12.5);

        Assertions.assertEquals(BigDecimal.valueOf(150), collection.leastCommonMultipleInGHz());
    }

    @Test
    void testSet() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        collection.add(50.0);
        collection.add(6.25);
        collection.add(75);
        collection.add(12.5);

        Set expected = new LinkedHashSet(
            List.of(
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(75)
            )
        );

        Assertions.assertEquals(expected, collection.set());
    }

    @Test
    void testSlots() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        collection.add(50.0);
        collection.add(BigDecimal.valueOf(6.25));
        collection.add(75);
        collection.add(12.5);

        Assertions.assertEquals(24, collection.slots(6.25));
    }

    @Test
    void testUnevenSlotsThrowsException() {
        Collection collection = new CenterFrequencyGranularityCollection(50);
        collection.add(6);
        collection.add(12);
        collection.add(18);

        Assertions.assertThrows(LeastCommonMultipleException.class,() -> collection.slots(6.25));
    }

}