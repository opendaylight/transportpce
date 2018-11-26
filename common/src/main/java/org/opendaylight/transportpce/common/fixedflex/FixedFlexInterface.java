/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;
/**
 * <p>
 *     Mapping 2.2 devices to Fixed flex.
 * </p>
 **/

public interface FixedFlexInterface {
    /**
     * Calculates the center frequency, wavelength, start and stop for a wavelength number.
     *
     * @param index Wavelength number
     * @return Returns FixedFlexImp object with the calculated result.
     */
    FixedFlexImpl getFixedFlexWaveMapping(long index);

    double getCenterFrequency();

    double getStart();

    double getStop();

    double getWavelength();

    long getIndex();
}

