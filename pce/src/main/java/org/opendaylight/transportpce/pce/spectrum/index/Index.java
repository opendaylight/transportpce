/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.index;

public interface Index {

    /**
     * Calculates the first center frequency index relative to baseFrequencySlotIndex.
     *
     * <p></p>
     */
    int firstCenterFrequencyIndex(int centerFrequencySlotWidth, int baseFrequencySlotIndex, int serviceSlotWidth);

}
