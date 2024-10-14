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
import org.opendaylight.transportpce.pce.frequency.spectrum.index.Index;
import org.opendaylight.yangtools.yang.common.Decimal64;

public class FrequencySpectrum implements Spectrum {


    private final Index idx;

    private final int effectiveBits;

    public FrequencySpectrum(Index idx, int effectiveBits) {
        this.idx = idx;
        this.effectiveBits = effectiveBits;
    }

    @Override
    public boolean isSubset(BitSet needle, BitSet haystack) {

        BitSet haystackClone = (BitSet) haystack.clone();

        haystackClone.and(needle);

        return haystackClone.equals(needle) && !needle.isEmpty();

    }

    @Override
    public BitSet frequencySlots(@NonNull BigDecimal startFrequency, BigDecimal endFrequency) {

        int startIndex = idx.index(Decimal64.valueOf(startFrequency));
        int endIndex = effectiveBits + 1;

        if (endFrequency != null) {
            endIndex = idx.index(Decimal64.valueOf(endFrequency));
        }

        BitSet bitSet = new BitSet(effectiveBits);
        bitSet.set(startIndex, endIndex);

        return bitSet;
    }

}
