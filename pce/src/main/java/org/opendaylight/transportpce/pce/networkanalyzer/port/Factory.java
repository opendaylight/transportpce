/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestInput;

public interface Factory {

    /**
     * Extracting preferred ports from pathComputationRequestInput.
     *
     * <p>
     * This is the recommended method of determining if a node/port combination
     * is preferred by the client.
     *
     * <p>
     * Pseudocode example:
     * <pre>
     *     Factory.portPreference(PCRI).preferredPort("ROADM-B-SRG1", "SRG1-PP1-TXRX");
     * </pre>
     *
     * @return Client port preference
     */
    Preference portPreference(PathComputationRequestInput pathComputationRequestInput);

    /**
     * Create a key value mapper from PCRI where key is the node and the value is
     * a unique list of port names.
     *
     * @return Client port preference map
     */
    Map<String, Set<String>> nodePortMap(PathComputationRequestInput pathComputationRequestInput);

    /**
     * Add node/port name to key value map. Mutable method, modifies the argument nodePortMap.
     */
    boolean add(String node, String portName, Map<String, Set<String>> nodePortMap);

}
