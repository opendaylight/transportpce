/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.adapter;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;

public interface Rev231110 {

    Map<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
            .rev231110.network.network.nodes.SharedRiskGroupKey, org.opendaylight.yang.gen.v1.http.com
            .smartoptics.openroadm.srg
            .rev231110.network.network.nodes.SharedRiskGroup> srg(
            Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups
    );

}