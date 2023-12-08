/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;

public interface Factory {

    /**
     * Read circuit connections from storage.
     *
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
    Set<Link> connections(
            Map<String, Set<String>> circuitConnections,
            Map<String, String> translation
    );

    /**
     * A read friendly string with links.
     */
    String connections(Set<Link> links);
}
