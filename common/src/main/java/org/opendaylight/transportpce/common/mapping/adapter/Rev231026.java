/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.adapter;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;

public interface Rev231026 {


    /**
     * Translate from connection map rev 181019
     * to rev 231026 connection map.
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> connectionMapRev231026(
        Map<ConnectionMapKey, ConnectionMap> connectionMaps
    );

}
