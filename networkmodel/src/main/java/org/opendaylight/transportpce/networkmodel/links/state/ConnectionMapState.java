/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links.state;

import java.util.Set;
import org.opendaylight.transportpce.networkmodel.links.ConnectionMap;
import org.opendaylight.transportpce.networkmodel.links.Link;
import org.opendaylight.transportpce.networkmodel.links.strategy.AllDirectionsExists;

/**
 * Falls back to {@link AllDirectionsExists} when no connection map links are
 * available, e.g. for devices that don't provide one. Otherwise applies the
 * given {@link ConnectionMap} strategy, which is expected to be backed by the
 * device's actual connection map (see {@link
 * org.opendaylight.transportpce.networkmodel.links.strategy.DeviceConnectionMap}).
 */
public class ConnectionMapState implements State {

    private final ConnectionMap connectionMap;

    public ConnectionMapState(ConnectionMap connectionMap) {
        this.connectionMap = connectionMap;
    }

    @Override
    public ConnectionMap connectionMap(Set<Link> links) {
        if (links.isEmpty()) {
            return new AllDirectionsExists();
        }

        return connectionMap;
    }
}
