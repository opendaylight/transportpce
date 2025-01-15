/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;

public interface McCapability {

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     */
    boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     * The observer is notified about errors.
     */
    boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     *
     * @see McCapability#isCompatibleWithServiceFrequency(BigDecimal)
     */
    boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     * The observer is notified about errors.
     *
     * @see McCapability#isCompatibleWithServiceFrequency(BigDecimal)
     * @see McCapability#isCompatibleWithServiceFrequency(BigDecimal, Observer)
     */
    boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz, Observer observer);
}
