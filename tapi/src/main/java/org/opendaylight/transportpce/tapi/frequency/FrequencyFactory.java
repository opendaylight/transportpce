/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

public class FrequencyFactory implements Factory {

    private final byte createAssignedFrequency;
    private final byte createFreeFrequency;

    /**
     * The parameters to this constructor is used when creating an object and assigning
     * values indicating a used or free frequency.
     *
     * @param assignedFrequency The value used by this factory to represent assigned frequency
     * @param freeFrequency The value used by this factory to represent free frequency
     */
    public FrequencyFactory(byte assignedFrequency, byte freeFrequency) {
        this.createAssignedFrequency = assignedFrequency;
        this.createFreeFrequency = freeFrequency;
    }

    /**
     * Is the same as calling AssignedFrequencyFactory((byte) 1, (byte) 0).
     * Meaning 1 is used to represent assigned frequency and 0 is used to represent free frequency.
     */
    public FrequencyFactory() {
        this((byte) 1, (byte) 0);
    }

    @Override
    public BitMap assigned(byte[] frequencyRange, byte assignedFrequency, byte freeFrequency) {

        if (frequencyRange == null || frequencyRange.length == 0) {
            throw new IllegalArgumentException("Frequency range cannot be null or empty");
        }

        byte[] range = new byte[frequencyRange.length];

        for (int i = 0; i < frequencyRange.length;i++) {

            if (frequencyRange[i] == assignedFrequency) {
                range[i] = createAssignedFrequency;
            } else if (frequencyRange[i] == freeFrequency) {
                range[i] = createFreeFrequency;
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Invalid frequency range representation value. Expected either %s or %s but got %s",
                                assignedFrequency,
                                freeFrequency,
                                frequencyRange[i]
                        )
                );
            }
        }

        return new BitMapFrequencies(range);
    }

    @Override
    public BitMap assigned(byte[] frequencyRange, byte assignedFrequency) {

        if (frequencyRange == null || frequencyRange.length == 0) {
            throw new IllegalArgumentException("Frequency range cannot be null or empty");
        }

        byte[] range = new byte[frequencyRange.length];

        for (int i = 0; i < frequencyRange.length;i++) {
            if (frequencyRange[i] == assignedFrequency) {
                range[i] = createAssignedFrequency;
            } else {
                range[i] = createFreeFrequency;
            }
        }

        return new BitMapFrequencies(range);
    }
}
