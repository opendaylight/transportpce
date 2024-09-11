/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.grid;

import org.opendaylight.transportpce.common.fixedflex.GridUtils;

public class FrequencyMath implements Math {
    @Override
    public Double getStartFrequencyFromIndex(int index) {
        return GridUtils.getStartFrequencyFromIndex(index).doubleValue();
    }

    @Override
    public Double getStopFrequencyFromIndex(int index) {
        return GridUtils.getStopFrequencyFromIndex(index).doubleValue();
    }
}
