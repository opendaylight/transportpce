/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;


public class AssignedGridFactory implements Factory {

    @Override
    public Assigned fromAvailable(byte[] frequencyRange) {
        byte[] frequencyRangeCopy = new byte[frequencyRange.length];

        for (int i = 0; i < frequencyRange.length; i++) {
            frequencyRangeCopy[i] = (byte) ~frequencyRange[i];
        }

        return new AssignedGrid(frequencyRangeCopy);
    }

    @Override
    public Assigned fromAssigned(byte[] frequencyRange) {
        return new AssignedGrid(frequencyRange);
    }

}
