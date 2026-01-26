/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;

public interface TopologyUpdate {

    /**
     * Applies OpenROADM topology changes to the TAPI topology.
     *
     * <p>The input is a {@link TopologyUpdateResult} produced by the OpenROADM topology update logic.
     * Implementations typically translate the changed OpenROADM termination points into one-or-more
     * TAPI NEPs and then update derived information on those NEPs (for example spectrum capability PACs)
     * in the TAPI operational datastore.
     *
     * <p>If {@code topologyUpdateResult} is {@code null} or contains no changes, implementations may
     * treat the call as a no-op.
     *
     * @param topologyUpdateResult the result of an OpenROADM topology update; may be {@code null}
     * @return {@code true} if the update was applied successfully (or there was nothing to apply)
     * @throws IllegalStateException if required topology data cannot be read or required dependencies fail
     */
    boolean copyToTAPI(TopologyUpdateResult topologyUpdateResult);

}
