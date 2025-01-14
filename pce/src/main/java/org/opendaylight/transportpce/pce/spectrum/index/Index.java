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
     *<p>Assuming the frequency range is divided into a grid of 6.25GHz wide chunks
     * and where the base frequency is 193.1 THz (G.694.1).
     * This method will find the first center frequency index relative to the base frequency index
     * suitable the nr of slots required for a service.</p>
     */
    int firstCenterFrequencyIndex(int centerFrequencySlotWidth, int baseFrequencySlotIndex, int serviceSlotWidth);

}
