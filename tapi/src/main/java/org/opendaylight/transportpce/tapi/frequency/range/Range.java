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
     * Add a range to the current range.
     *
     * <p>
     * It is up to the implementing class what to consider a valid range.
     * Meaning, how to handle the addition of a range that overlaps with
     * an existing range, or if the lower frequency is equal to the upper frequency.
     *
     * <p>
     * If the range already exists, it will not be added again and false may be returned.
     *
     * @return true if the range was added, false otherwise
     * @throws InvalidFrequencyRangeException if the range is invalid
     */
    boolean add(Frequency lowerInclusive, Frequency upperInclusive);

    /**
     * Add a range to the current range.
     *
     * <p>
     * It is up to the implementing class what to consider a valid range.
     * Meaning, how to handle the addition of a range that overlaps with
     * an existing range, or if the lower frequency is equal to the upper frequency.
     *
     * <p>
     * If the range already exists, it will not be added again and false may be returned.
     *
     * @return true if the range was added, false otherwise
     * @throws InvalidFrequencyRangeException if the range is invalid
     */
    boolean add(Double lower, Double upper);

    /**
     * Add a range to the current range.
     *
     * <p>
     * Any overlapping ranges found in range will be
     * ignored.
     *
     * @return true if the range was added, false otherwise
     */
    boolean add(Range range);

    /**
     * Add a range to the current range.
     *
     * <p>
     * The range is specified by a center frequency and a width and will be added
     * as a range from centerFrequencyTHz - widthGHz/2 to centerFrequencyTHz + widthGHz/2.
     *
     * @return true if the range was added, false otherwise
     */
    boolean addCenterFrequency(Double centerFrequencyTHz, Double widthGHz, Factory factory);

    /**
     * Return this range as a map of lower and upper frequencies.
     */
    Map<Frequency, Frequency> ranges();

    /**
     * Return this range as a map of lower and upper frequencies.
     */
    Map<Double, Double> rangesAsDouble();

    /**
     * Return this range as a map of lower and upper frequencies.
     */
    Map<Uint64, Uint64> rangesAsUint64();

}
