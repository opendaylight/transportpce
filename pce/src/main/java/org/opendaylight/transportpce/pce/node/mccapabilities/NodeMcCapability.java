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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mc.capabilities.McCapabilities;

/**
 * The primary purpose of this POJO is to specify default values in regard to MC-capabilities
 * and thus saving the client from the hassle of implementing null-checks.
 */
public class NodeMcCapability implements McCapability {

    private BigDecimal slotWidthGranularityGHz = BigDecimal.valueOf(50);

    private BigDecimal centerFrequencyGranularityGHz = BigDecimal.valueOf(50);

    private int minSlots = 1;

    private int maxSlots = 1;

    /**
     * Create a NodeMcCapability object with default values defined in the yang model:
     * - CenterFrequencyGranularity = 50(GHz).
     * - SlotWidthFrequencyGranularity = 50(GHz).
     * - min and max slots set to 1.
     */
    public NodeMcCapability() {
    }

    public NodeMcCapability(BigDecimal slotWidthGranularityGHz, BigDecimal centerFrequencyGranularityGHz, int minSlots,
            int maxSlots) {

        if (slotWidthGranularityGHz != null) {
            this.slotWidthGranularityGHz = slotWidthGranularityGHz;
        }
        if (centerFrequencyGranularityGHz != null) {
            this.centerFrequencyGranularityGHz = centerFrequencyGranularityGHz;
        }

        this.minSlots = minSlots;
        this.maxSlots = maxSlots;
    }

    /**
     * Create an object using the data in mcCapabilities. If a piece of data is null
     * in mcCapabilities, the default values for this class is used.
     *
     * @see NodeMcCapability#NodeMcCapability()
     */
    public NodeMcCapability(McCapabilities mcCapabilities) {
        this();

        if (mcCapabilities != null) {
            if (mcCapabilities.getSlotWidthGranularity() != null) {
                this.slotWidthGranularityGHz = mcCapabilities.getSlotWidthGranularity().getValue().decimalValue();
            }

            if (mcCapabilities.getCenterFreqGranularity() != null) {
                this.centerFrequencyGranularityGHz = mcCapabilities
                    .getCenterFreqGranularity().getValue().decimalValue();
            }

            if (mcCapabilities.getMinSlots() != null) {
                this.minSlots = mcCapabilities.getMinSlots().intValue();
            }

            if (mcCapabilities.getMaxSlots() != null) {
                this.maxSlots = mcCapabilities.getMaxSlots().intValue();
            }
        }
    }

    @Override
    public @NonNull BigDecimal slotWidthGranularity() {
        return slotWidthGranularityGHz;
    }

    @Override
    public @NonNull BigDecimal centerFrequencyGranularity() {
        return centerFrequencyGranularityGHz;
    }

    @Override
    public int minSlots() {
        return minSlots;
    }

    @Override
    public int maxSlots() {
        return maxSlots;
    }
}
