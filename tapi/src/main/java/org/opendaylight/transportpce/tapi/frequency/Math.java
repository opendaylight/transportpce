/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

public interface Math {

    /**
     * Compute the start frequency in THz for the given index.
     *
     * @param index int
     * @return the start frequency in THz for the provided index.
     */
    Double getStartFrequencyFromIndex(int index);

    /**
     * Compute the stop frequency in THz for the given index.
     *
     * @param index int
     * @return the stop frequency in THz for the provided index.
     */
    Double getStopFrequencyFromIndex(int index);

}
