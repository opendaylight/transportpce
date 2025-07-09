/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.adapter;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.srg.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.srg.CircuitPacksKey;

public interface Rev181019 {
    Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
            .rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device
                    .rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup> srg(
            Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups
    );

    Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
            .rev181019.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.org.openroadm.device
            .rev181019.srg.CircuitPacks> circuitPacks(Map<CircuitPacksKey, CircuitPacks> rev231110);

}
