/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links.strategy;

import org.opendaylight.transportpce.networkmodel.links.ConnectionMap;

/**
 * In this state, it is assumed all directions are viable.
 */
public class AllDirectionsExists implements ConnectionMap {

    @Override
    public boolean contains(String source, String destination) {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }
}
