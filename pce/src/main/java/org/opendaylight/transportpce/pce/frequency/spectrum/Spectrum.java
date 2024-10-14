/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum;

import java.math.BigDecimal;
import java.util.BitSet;
import org.eclipse.jdt.annotation.NonNull;

public interface Spectrum {

    /**
     * Search for a bitset in another bitset.
     *
     * @param needle   Search for this needle
     * @param haystack The haystack of bits
     * @return true if all bits in needle are found in haystack
     */
    boolean isSubset(BitSet needle, BitSet haystack);

    /**
     * Create a BitSet representing the frequencies occupied by a frequency range.
     *
     * <p>
     * Example:
     * frequencySlots(193.05, 193.15) will return
     * {288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303}
     * </p>
     * @param startFrequency Create frequency slots starting with this frequency.
     * @param endFrequency   Create frequency slots ending with this frequency.
     * @return A bitset representing the occupied frequencies by channel.
     */
    BitSet frequencySlots(@NonNull BigDecimal startFrequency, BigDecimal endFrequency);

}
