/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency;

import java.util.BitSet;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.pce.frequency.input.ClientInput;
import org.opendaylight.transportpce.pce.frequency.interval.Collection;

public class FrequencySelectionFactory implements Select {

    @Override
    public BitSet availableFrequencies(
            @NonNull Collection clientRangeWishListIntersectionLimitation,
            @NonNull Collection clientRangeWishListSubsetLimitation,
            @Nullable BitSet availableCustomerRange,
            @NonNull BitSet availableFrequenciesOnNodes) {

        BitSet availableFrequencies = (BitSet) availableFrequenciesOnNodes.clone();

        if (availableCustomerRange != null) {
            availableFrequencies.and(availableCustomerRange);
        }

        BitSet availableRange = clientRangeWishListIntersectionLimitation.intersection(
                availableFrequencies
        );

        return clientRangeWishListSubsetLimitation.subset(availableRange);
    }


    @Override
    public BitSet availableFrequencies(
            ClientInput clientInput,
            @Nullable BitSet availableCustomerRange,
            @NonNull BitSet availableFrequenciesOnNodes) {

        return availableFrequencies(
                clientInput.clientRangeWishListIntersection(),
                clientInput.clientRangeWishListSubset(),
                availableCustomerRange,
                availableFrequenciesOnNodes
        );
    }
}
