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
import org.opendaylight.transportpce.pce.frequency.interval.Collection;
import org.opendaylight.transportpce.pce.input.ClientInput;

public interface Select {

    /**
     * Selecting an available frequency range from which a service frequency may be chosen.
     *
     * <p>
     * Selecting a frequency range may depend on multiple factors.
     *  A)  API input.
     *      This may be viewed as "client wish list". Meaning the client
     *      may wish to restrict picking a frequency range from within a larger
     *      range of the clients choosing. This range may be viewed as the
     *      intersection between the client's range, the available range
     *      and the required range for a service.
     *
     * <p>
     *      The client may also wish to pick a specific range determined
     *      by a central frequency and number of slots. Unlike the precious
     *      example, this range is more of hard requirement. Meaning this range
     *      will have to be a subset of the available range.
     *
     * <p>
     *  B)  Available frequencies.
     *      Independent of the client wish list there are physical restrictions,
     *      such as the available frequencies on the node(s). This will supersede
     *      the client's wishes.
     *
     * <p>
     *  C)  Customer restrictions.
     *      Maybe there is a limitation to what frequencies
     *      are available to a certain customer. This may also supersede both
     *      the client wish list and the available frequencies on the nodes.
     *
     * <p>
     * If any of the requirements cannot be met, an empty BitSet should be returned. Meaning there is
     * no frequency range available suiting the given requirements.
     *
     * @param clientRangeWishListIntersectionLimitation Should be treated as the intersection of available frequencies.
     * @param clientRangeWishListSubsetLimitation Should be treated as a subset of available frequencies.
     * @param availableCustomerRange Should be treated as the intersection of available frequencies.
     * @param availableFrequenciesOnNodes The available frequencies on the nodes.
     */
    BitSet availableFrequencies(
            @NonNull Collection clientRangeWishListIntersectionLimitation,
            @NonNull Collection clientRangeWishListSubsetLimitation,
            @Nullable BitSet availableCustomerRange,
            @NonNull BitSet availableFrequenciesOnNodes
    );


    /**
     * Select a frequency range from which a service frequency may be chosen.
     *
     * @see Select#availableFrequencies(Collection, Collection, BitSet, BitSet)
     */
    BitSet availableFrequencies(
            ClientInput clientInput,
            @Nullable BitSet availableCustomerRange,
            @NonNull BitSet availableFrequenciesOnNodes
    );
}
