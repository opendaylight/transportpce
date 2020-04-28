/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;

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
     * Requests a path compuation.
     *
     * @param input PathComputationRequestInput data
     * @return output PathComputationRequestOutput data
     */
    ListenableFuture<PathComputationRequestOutput> pathComputationRequest(PathComputationRequestInput input);

}
