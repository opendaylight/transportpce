/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

public interface RangeFactory {

    /**
     * Create an effective range base on a start frequency, granularity and effective bits.
     * The end frequency is calculated as startFrequency + (effectiveBits) * granularity.
     */
    Range effectiveRange(Double startFrequencyTHz, Double granularityGHz, int effectiveBits);

    Range range(Double centerFrequencyTHz, Double bandwidthGHz);

}
