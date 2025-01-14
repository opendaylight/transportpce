/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.index;

public interface Base {

    /**
     * Calculate a reference frequency relative index nr.
     *
     * <p>Assuming a spectrum grid, divided into chunks with a frequency width of frequencyGranularityGHz and where
     * the lowest frequency on the grid is edge edgeFrequencyTHz, this method will return the index of
     * referenceFrequencyTHz.</p>
     *
     * <p>Note: (referenceFrequencyTHz - referenceFrequencyTHz) mod frequencyGranularityGHz = 0</p>
     * @throws NoIndexFoundException if the reference index cannot be found on the spectrum grid.
     */
    int referenceFrequencySpectrumIndex(
        double referenceFrequencyTHz, double edgeFrequencyTHz, double frequencyGranularityGHz);

}
