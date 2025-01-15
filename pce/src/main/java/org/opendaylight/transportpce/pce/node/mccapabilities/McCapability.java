/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.node.mccapabilities;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.math.BigDecimal;

/**
 * This interface is intended to specify methods from
 * which the client may retrieve MC Capabilities.
 */
public interface McCapability {

    /**
     * Width of a slot measured in GHz.
     */
    @NonNull BigDecimal slotWidthGranularity();

    /**
     * Granularity of allowed center frequencies.
     * The base frequency for this computation is 193.1 THz (G.694.1)
     */
    @NonNull BigDecimal centerFrequencyGranularity();

    /**
     * Minimum number of slots permitted to be joined together to form a media channel.
     * Must be less than or equal to the max-slots.
     */
    int minSlots();

    /**
     * Maximum number of slots permitted to be joined together to form a media channel.
     * Must be greater than or equal to the min-slots.
     */
    int maxSlots();

}
