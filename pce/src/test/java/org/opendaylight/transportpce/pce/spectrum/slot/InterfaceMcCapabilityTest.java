/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;

public class InterfaceMcCapabilityTest {

    Observer observer = Mockito.mock(Observer.class);

    @Test
    public void slotWidthEqualToServiceWidth() {
        McCapability slot = new InterfaceMcCapability(50, 1, 1);

        assertTrue(slot.isCompatibleWithServiceFrequency(50));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertTrue(slot.isCompatibleWithServiceFrequency(50, observer));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void slotWidthIsLessThanServiceWidth() {
        McCapability slot = new InterfaceMcCapability(6.25, 1, 8);

        assertTrue(slot.isCompatibleWithServiceFrequency(50));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertTrue(slot.isCompatibleWithServiceFrequency(50, observer));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void incompatibleGranularityIsFalse() {
        McCapability slot = new InterfaceMcCapability(6.30, 1, 8);

        assertFalse(slot.isCompatibleWithServiceFrequency(50));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertFalse(slot.isCompatibleWithServiceFrequency(50, observer));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void minSlotIsTooHigh() {
        McCapability slot = new InterfaceMcCapability(6.25, 8, 16);

        assertFalse(slot.isCompatibleWithServiceFrequency(37.5));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(37.5)));
        assertFalse(slot.isCompatibleWithServiceFrequency(37.5, observer));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(37.5), observer));
    }

    @Test
    public void slotWidthGranularityIsTooHigh() {
        McCapability slot = new InterfaceMcCapability(100, 1, 1);

        assertFalse(slot.isCompatibleWithServiceFrequency(50));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertFalse(slot.isCompatibleWithServiceFrequency(50, observer));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void testUnknownNodesEquals() {
        McCapability slotOne = new InterfaceMcCapability(50, 1, 1);
        McCapability slotTwo = new InterfaceMcCapability(50, 1, 1);

        assertTrue(slotOne.equals(slotTwo));
    }

    @Test
    public void testKnownNodesEquals() {
        McCapability slotOne = new InterfaceMcCapability("A", 50, 1, 1);
        McCapability slotTwo = new InterfaceMcCapability("A", 50, 1, 1);

        assertTrue(slotOne.equals(slotTwo));
    }

    @Test
    public void testKnownNodesNotEquals() {
        McCapability slotOne = new InterfaceMcCapability("A", 50, 1, 1);
        McCapability slotTwo = new InterfaceMcCapability("B", 50, 1, 1);

        assertFalse(slotOne.equals(slotTwo));
    }

    @Test
    public void observerIsNotifiedFrequencyAsDoubleValue() {
        McCapability slot = new InterfaceMcCapability(100, 1, 1);

        Observer observerMock = Mockito.mock(Observer.class);

        assertFalse(slot.isCompatibleWithServiceFrequency(50, observerMock));

        Mockito.verify(observerMock, Mockito.times(1)).error(Mockito.anyString());
    }


    @Test
    public void observerIsNotifiedFrequencyAsBigDecimalValue() {
        McCapability slot = new InterfaceMcCapability(100, 1, 1);

        Observer observerMock = Mockito.mock(Observer.class);

        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observerMock));

        Mockito.verify(observerMock, Mockito.times(1)).error(Mockito.anyString());
    }
}
