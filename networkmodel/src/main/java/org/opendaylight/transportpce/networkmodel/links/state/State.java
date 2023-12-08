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

/**
 * Selects which {@link ConnectionMap} strategy applies, given the links found
 * on a node.
 */
public interface State {

    /**
     * Determines the connection map state and returns an appropriate class
     * representing the state.
     *
     * @param links the links found for the node
     * @return the connection map strategy that applies for those links
     */
    ConnectionMap connectionMap(Set<Link> links);

}
