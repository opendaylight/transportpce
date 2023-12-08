/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;

public interface Storage {

    /**
     * Save connection map for a node.
     *
     * @return false if the connection map couldn't be saved.
     */
    boolean save(String nodeId, Map<ConnectionMapKey, ConnectionMap> connectionMap);

    /**
     * Read connection map for a node.
     *
     * @return Connection map or an empty map if no connection map is found in storage.
     */
    Map<ConnectionMapKey, ConnectionMap> read(String nodeId);

}
