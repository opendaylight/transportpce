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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SlotWidthFrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;

public class ValidInput implements Valid {

    private final Slot slot;

    private final Format format;

    private String lastError = "";

    public ValidInput(Slot slot, Format format) {
        this.slot = slot;
        this.format = format;
    }

    @Override
    public boolean isValid(PathComputationRequestInput pathComputationRequestInput) {
        lastError = "";

        ServiceAEnd serviceAEnd = pathComputationRequestInput.getServiceAEnd();
        if (serviceAEnd != null) {
            ServiceAEnd1 serviceAEnd1 = serviceAEnd.augmentation(ServiceAEnd1.class);
            if (serviceAEnd1 != null) {

                ServiceFormat serviceAEndFormat = serviceAEnd.getServiceFormat();
                String serviceFormat = null;
                if (serviceAEndFormat != null) {
                    serviceFormat = serviceAEndFormat.getName();
                }
                if (!format.isValidFormat(serviceFormat, serviceAEnd1.getFrequencySlot(), this)) {
                    return false;
                }

                if (!isValidSlot(serviceAEnd1.getFrequencySlot())) {
                    return false;
                }
            }
        }

        ServiceZEnd serviceZEnd = pathComputationRequestInput.getServiceZEnd();
        if (serviceZEnd != null) {
            ServiceZEnd1 serviceZEnd1 = serviceZEnd.augmentation(ServiceZEnd1.class);
            if (serviceZEnd1 != null) {

                ServiceFormat serviceZEndFormat = serviceZEnd.getServiceFormat();
                String serviceFormat = null;
                if (serviceZEndFormat != null) {
                    serviceFormat = serviceZEndFormat.getName();
                }
                if (!format.isValidFormat(serviceFormat, serviceZEnd1.getFrequencySlot(), this)) {
                    return false;
                }

                if (!isValidSlot(serviceZEnd1.getFrequencySlot())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isValidSlot(FrequencySlot frequencySlot) {
        if (frequencySlot != null) {
            SlotWidthFrequencyGHz slotWidth = frequencySlot.getSlotWidth();
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
