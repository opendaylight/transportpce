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

/**
 * Translates OpenROADM device connection maps of either supported revision into the
 * TransportPCE connection-map model.
 */
public interface Translate {

    /**
     * Translates a rev181019 connection map into the rev231026 TransportPCE connection-map model.
     *
     * @param connectionMap the rev181019 connection map to translate
     * @return the connection map expressed in the TransportPCE connection-map model
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCErev181019(
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap
    );

    /**
     * Translates a rev200529 connection map into the rev231026 TransportPCE connection-map model.
     *
     * @param connectionMap the rev200529 connection map to translate
     * @return the connection map expressed in the TransportPCE connection-map model
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCErev200529(
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                .org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                .org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap
    );
}
