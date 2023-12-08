/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the equivalent of /dev/null.
 * Meaning nothing is read from or written to this storage.
 * Useful if we know there never is any connection map to save or read.
 * One example is ILA.
 */
public class VoidStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(VoidStorage.class);

    @Override
    public boolean save(String nodeId, Map<ConnectionMapKey, ConnectionMap> connectionMap) {

        if (connectionMap.size() > 0) {
            LOG.warn("Unable to save connection map with size {} for node {} since this storage is void.",
                    connectionMap.size(),
                    nodeId);
        } else {
            LOG.debug("Empty connection map for node {} passed on to void storage and ignored.", nodeId);
        }

        return true;
    }

    @Override
    public Map<ConnectionMapKey, ConnectionMap> read(String nodeId) {
        LOG.debug("Reading connection map from void storage for node {}", nodeId);
        return new HashMap<>();
    }
}
