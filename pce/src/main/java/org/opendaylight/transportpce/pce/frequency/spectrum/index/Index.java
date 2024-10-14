/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;

import org.opendaylight.yangtools.yang.common.Decimal64;

public interface Index {

    /**
     * Find the slot nr for a frequency.
     */
    int index(Decimal64 frequency);

    /**
     * Find the slot nr for a frequency based on the nr of effective bits in
     * the spectrum, frequency width for each slot and the edge frequency.
     */
    int index(Double startFrequency, Double widthGHz, int effectiveBits, Decimal64 frequency);

}
