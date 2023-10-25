/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRerouteRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRerouteRequestOutput;

/**
 * Path Computation Service.
 */
public interface PathComputationService {

    /**
     * Cancels a resource reservation.
     *
     * @param input CancelResourceReserveInput data
     * @return output CancelResourceReserveOutput data
     */
    ListenableFuture<CancelResourceReserveOutput> cancelResourceReserve(CancelResourceReserveInput input);

    /**
     * Requests a path computation.
     *
     * @param input PathComputationRequestInput data
     * @return output PathComputationRequestOutput data
     */
    ListenableFuture<PathComputationRequestOutput> pathComputationRequest(PathComputationRequestInput input);

    /**
     * Requests a path computation in order to reroute a service.
     *
     * @param input PathComputationRerouteRequestInput data
     * @return output PathComputationRerouteRequestOutput data
     */
    ListenableFuture<PathComputationRerouteRequestOutput> pathComputationRerouteRequest(
            PathComputationRerouteRequestInput input);

}
