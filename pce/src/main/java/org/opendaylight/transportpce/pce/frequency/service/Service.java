/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.service;

import org.opendaylight.transportpce.pce.input.InvalidClientInputException;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;

public interface Service {

    /**
     * The nr of slots for a service. Each slot is assumed to be frequencyGranularityGHz in width.
     * Prioritizes the pathComputationRequestInput over spectralWidth.
     *
     * @param pathComputationRequestInput Api input, assumed to contain slot width = 2 x frequencyGranularityGHz.
     * @param spectralWidth the nr of slots required for a particular service type.
     *                      i.e. spectralWidth * frequencyGranularityGHz = required bandwidth.
     * @param frequencyGranularityGHz the frequency granularity to be used.
     * @return the nr of slots for a service.
     * @throws InvalidClientInputException if the pathComputationRequestInput contains invalid or conflicting data.
     */
    int slotWidth(
            PathComputationRequestInput pathComputationRequestInput,
            int spectralWidth,
            Double frequencyGranularityGHz
    );

    /**
     * Converts a frequency slot width of 12.5GHz to a slot width of frequencyGranularity.
     *
     * @param frequencySlot the frequency slot width to be converted. Defined as twice the frequencyGranularity.
     * @param frequencyGranularity the frequency granularity to convert to.
     * @throws InvalidClientInputException if the frequencyGranularity is not a valid value.
     */
    int slotWidth(FrequencySlot frequencySlot, Double frequencyGranularity);

}
