/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodesKey;

public interface Storage {

    /**
     * Save connection map for a node.
     *
     * @return false if the connection map couldn't be saved.
     */
    boolean saveRev170206(String nodeId,
                          Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev170206.org.openroadm.device.container.org.openroadm.device
                                    .ConnectionMapKey,
                              org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev170206.org.openroadm.device.container.org.openroadm.device
                                    .ConnectionMap> connectionMap);

    /**
     * Save connection map for a node.
     *
     * @return false if the connection map couldn't be saved.
     */
    boolean saveRev181019(String nodeId,
                          Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev181019.org.openroadm.device.container.org.openroadm.device
                                    .ConnectionMapKey,
                              org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev181019.org.openroadm.device.container.org.openroadm.device
                                    .ConnectionMap> connectionMap);

    /**
     * Save connection map for a node.
     *
     * @return false if the connection map couldn't be saved.
     */
    boolean saveRev200529(String nodeId,
                          Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev200529.org.openroadm.device.container.org.openroadm.device
                                    .ConnectionMapKey,
                              org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev200529.org.openroadm.device.container.org.openroadm.device
                                    .ConnectionMap> connectionMap);

    /**
     * Read connection map for a node.
     *
     * @return Connection map or an empty map if no connection map is found in storage.
     */
    Map<NetworkNodesKey, NetworkNodes> read(String nodeId);

}
