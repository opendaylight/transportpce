/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import java.util.Map;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.yangtools.yang.common.Uint64;

public interface Range {

    /**
     * Adds a frequency interval to this range set.
     *
     * <p>Implementations may normalize the stored ranges (e.g. merge overlapping
     * or adjacent intervals).
     *
     * @return {@code true} if this call changed the stored ranges, {@code false} if it was a no-op
     * @throws InvalidFrequencyRangeException if {@code lowerBound > upperBound}
     */
    boolean add(Frequency lowerBound, Frequency upperBound);

    /**
     * Adds a frequency interval (in THz) to this range set.
     *
     * <p>Implementations may normalize the stored ranges (e.g. merge overlapping
     * or adjacent intervals).
     *
     * @return {@code true} if this call changed the stored ranges, {@code false} if it was a no-op
     * @throws InvalidFrequencyRangeException if {@code lowerBound > upperBound}
     */
    boolean add(Double lowerBound, Double upperBound);

    /**
     * Adds all intervals from {@code range} into this range set.
     *
     * <p>The effect of overlaps/adjacency is implementation-defined (e.g. may merge).
     *
     * @return {@code true} if this call changed the stored ranges, {@code false} otherwise
     */
    boolean add(Range range);

    /**
     * Adds an interval specified by a center frequency and width.
     *
     * <p>The interval is added as
     * {@code [centerFrequencyTHz - widthGHz/2, centerFrequencyTHz + widthGHz/2]}.
     *
     * @return {@code true} if this call changed the stored ranges, {@code false} otherwise
     */
    boolean add(Double centerFrequencyTHz, Double widthGHz, Factory factory);

    /**
     * Return this range as a map of lower and upper frequencies.
     * The key is the lower frequency and the value is the upper frequency
     * in the range.
     */
    Map<Frequency, Frequency> ranges();

    /**
     * Return this range as a map of lower and upper frequencies in THz.
     * The key is the lower frequency and the value is the upper frequency
     * in the range.
     */
    Map<Double, Double> asTeraHertz();

    /**
     * Return this range as a map of lower and upper frequencies in Hz.
     * The key is the lower frequency and the value is the upper frequency
     * in the range.
     */
    Map<Uint64, Uint64> asHertz();

}
