/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceAEnd1;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceZEnd1;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;

public class ServiceFrequency implements Service {

    @Override
    public int slotWidth(PathComputationRequestInput input, int spectralWidth, Double frequencyGranularityGHz) {

        Set<Integer> slotWidths = new HashSet<>();

        ServiceAEnd serviceAEnd = input.getServiceAEnd();
        if (serviceAEnd != null) {
            ServiceAEnd1 augmentation = serviceAEnd.augmentation(ServiceAEnd1.class);

            if (augmentation != null) {
                FrequencySlot frequencySlot = augmentation.getFrequencySlot();
                if (frequencySlot != null && frequencySlot.getSlotWidth() != null) {
                    slotWidths.add(slotWidth(frequencySlot, frequencyGranularityGHz));
                }
            }
        }

        ServiceZEnd serviceZEnd = input.getServiceZEnd();
        if (serviceZEnd != null) {
            ServiceZEnd1 augmentation = serviceZEnd.augmentation(ServiceZEnd1.class);

            if (augmentation != null) {
                FrequencySlot frequencySlot = augmentation.getFrequencySlot();
                if (frequencySlot != null && frequencySlot.getSlotWidth() != null) {
                    slotWidths.add(slotWidth(frequencySlot, frequencyGranularityGHz));
                }
            }

        }

        if (slotWidths.isEmpty()) {
            return spectralWidth;
        }

        if (slotWidths.size() > 1) {
            throw new InvalidClientInputException(
                    String.format(
                            "Can not process conflicting slot width values: %s, data from client %s",
                            slotWidths,
                            input
                    )
            );
        }

        return slotWidths.stream().findFirst().orElseThrow(() -> new InvalidClientInputException(
                String.format("No slot width information provided %s", input)
        ));
    }

    @Override
    public int slotWidth(FrequencySlot frequencySlot, Double frequencyGranularity) {
        if (frequencySlot == null || frequencySlot.getSlotWidth() == null) {
            throw new InvalidClientInputException("No frequency slot information provided");
        }

        BigDecimal inputFrequencyWidth = BigDecimal.valueOf(frequencySlot.getSlotWidth().intValue())
                .multiply(BigDecimal.valueOf(12.5));

        if (inputFrequencyWidth.remainder(BigDecimal.valueOf(frequencyGranularity)).compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidClientInputException(
                    String.format(
                            "Frequency slot width %s is not a multiple of frequency granularity %s",
                            frequencySlot.getSlotWidth(),
                            frequencyGranularity
                    )
            );
        }

        return inputFrequencyWidth.divide(BigDecimal.valueOf(frequencyGranularity)).intValue();
    }
}
