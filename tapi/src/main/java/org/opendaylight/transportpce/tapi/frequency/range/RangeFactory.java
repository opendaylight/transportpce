/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import java.util.Map;

public interface RangeFactory {


    /**
     * Create an effective range (i.e. the entire range) based on a start frequency, granularity
     * and effective bits.
     * The end frequency is calculated as startFrequency + (effectiveBits * granularity).
     */
    Range effectiveRange(Double startFrequencyTHz, Double granularityGHz, int effectiveBits);

    /**
     * Create a range based on a center frequency and a bandwidth.
     * The range is calculated as centerFrequency - (bandwidth/2) to centerFrequency + (bandwidth/2).
     */
    Range range(Double centerFrequencyTHz, Double bandwidthGHz);

    /**
     * Create a range based on a map of lower and upper frequencies.
     * The map should contain entries where the key is the lower frequency and the value is the upper frequency.
     */
    Range range(Map<Double, Double> ranges);

}
