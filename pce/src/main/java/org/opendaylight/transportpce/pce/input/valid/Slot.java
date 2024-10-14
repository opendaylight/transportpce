/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import java.math.BigDecimal;

public interface Slot {

    /**
     * Validate the center frequency.
     * The implementing class is free to make up its own rules as to what
     * is considered a valid center frequency.
     *
     * @param centerFrequencyTHz The center frequency.
     * @param observer The observer is notified about any errors found.
     * @return true if this is a valid center frequency.
     */
    boolean isValidCenterFrequency(BigDecimal centerFrequencyTHz, Observer observer);

    /**
     * Validate the slot width.
     *
     * @param slotWidthGHz Slot width frequency.
     * @param observer The observer is notified about any errors found.
     * @return true is slotWidthGHz is a valid frequency slot width.
     */
    boolean isValidSlotWidth(BigDecimal slotWidthGHz, Observer observer);

    /**
     * Validate the range.
     * Validates the center frequency, slot width and the resulting range.
     *
     * @param centerFrequencyTHz The center frequency.
     * @param slotWidthGHz The entire frequency range centered on centerFrequencyTHz.
     * @param observer The observer is notified about any errors found.
     * @return true is the center frequency and slot frequency width is a valid range.
     */
    boolean isValidSlot(BigDecimal centerFrequencyTHz, BigDecimal slotWidthGHz, Observer observer);

}
