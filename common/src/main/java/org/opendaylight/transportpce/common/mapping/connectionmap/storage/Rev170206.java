/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import java.util.Map;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.Translate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;

public interface Rev170206 {

    /**
     * Save connection map for a node.
     *
     * @return false if the connection map couldn't be saved.
     */
    boolean save(
        DataBroker dataBroker,
        Translate openRoadm,
        String nodeId,
        Map<ConnectionMapKey, ConnectionMap> connectionMap
    );

}
