/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.adapter;

import java.util.Map;
import org.opendaylight.transportpce.tapi.frequency.grid.Available;
import org.opendaylight.transportpce.tapi.frequency.grid.Numeric;

public class UsedFrequencyRange extends FrequencyRange {

    public UsedFrequencyRange(Numeric numeric) {
        super(numeric);
    }

    @Override
    public Map<Double, Double> frequency(Available frequency) {
        return numeric.assignedFrequency(frequency);
    }
}
