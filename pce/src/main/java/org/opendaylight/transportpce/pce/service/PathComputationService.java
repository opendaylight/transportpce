/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;

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
    CancelResourceReserveOutput cancelResourceReserve(CancelResourceReserveInput input);

    /**
     * Requests a path compuation.
     *
     * @param input PathComputationRequestInput data
     * @return output PathComputationRequestOutput data
     */
    PathComputationRequestOutput pathComputationRequest(PathComputationRequestInput input);

}
