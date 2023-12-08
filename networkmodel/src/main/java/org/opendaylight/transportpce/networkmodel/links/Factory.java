/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;

/**
 * Builds {@link Link}s from a device's circuit pack connection map, translating
 * circuit pack names into the node/port names used by the network model.
 */
public interface Factory {

    /**
     * Read circuit connections from storage.
     *
     * @param storage the storage to read the circuit connections from
     * @param nodeId the node to read the circuit connections for
     * @return e.g. {1/WSS -> (1/XC2, 1/XC3)}
     */
    Map<String, Set<String>> circuitConnections(Storage storage, String nodeId);

    /**
     * Create node links.
     *
     * @param circuitConnections e.g. {1/WSS -> (1/XC2, 1/XC3)}
     * @param translation e.g. {(1/WSS -> ROADM-B-DEG1), (1/XC2 -> ROADM-B-SRG3), 1/XC3 -> ROADM-B-SRG1)}
     * @return e.g {('ROADM-B-DEG1' -> 'ROADM-B-SRG3'), ('ROADM-B-DEG1' -> 'ROADM-B-SRG1')}
     */
    Set<Link> connections(Map<String, Set<String>> circuitConnections, Map<String, String> translation);

    /**
     * A read friendly string with links.
     *
     * @param links the links to render
     * @return one "(source:destination)" entry per line
     */
    String connections(Set<Link> links);
}
