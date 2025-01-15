/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;
import org.opendaylight.transportpce.pce.spectrum.observer.VoidObserver;

public class InterfaceMcCapability implements McCapability {

    private final String node;

    private final BigDecimal slotWidthGranularity;

    private final int minSlots;

    private final int maxSlots;

    public InterfaceMcCapability(BigDecimal slotWidthGranularity, int minSlots, int maxSlots) {
        this("Unknown node", slotWidthGranularity, minSlots, maxSlots);
    }

    public InterfaceMcCapability(@NonNull String node, BigDecimal slotWidthGranularity, int minSlots, int maxSlots) {
        this.node = node;
        this.slotWidthGranularity = slotWidthGranularity;
        this.minSlots = minSlots;
        this.maxSlots = maxSlots;
    }

    public InterfaceMcCapability(double slotWidthGranularity, int minSlots, int maxSlots) {
        this(BigDecimal.valueOf(slotWidthGranularity), minSlots, maxSlots);
    }

    public InterfaceMcCapability(@NonNull String node, double slotWidthGranularity, int minSlots, int maxSlots) {
        this(node, BigDecimal.valueOf(slotWidthGranularity), minSlots, maxSlots);
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz) {
        return isCompatibleWithServiceFrequency(requiredFrequencyWidthGHz, new VoidObserver());
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer) {

        BigDecimal quotient;
        try {
            quotient = requiredFrequencyWidthGHz.divide(slotWidthGranularity);
        } catch (ArithmeticException e) {
            return false;
        }

        BigDecimal remainder = requiredFrequencyWidthGHz.remainder(slotWidthGranularity);

        if (remainder.compareTo(BigDecimal.ZERO) == 0
            && quotient.compareTo(BigDecimal.valueOf(minSlots)) >= 0
            && quotient.compareTo(BigDecimal.valueOf(maxSlots)) <= 0) {
            return true;
        }

        observer.error(
            String.format("Cannot fit a service with %sGHz on node %s, "
                    + "with slot width granularity %s min slots %s and max slots %s",
                requiredFrequencyWidthGHz,
                node,
                slotWidthGranularity,
                minSlots,
                maxSlots
            )
        );

        return false;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz) {
        return isCompatibleWithServiceFrequency(BigDecimal.valueOf(requiredFrequencyWidthGHz), new VoidObserver());
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz, Observer observer) {
        return isCompatibleWithServiceFrequency(BigDecimal.valueOf(requiredFrequencyWidthGHz), observer);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof InterfaceMcCapability interfaceMcCapability)) {
            return false;
        }
        return node.equals(interfaceMcCapability.node)
            && minSlots == interfaceMcCapability.minSlots
            && maxSlots == interfaceMcCapability.maxSlots
            && Objects.equals(slotWidthGranularity, interfaceMcCapability.slotWidthGranularity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotWidthGranularity, minSlots, maxSlots);
    }
}
