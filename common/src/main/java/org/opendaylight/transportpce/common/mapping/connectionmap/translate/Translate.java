/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.translate;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMapKey;

public interface Translate {

    /**
     * Translate from connection map rev 170206
     * to rev 231026 connection map.
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCErev170206(
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap
    );

    /**
     * Translate from connection map rev 181019
     * to rev 231026 connection map.
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCErev181019(
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap
    );

    /**
     * Translate from connection map rev 200529
     * to rev 231026 connection map.
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCErev200529(
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap
    );
}
