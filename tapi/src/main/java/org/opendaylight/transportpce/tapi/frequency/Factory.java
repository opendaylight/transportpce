/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;


public interface Factory {

    /**
     * Instantiate an Assigned object.
     *
     * <p>
     * Useful when the frequency range contains other values than 0 and 1.
     *
     * @param frequencyRange The range we're working with.
     * @param assignedFrequency The value interpreted as assigned frequency.
     * @param freeFrequency The value interpreted as free frequency.
     * @throws IllegalArgumentException If a value other than the assigned frequency or the free frequency
     *                                  is found in frequencyRange.
     */
    BitMap assigned(byte[] frequencyRange, byte assignedFrequency, byte freeFrequency);

    /**
     * Instantiate an Assigned object.
     *
     * <p>
     * Useful when the frequency range may contain more than two values,
     * but the assigned frequency value is known.
     *
     * <p>
     * Any value other than the assigned frequency is treated as free.
     *
     * @param frequencyRange The range we're working with.
     * @param assignedFrequency The value interpreted as assigned frequency.
     */
    BitMap assigned(byte[] frequencyRange, byte assignedFrequency);
}
