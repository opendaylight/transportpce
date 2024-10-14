/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencySlot;

public class ValidFormat implements Format {

    @Override
    public boolean isValidFormat(String format, FrequencySlot frequencySlot, Observer observer) {
        if (frequencySlot == null
                || (frequencySlot.getSlotWidth() == null && frequencySlot.getCenterFrequency() == null)) {
            return true;
        }

        if (format != null && format.equals("other")
                || (frequencySlot.getSlotWidth() == null && frequencySlot.getCenterFrequency() == null)) {
            return true;
        }

        observer.error(String.format("Service format %s does not support manually setting slot-width.", format));
        return false;
    }
}
