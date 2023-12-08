/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

/**
 * Delegates to a {@link ConnectionMap} strategy, e.g. one produced by
 * {@link org.opendaylight.transportpce.networkmodel.links.state.State}.
 */
public class InterfaceConnectionMap implements ConnectionMap {

    private final ConnectionMap connectionMapStrategy;

    public InterfaceConnectionMap(ConnectionMap connectionMapStrategy) {
        this.connectionMapStrategy = connectionMapStrategy;
    }

    @Override
    public boolean contains(String source, String destination) {
        return connectionMapStrategy.contains(source, destination);
    }

    @Override
    public int size() {
        return connectionMapStrategy.size();
    }
}
