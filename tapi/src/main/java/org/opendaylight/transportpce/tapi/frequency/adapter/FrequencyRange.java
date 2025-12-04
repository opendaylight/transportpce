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

public abstract class FrequencyRange {

    protected final Numeric numeric;

    FrequencyRange(Numeric numeric) {
        this.numeric = numeric;
    }

    /**
     * Return a map of ranges based on available frequency ranges.
     * The implementing class is responsible for what range is actually
     * returned.
     *
     * <p>The key is the lower frequency bound and the value is the upper frequency bound.
     */
    public abstract Map<Double, Double> frequency(Available frequency);

}
