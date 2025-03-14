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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.Test;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;

public class InterfaceMcCapabilityTest {

    Observer observer = mock(Observer.class);

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
    public void slotWidthIsLessThanServiceWidthTwo() {
        McCapability slot = new InterfaceMcCapability(3.125, 1, 16);

        assertTrue(slot.isCompatibleWithServiceFrequency(50));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertTrue(slot.isCompatibleWithServiceFrequency(50, observer));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void slotWidthIsLessThanServiceWidthThree() {
        McCapability slot = new InterfaceMcCapability(4.6875, 1, 16);

        assertTrue(slot.isCompatibleWithServiceFrequency(37.5));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(37.5)));
        assertTrue(slot.isCompatibleWithServiceFrequency(37.5, observer));
        assertTrue(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(37.5), observer));
    }

    @Test
    public void incompatibleGranularityIsFalse() {
        McCapability slot = new InterfaceMcCapability(4.6875, 1, 16);

        assertFalse(slot.isCompatibleWithServiceFrequency(50));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertFalse(slot.isCompatibleWithServiceFrequency(50, observer));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void incompatibleGranularityIsFalseTwo() {
        McCapability slot = new InterfaceMcCapability(6.30, 1, 8);

        assertFalse(slot.isCompatibleWithServiceFrequency(50));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50)));
        assertFalse(slot.isCompatibleWithServiceFrequency(50, observer));
        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observer));
    }

    @Test
    public void incompatibleGranularityIsFalseThree() {
        McCapability slot = new InterfaceMcCapability(6.25, 1, 6);

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

        Observer observerMock = mock(Observer.class);

        assertFalse(slot.isCompatibleWithServiceFrequency(50, observerMock));

        verify(observerMock, times(1)).error(anyString());
    }

    @Test
    public void observerIsNotifiedMinSlotsEqualsMaxSlots() {
        McCapability slot = new InterfaceMcCapability("ROADM-A-SRG1", 100, 1, 1);

        Observer observerMock = mock(Observer.class);

        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observerMock));

        verify(observerMock, times(1)).error("ROADM-A-SRG1 does not support a service slot width of 50GHz (ROADM-A-SRG1"
                + " supports slot-width-granularity: 100GHz, and min-slots: 1, and max-slots 1, i.e. slot width:"
                + " 100GHz).");
    }

    @Test
    public void observerIsNotifiedMinSlotsNotEqualToMaxSlots() {
        McCapability slot = new InterfaceMcCapability("ROADM-A-SRG1", 12.5, 1, 3);

        Observer observerMock = mock(Observer.class);

        assertFalse(slot.isCompatibleWithServiceFrequency(BigDecimal.valueOf(50), observerMock));

        verify(observerMock, times(1)).error("ROADM-A-SRG1 does not support a service slot width of 50GHz (ROADM-A-SRG1"
                + " supports slot-width-granularity: 12.5GHz, and min-slots: 1, and max-slots 3, i.e. slot width:"
                + " 12.5GHz to 37.5GHz).");
    }
}
