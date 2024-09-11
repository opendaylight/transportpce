/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.transportpce.tapi.frequency.TeraHertzFactory;

public class FrequencyRangeFactory implements RangeFactory {

    private final org.opendaylight.transportpce.tapi.frequency.Factory frequencyFactory;

    public FrequencyRangeFactory(org.opendaylight.transportpce.tapi.frequency.Factory frequencyFactory) {
        this.frequencyFactory = frequencyFactory;
    }

    public FrequencyRangeFactory() {
        this.frequencyFactory = new TeraHertzFactory();
    }

    @Override
    public Range effectiveRange(Double startFrequencyTHz, Double granularityGHz, int effectiveBits) {
        Range sortedRange = new SortedRange();

        Frequency upperFrequency = frequencyFactory.frequency(startFrequencyTHz, granularityGHz, effectiveBits);
        sortedRange.add(new TeraHertz(startFrequencyTHz), upperFrequency);

        return sortedRange;
    }

    @Override
    public Range range(Double centerFrequencyTHz, Double bandwidthGHz) {

        Range sortedRange = new SortedRange();

        Frequency lower = frequencyFactory.lower(centerFrequencyTHz, bandwidthGHz);
        Frequency upper = frequencyFactory.upper(centerFrequencyTHz, bandwidthGHz);

        sortedRange.add(lower, upper);

        return sortedRange;

    }
}
