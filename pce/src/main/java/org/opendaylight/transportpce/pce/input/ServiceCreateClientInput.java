/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.pce.frequency.interval.Collection;
import org.opendaylight.transportpce.pce.frequency.interval.IntervalFactory;
import org.opendaylight.transportpce.pce.frequency.service.Service;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;

public class ServiceCreateClientInput implements ClientInput {

    private final PathComputationRequestInput pathComputationRequestInput;

    private final IntervalFactory intervalFactory;

    private final Spectrum spectrum;

    private final Service service;

    private final Double frequencyGranularity;

    public ServiceCreateClientInput(
            PathComputationRequestInput pathComputationRequestInput,
            IntervalFactory intervalFactory,
            Spectrum spectrum,
            Service service,
            Double frequencyGranularity) {

        this.pathComputationRequestInput = pathComputationRequestInput;
        this.intervalFactory = intervalFactory;
        this.spectrum = spectrum;
        this.service = service;
        this.frequencyGranularity = frequencyGranularity;
    }

    @Override
    public @NonNull Collection clientRangeWishListIntersection() {
        return intervalFactory.frequencyRange(pathComputationRequestInput, spectrum);
    }

    @Override
    public @NonNull Collection clientRangeWishListSubset() {
        return intervalFactory.frequencySlot(pathComputationRequestInput, spectrum);
    }

    @Override
    public int slotWidth(int spectralWidth) {
        return service.slotWidth(pathComputationRequestInput, spectralWidth, frequencyGranularity);
    }
}
