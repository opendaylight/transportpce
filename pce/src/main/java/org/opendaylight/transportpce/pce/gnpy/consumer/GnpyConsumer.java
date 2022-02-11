/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;


import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev201022.Result;


public interface GnpyConsumer {

    /**
     * Check if api is available or not.
     * @return true os available, false otherwise.
     */
    boolean isAvailable();

    /**
     * Path computation request.
     * @param request GnpyApi.
     * @return Result the result of pat computation.
     */
    Result computePaths(Request request);

}
