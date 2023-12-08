/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links.strategy;

import java.util.Set;
import org.opendaylight.transportpce.networkmodel.links.ConnectionMap;
import org.opendaylight.transportpce.networkmodel.links.InterfaceLink;
import org.opendaylight.transportpce.networkmodel.links.Link;

/**
 * In this state the application will use the connection map.
 */
public class DeviceConnectionMap implements ConnectionMap {

    private final Set<Link> linkMap;

    public DeviceConnectionMap(Set<Link> linkMap) {
        this.linkMap = linkMap;
    }

    @Override
    public boolean contains(String source, String destination) {
        return linkMap.contains(new InterfaceLink(source, destination));
    }

    @Override
    public int size() {
        return linkMap.size();
    }
}
