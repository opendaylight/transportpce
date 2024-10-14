/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.input;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.pce.frequency.interval.Collection;

public interface ClientInput {

    /**
     * A frequency collection representing how the client may wish to filter
     * the range of available frequencies from which a service frequency range is chosen.
     * The intent is to apply an intersection calculation between the client's wish list
     * and the available frequencies. Subsequently, a service frequency range
     * is picked from the result.
     *
     * <p>
     * The implementing class is free to interpret how to handle the situation where
     * the client has no preference.
     */
    @NonNull Collection clientRangeWishListIntersection();

    /**
     * A frequency collection representing how the client may wish to restrict/limit
     * the range of frequencies from which a service is setup.
     * The intent is to apply a subset calculation between the client's wish list
     * and the available frequencies. Subsequently, a service frequency range may be
     * setup from the result.
     *
     * <p>
     * The implementing class is free to interpret how to handle the situation where
     * the client has no preference.
     */
    @NonNull Collection clientRangeWishListSubset();

    /**
     * The number of slots the client wants to use. Each slot is assumed to be 6.25 GHz wide.
     */
    int slots(int spectralWidth);
}
