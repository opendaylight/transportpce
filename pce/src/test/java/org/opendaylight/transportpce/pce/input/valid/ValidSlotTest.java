/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ValidSlotTest {

    @Test
    void invalidCenterFrequency() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidCenterFrequency(
                BigDecimal.valueOf(192.126),
                observer
        ));

        Mockito.verify(observer).error(
                "Center frequency 192.126 (THz) is not evenly dividable by 6.25000 (GHz)");
    }

    @Test
    void invalidCenterFrequencyIsTooLow() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidCenterFrequency(
                BigDecimal.valueOf(190.325),
                observer
        ));

        Mockito.verify(observer).error(
                "Center frequency 190.325 (THz) is outside the range 191.325 - 195.0 (THz)");
    }

    @Test
    void invalidCenterFrequencyIsTooHigh() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidCenterFrequency(
                BigDecimal.valueOf(195.15),
                observer
        ));

        Mockito.verify(observer).error(
                "Center frequency 195.15 (THz) is outside the range 191.325 - 195.0 (THz)");
    }

    @Test
    void validCenterFrequency() {

        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertTrue(validSlot.isValidCenterFrequency(
                BigDecimal.valueOf(192.125),
                observer
        ));
    }

    @Test
    void anchorFrequencyIsValidCenterFrequency() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertTrue(validSlot.isValidCenterFrequency(
                BigDecimal.valueOf(193.1),
                observer
        ));
    }

    @Test
    void inValidSlotWidth() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidSlotWidth(
                BigDecimal.valueOf(30.0),
                observer
        ));
        Mockito.verify(observer).error("Slot width 30.0 (GHz) is not evenly dividable by 12.5 (GHz)");
    }


    @Test
    void entireRangeIsValid() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertTrue(validSlot.isValidSlot(
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(100.0),
                observer
        ));
    }

    @Test
    void entireRangeIsInValid() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidSlot(
                BigDecimal.valueOf(191.33),
                BigDecimal.valueOf(100.0),
                observer
        ));

        Mockito.verify(observer).error(
                "Center frequency 191.33 (THz) is not evenly dividable by 6.25000 (GHz)");
    }

    @Test
    void lowerFrequencyInRangeIsTooLow() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidSlot(
                BigDecimal.valueOf(191.425),
                BigDecimal.valueOf(300.0),
                observer
        ));

        Mockito.verify(observer).error(
                "Center frequency 191.425 (THz) with slot width 300.0 (GHz) "
                + "has a lower frequency outside the range 191.325-195.0");
    }

    @Test
    void upperFrequencyInRangeIsTooHigh() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidSlot(
                BigDecimal.valueOf(194.975),
                BigDecimal.valueOf(300.0),
                observer
        ));

        Mockito.verify(observer).error(
                "Center frequency 194.975 (THz) with slot width 300.0 (GHz) "
                + "has a higher frequency outside the range 191.325-195.0");
    }

    @Test
    void invalidSlotWidth() {
        Observer observer = Mockito.mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        Assertions.assertFalse(validSlot.isValidSlot(
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(99.0),
                observer
        ));

        Mockito.verify(observer).error("Slot width 99.0 (GHz) is not evenly dividable by 12.5 (GHz)");
    }
}
