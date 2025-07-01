/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.util.Map;
import java.util.Set;

public interface NodeTerminationPoint {

    /**
     * Returns a map of node IDs to sets of termination point IDs.
     *
     * @return a map where the key is a node ID and the value is a set of termination point IDs
     */
    Map<String, Set<String>> nodeTerminationPointIdMap();

    boolean isEmpty();

}
