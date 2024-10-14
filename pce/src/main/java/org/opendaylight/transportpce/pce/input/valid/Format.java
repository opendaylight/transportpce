/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;

public interface Format {

    /**
     * Validates if the serviceFormat and frequencySlot is a valid combination.
     *
     * @param serviceFormat e.g. ODU, OTU, other etc.
     * @param frequencySlot Center and slot-width frequency.
     * @param observer Will be notified about any validation error.
     * @return true if the serviceFormat and frequencySlot is a valid combination.
     */
    boolean isValidFormat(String serviceFormat, FrequencySlot frequencySlot, Observer observer);

}
