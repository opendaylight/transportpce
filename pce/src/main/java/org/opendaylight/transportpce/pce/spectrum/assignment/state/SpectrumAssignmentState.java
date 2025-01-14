/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment.state;

import org.opendaylight.transportpce.pce.spectrum.assignment.Assign;
import org.opendaylight.transportpce.pce.spectrum.assignment.AssignSpectrumHighToLow;
import org.opendaylight.transportpce.pce.spectrum.assignment.AssignSpectrumLowToHigh;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpectrumAssignmentState implements State {

    private static final Logger LOG = LoggerFactory.getLogger(SpectrumAssignmentState.class);

    public static final String HIGH_TO_LOW_FREQUENCY = "HIGH_TO_LOW_FREQUENCY";

    public static final String LOW_TO_HIGH_FREQUENCY = "LOW_TO_HIGH_FREQUENCY";

    @Override
    public Assign configuredState(String configuration) {

        if (configuration == null || configuration.isEmpty()) {
            LOG.info("Choosing spectrum assignment strategy {} (searching from high frequency to low frequency).",
                     HIGH_TO_LOW_FREQUENCY);
            return new AssignSpectrumHighToLow(new SpectrumIndex());
        }

        if (!configuration.equalsIgnoreCase(LOW_TO_HIGH_FREQUENCY)
                && !configuration.equalsIgnoreCase(HIGH_TO_LOW_FREQUENCY)) {

            LOG.warn("Unknown configuration setting '{}', expected either {} or {}.",
                    configuration, HIGH_TO_LOW_FREQUENCY, LOW_TO_HIGH_FREQUENCY);

            LOG.info("Choosing spectrum assignment strategy {} (searching from high frequency to low frequency).",
                    HIGH_TO_LOW_FREQUENCY);

            return new AssignSpectrumHighToLow(new SpectrumIndex());
        }

        if (configuration.equalsIgnoreCase(LOW_TO_HIGH_FREQUENCY)) {
            LOG.info("Choosing spectrum assignment strategy {} (searching from low frequency to high frequency).",
                    LOW_TO_HIGH_FREQUENCY);
            return new AssignSpectrumLowToHigh(new SpectrumIndex());
        }

        LOG.info("Choosing spectrum assignment strategy {} (searching from high frequency to low frequency).",
                HIGH_TO_LOW_FREQUENCY);
        return new AssignSpectrumHighToLow(new SpectrumIndex());

    }
}
