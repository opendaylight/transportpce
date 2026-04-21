/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SlotWidthFrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev250110.FrequencyTHz;

class ValidSlotTest {

    @Test
    void invalidCenterFrequency() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidCenterFrequency(BigDecimal.valueOf(192.126), observer));

        verify(observer).error(
                "Center frequency 192.126 (THz) is not evenly dividable by 6.25000 (GHz)");
    }

    @Test
    void invalidCenterFrequencyIsTooLow() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidCenterFrequency(BigDecimal.valueOf(190.325), observer));

        verify(observer).error(
                "Center frequency 190.325 (THz) is outside the range 191.325 - 195.0 (THz)");
    }

    @Test
    void invalidCenterFrequencyIsTooHigh() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidCenterFrequency(BigDecimal.valueOf(195.15), observer));

        verify(observer).error(
                "Center frequency 195.15 (THz) is outside the range 191.325 - 195.0 (THz)");
    }

    @Test
    void validCenterFrequency() {

        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertTrue(validSlot.isValidCenterFrequency(BigDecimal.valueOf(192.125), observer));
    }

    @Test
    void anchorFrequencyIsValidCenterFrequency() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertTrue(validSlot.isValidCenterFrequency(BigDecimal.valueOf(193.1), observer));
    }

    @Test
    void inValidSlotWidth() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidSlotWidth(BigDecimal.valueOf(30.0), observer));
        verify(observer).error("Slot width 30.0 (GHz) is not evenly dividable by 12.5 (GHz)");
    }


    @Test
    void entireRangeIsValid() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertTrue(validSlot.isValidSlot(BigDecimal.valueOf(193.1), BigDecimal.valueOf(100.0), observer));
    }

    @Test
    void entireRangeIsInValid() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidSlot(BigDecimal.valueOf(191.33), BigDecimal.valueOf(100.0), observer));

        verify(observer).error(
                "Center frequency 191.33 (THz) is not evenly dividable by 6.25000 (GHz)");
    }

    @Test
    void lowerFrequencyInRangeIsTooLow() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidSlot(BigDecimal.valueOf(191.425), BigDecimal.valueOf(300.0), observer));

        verify(observer).error(
                "Center frequency 191.425 (THz) with slot width 300.0 (GHz) "
                + "has a lower frequency outside the range 191.325-195.0");
    }

    @Test
    void upperFrequencyInRangeIsTooHigh() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidSlot(BigDecimal.valueOf(194.975), BigDecimal.valueOf(300.0), observer));

        verify(observer).error(
                "Center frequency 194.975 (THz) with slot width 300.0 (GHz) "
                + "has a higher frequency outside the range 191.325-195.0");
    }

    @Test
    void invalidSlotWidth() {
        Observer observer = mock(Observer.class);

        ValidSlot validSlot = new ValidSlot(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(193.1),
                BigDecimal.valueOf(195.0),
                BigDecimal.valueOf(6.25),
                BigDecimal.valueOf(12.5)
        );

        assertFalse(validSlot.isValidSlot(BigDecimal.valueOf(193.1), BigDecimal.valueOf(99.0), observer));

        verify(observer).error("Slot width 99.0 (GHz) is not evenly dividable by 12.5 (GHz)");
    }

    @ParameterizedTest
    @CsvSource({
        "191.350,50,191.350000000,50",
        "191.350,25,191.350,25.00000",
        "191.375,50,191.375000000,50.00000"
    })
    void equivalentRepresentationsProduceSameValidationResult(
            String center1,
            String width1,
            String center2,
            String width2) {

        ValidSlot slot = createValidSlot();

        RecordingObserver observer1 = new RecordingObserver();
        RecordingObserver observer2 = new RecordingObserver();

        boolean result1 = slot.isValidSlot(
                new BigDecimal(center1),
                new BigDecimal(width1),
                observer1);

        boolean result2 = slot.isValidSlot(
                new BigDecimal(center2),
                new BigDecimal(width2),
                observer2);

        assertEquals(result1, result2);
        assertEquals(observer1.lastErrorMessage(), observer2.lastErrorMessage());
        assertTrue(result1, observer1.lastErrorMessage());
        assertTrue(result2, observer2.lastErrorMessage());
        assertEquals(observer1.lastErrorMessage(), observer2.lastErrorMessage());
    }

    @Test
    void generatedWrapperValuesValidateCorrectly() {
        ValidSlot slot = createValidSlot();
        RecordingObserver observer = new RecordingObserver();

        boolean result = slot.isValidSlot(
                FrequencyTHz.getDefaultInstance("191.37500000").getValue().decimalValue(),
                SlotWidthFrequencyGHz.getDefaultInstance("50.00000").getValue().decimalValue(),
                observer);

        assertTrue(result, observer.lastErrorMessage());
        assertEquals("", observer.lastErrorMessage());
    }

    @Test
    void generatedWrapperValuesProduceInvalidSlot() {
        ValidSlot slot = createValidSlot();
        RecordingObserver observer = new RecordingObserver();

        boolean result = slot.isValidSlot(
                FrequencyTHz.getDefaultInstance("191.32500000").getValue().decimalValue(),
                SlotWidthFrequencyGHz.getDefaultInstance("100.00000").getValue().decimalValue(),
                observer);

        assertFalse(result, observer.lastErrorMessage());
        assertEquals("Center frequency 191.32500000 (THz) with slot width 100.00000 (GHz) "
                + "has a lower frequency outside the range 191.325-196.12500", observer.lastErrorMessage());
    }

    private static ValidSlot createValidSlot() {
        BigDecimal lowerEdgeFrequency = BigDecimal.valueOf(GridConstant.START_EDGE_FREQUENCY_THZ);
        BigDecimal anchorFrequencyTHz = BigDecimal.valueOf(GridConstant.CENTRAL_FREQUENCY_THZ);
        BigDecimal upperEdgeFrequency = lowerEdgeFrequency.add(
                BigDecimal.valueOf(GridConstant.GRANULARITY)
                        .multiply(BigDecimal.valueOf(GridConstant.EFFECTIVE_BITS))
                        .multiply(BigDecimal.valueOf(0.001)));

        return new ValidSlot(
                lowerEdgeFrequency,
                anchorFrequencyTHz,
                upperEdgeFrequency,
                BigDecimal.valueOf(GridConstant.GRANULARITY),
                BigDecimal.valueOf(12.5)
        );
    }

    private static final class RecordingObserver implements Observer {
        private String lastError = "";

        @Override
        public void error(String errorMessage) {
            lastError = errorMessage;
        }

        String lastErrorMessage() {
            return lastError;
        }
    }
}
