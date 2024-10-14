/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;

public class ValidInput implements Valid {

    private final Slot slot;

    private String lastError = "";

    public ValidInput(Slot slot) {
        this.slot = slot;
    }

    @Override
    public boolean isValid(PathComputationRequestInput pathComputationRequestInput) {
        lastError = "";

        ServiceAEnd serviceAEnd = pathComputationRequestInput.getServiceAEnd();
        if (serviceAEnd != null) {
            ServiceAEnd1 serviceAEnd1 = serviceAEnd.augmentation(ServiceAEnd1.class);
            if (serviceAEnd1 != null) {
                if (!isValidSlot(serviceAEnd1.getFrequencySlot())) {
                    return false;
                }
            }
        }

        ServiceZEnd serviceZEnd = pathComputationRequestInput.getServiceZEnd();
        if (serviceZEnd != null) {
            ServiceZEnd1 serviceZEnd1 = serviceZEnd.augmentation(ServiceZEnd1.class);
            if (serviceZEnd1 != null) {
                if (!isValidSlot(serviceZEnd1.getFrequencySlot())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isValidSlot(FrequencySlot frequencySlot) {
        if (frequencySlot != null) {
            FrequencyGHz slotWidth = frequencySlot.getSlotWidth();
            FrequencyTHz centerFrequency = frequencySlot.getCenterFrequency();

            if (centerFrequency != null && slotWidth != null) {
                return slot.isValidSlot(
                        centerFrequency.getValue().decimalValue(),
                        slotWidth.getValue().decimalValue(),
                        this
                );
            } else if (centerFrequency != null) {
                return slot.isValidCenterFrequency(centerFrequency.getValue().decimalValue(), this);
            } else if (slotWidth != null) {
                return slot.isValidSlotWidth(slotWidth.getValue().decimalValue(), this);
            }
        }

        return true;
    }

    @Override
    public void error(String errorMessage) {
        lastError = errorMessage;
    }

    @Override
    public String lastErrorMessage() {
        return lastError;
    }
}
