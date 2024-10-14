/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

public interface Factory {

    /**
     * Instantiate an object suitable to input validation.
     *
     * @param lowerEdgeFrequencyTHz The lowest frequency in the frequency grid (effectiveBits)
     * @param anchorFrequencyTHz The frequency grid anchor frequency.
     * @param centerFrequencyGranularityGHz Each center frequency is separated by this range.
     * @param slotGranularityGHz Service input frequency range need to be dividable by this range. Typically, this value
     *                           is centerFrequencyGranularityGHz x 2.
     * @param effectiveBits The nr of effective bits.
     */
    Valid instantiate(
            double lowerEdgeFrequencyTHz,
            double anchorFrequencyTHz,
            double centerFrequencyGranularityGHz,
            double slotGranularityGHz,
            int effectiveBits);

}
