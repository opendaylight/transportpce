/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.input.InvalidClientInputException;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd1Builder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd1Builder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SlotWidthFrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlotBuilder;

class ServiceFrequencyTest {

    @Test
    void emptyServiceInformationReturnsDefaultSpectralWidth() {
        Service serviceFrequency = new ServiceFrequency();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder().build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder().build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

        assertEquals(16, serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void emptyRxInformationReturnsDefaultSpectralWidth() {
        Service serviceFrequency = new ServiceFrequency();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder().build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder().build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

        assertEquals(16, serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void emptyAugmentationServiceAEendInformationReturnsDefaultSpectralWidth() {
        Service serviceFrequency = new ServiceFrequency();
        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder().build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder().build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

        assertEquals(16, serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void slotServiceA() {
        Service serviceFrequency = new ServiceFrequency();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder().build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

        assertEquals(8, serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void slotServiceZ() {
        Service serviceFrequency = new ServiceFrequency();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceZEnd(serviceZEnd)
                .build();

        assertEquals(8, serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void theSameSlotWidthSpecifiedInMultipleLocations() {
        Service serviceFrequency = new ServiceFrequency();
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

        assertEquals(8, serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void differentSlotWidthSpecifiedInMultipleLocationsThrowException() {
        Service serviceFrequency = new ServiceFrequency();
        FrequencySlot frequencySlot1 = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();
        FrequencySlot frequencySlot2 = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("62.5"))
                .build();
        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlot1)
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();
        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot2)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInput pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

        assertThrows(
                InvalidClientInputException.class,
                () -> serviceFrequency.slotWidth(pathComputationRequestInput, 16, 6.25));
    }

    @Test
    void inputSlotWidthFiftyIsEqualToSixPointTwentyFiveTimesEight() {
        Service serviceFrequency = new ServiceFrequency();
        FrequencySlot frequencySlot2 = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("50"))
                .build();

        assertEquals(8, serviceFrequency.slotWidth(frequencySlot2, 6.25));
    }

    @Test
    void assertGranularityNotDividableByTwelvePointFiveThrowsException() {
        Service serviceFrequency = new ServiceFrequency();
        FrequencySlot frequencySlot2 = new FrequencySlotBuilder()
                .setSlotWidth(SlotWidthFrequencyGHz.getDefaultInstance("55"))
                .build();

        assertThrows(
                InvalidClientInputException.class,
                () -> serviceFrequency.slotWidth(frequencySlot2, 5.0));
    }
}
