/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.grid;


public class AvailableGridFactory implements Factory {

    @Override
    public Available fromAvailable(byte[] frequencyRange) {
        return new AvailableGrid(frequencyRange);
    }

    @Override
    public Available fromAssigned(byte[] frequencyRange) {
        byte[] frequencyRangeCopy = new byte[frequencyRange.length];

        for (int i = 0; i < frequencyRange.length; i++) {
            frequencyRangeCopy[i] = (byte) ~frequencyRange[i];
        }

        return new AvailableGrid(frequencyRangeCopy);
    }

}
