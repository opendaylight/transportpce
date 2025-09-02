/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SlotWidthFrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlotBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev250110.FrequencyTHz;

public class ValidFormatTest {

    @Test
    public void nullIsValid() {
        Format validFormat = new ValidFormat();
        assertTrue(validFormat.isValidFormat(null, null, mock(Observer.class)));
    }

    @Test
    public void nullFrequencySlotIsValid() {
        Format validFormat = new ValidFormat();
        assertTrue(validFormat.isValidFormat("OTU", null, mock(Observer.class)));
    }

    @Test
    public void emptyFrequencySlotIsValid() {
        Format validFormat = new ValidFormat();
        assertTrue(validFormat.isValidFormat("OTU", new FrequencySlotBuilder().build(), mock(Observer.class)));
    }

    @Test
    public void centerFrequencyAndOTUServiceIsNotValid() {
        Format validFormat = new ValidFormat();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("193.1"))
                .build();
        assertFalse(validFormat.isValidFormat("OTU", frequencySlot, mock(Observer.class)));
    }

    @Test
    public void centerFrequencyAndNullServiceIsNotValid() {
        Format validFormat = new ValidFormat();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("193.1"))
                .build();
        assertFalse(validFormat.isValidFormat(null, frequencySlot, mock(Observer.class)));
    }

    @Test
    public void slotWidthAndOTUServiceIsNotValid() {
        Format validFormat = new ValidFormat();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        assertFalse(validFormat.isValidFormat("OTU", frequencySlot, mock(Observer.class)));
    }

    @Test
    public void centerFrequencyAndOtherServiceIsValid() {
        Format validFormat = new ValidFormat();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("193.1"))
                .build();
        assertTrue(validFormat.isValidFormat("other", frequencySlot, mock(Observer.class)));
    }

    @Test
    public void slotWidthAndOtherServiceIsValid() {
        Format validFormat = new ValidFormat();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        assertTrue(validFormat.isValidFormat("other", frequencySlot, mock(Observer.class)));
    }

    @Test
    public void errorMessage() {
        Observer observer = mock(Observer.class);
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        Format validFormat = new ValidFormat();
        assertFalse(validFormat.isValidFormat("OTU", frequencySlot, observer));
        verify(observer).error("Service format OTU does not support manually setting slot-width.");

    }
}