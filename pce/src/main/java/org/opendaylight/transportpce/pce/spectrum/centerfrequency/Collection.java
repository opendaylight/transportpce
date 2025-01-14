/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.centerfrequency;

import java.math.BigDecimal;
import java.util.Set;

public interface Collection {

    /**
     * Add a center frequency to this collection.
     * @param centerFrequencyGranularityGHz A center frequency, e.g. for a node.
     */
    boolean add(BigDecimal centerFrequencyGranularityGHz);

    /**
     * Add a center frequency to this collection.
     * @param centerFrequencyGranularityGHz A center frequency, e.g. for a node.
     */
    boolean add(float centerFrequencyGranularityGHz);

    /**
     * Add a center frequency to this collection.
     * @param centerFrequencyGranularityGHz A center frequency, e.g. for a node.
     */
    boolean add(double centerFrequencyGranularityGHz);

    /**
     * A unique list of frequency granularities in this collection.
     */
    Set<BigDecimal> set();

    /**
     * Finds the least common denominator of all center frequency granularities in this collection.
     * @throws LeastCommonMultiplierException if no least common denominator is possible.
     */
    BigDecimal leastCommonMultiplierInGHz();

    /**
     * The nr of slots with a width of frequencyGranularityGHz this collection will occupy.
     *
     * <p>Calculates the least common multiplier for this collection of frequency granularities
     * and then calculates how many slots it will occupy.</p>
     * @throws LeastCommonMultiplierException In case this collection is empty or if the lcm is not evenly dividable
     *                                        by frequencyGranularityGHz.
     */
    int slots(BigDecimal frequencyGranularityGHz);

    /**
     * The nr of slots with a width of frequencyGranularityGHz this collection will occupy.
     *
     * @see Collection#slots(BigDecimal)
     */
    int slots(double frequencyGranularityGHz);

}
