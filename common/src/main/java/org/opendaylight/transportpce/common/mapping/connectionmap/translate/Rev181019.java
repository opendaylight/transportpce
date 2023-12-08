/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.translate;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;

/**
 * Translates an OpenROADM device rev181019 connection map into the TransportPCE connection-map model.
 */
public interface Rev181019 {

    /**
     * Translates a rev181019 connection map into the rev260819 TransportPCE connection-map model.
     *
     * @param openRoadmConnectionMap the rev181019 connection map to translate
     * @return the connection map expressed in the TransportPCE connection-map model
     */
    Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCE(
        Map<ConnectionMapKey, ConnectionMap> openRoadmConnectionMap
    );
}
