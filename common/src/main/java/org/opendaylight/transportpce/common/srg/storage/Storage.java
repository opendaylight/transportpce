/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupKey;

public interface Storage {

    boolean save(String nodeId, Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap);

    Map<SharedRiskGroupKey, SharedRiskGroup> read(String nodeId);

    Map<SharedRiskGroupKey, SharedRiskGroup> read(NetworkNodesKey nodeId);
}