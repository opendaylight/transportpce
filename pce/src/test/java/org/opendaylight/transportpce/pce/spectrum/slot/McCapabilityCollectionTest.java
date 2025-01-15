/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;
import org.opendaylight.transportpce.pce.spectrum.observer.ThrowExceptionObserver;

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
    void assertExceptionIsThrown() {

        Observer observer = new ThrowExceptionObserver();
        CapabilityCollection slotCollection = new McCapabilityCollection(observer);

        McCapability mcCapabilityOne = mock(McCapability.class);
        double slotWidthGranularityGHz = 6.25;
        int slotCount = 8;
        when(mcCapabilityOne.isCompatibleWithServiceFrequency(
                BigDecimal.valueOf(slotWidthGranularityGHz)
                        .multiply(BigDecimal.valueOf(slotCount)), observer)).thenAnswer(
                                input -> {
                                    Observer observer1 = input.getArgument(1);
                                    observer1.error("Message");
                                    return false;
                                });

        assertTrue(slotCollection.add(mcCapabilityOne));

        assertThrows(RuntimeException.class,
                () -> slotCollection.isCompatibleService(slotWidthGranularityGHz, slotCount));

    }
}
