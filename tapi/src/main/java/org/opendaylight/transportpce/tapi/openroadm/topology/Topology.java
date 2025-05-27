/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;

public interface Topology {

    /**
     * Copies the OpenROADM topology to TAPI.
     *
     * @param topologyUpdateResult the result of the topology update
     */
    void copyTopologyToTAPI(TopologyUpdateResult topologyUpdateResult);

}
