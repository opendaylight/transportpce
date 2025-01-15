/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;
import org.opendaylight.transportpce.pce.spectrum.observer.VoidObserver;

public class McCapabilityCollection implements CapabilityCollection {

    private final Set<McCapability> slots = new LinkedHashSet<>();

    private final Observer observer;

    public McCapabilityCollection() {
        this(new VoidObserver());
    }

    public McCapabilityCollection(Observer observer) {
        this.observer = observer;
    }

    @Override
    public boolean add(McCapability mcCapability) {
        return slots.add(mcCapability);
    }

    @Override
    public boolean isCompatibleService(double slotWidthGranularityGHz, int slotCount) {
        BigDecimal widthGHz = BigDecimal.valueOf(slotWidthGranularityGHz).multiply(BigDecimal.valueOf(slotCount));

        for (McCapability mcCapability : slots) {
            if (!mcCapability.isCompatibleWithServiceFrequency(widthGHz, observer)) {
                return false;
            }
        }

        return true;
    }
}
