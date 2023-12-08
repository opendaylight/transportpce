/*
 * Copyright © 2023  AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links.state;

import java.util.Set;
import org.opendaylight.transportpce.networkmodel.links.ConnectionMap;
import org.opendaylight.transportpce.networkmodel.links.Link;

public interface State {

    /**
     * Determines the connection map state and returns an appropriate class
     * representing the state.
     */
    ConnectionMap connectionMap(Set<Link> links);

}
