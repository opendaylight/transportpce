/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesKey;

/**
 * Persists and reads OpenROADM connection maps, independent of device revision.
 */
public interface Storage {

    /**
     * Translates and saves a rev181019 connection map for a node.
     *
     * @param nodeId identifier of the node the connection map belongs to
     * @param connectionMap the rev181019 connection map to save
     * @return false if the connection map couldn't be saved
     */
    boolean saveRev181019(String nodeId,
                          Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm
                                  .device.container.org.openroadm.device
                                  .ConnectionMapKey,
                              org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm
                                  .device.container.org.openroadm.device
                                  .ConnectionMap> connectionMap);

    /**
     * Translates and saves a rev200529 connection map for a node.
     *
     * @param nodeId identifier of the node the connection map belongs to
     * @param connectionMap the rev200529 connection map to save
     * @return false if the connection map couldn't be saved
     */
    boolean saveRev200529(String nodeId,
                          Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm
                                  .device.container.org.openroadm.device
                                  .ConnectionMapKey,
                              org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm
                                  .device.container.org.openroadm.device
                                  .ConnectionMap> connectionMap);

    /**
     * Reads the connection map for a node.
     *
     * @param nodeId identifier of the node to read the connection map for
     * @return the connection map, or an empty map if none is found in storage
     */
    Map<NetworkNodesKey, NetworkNodes> read(String nodeId);
}
