/*
 * Copyright (c) 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;

public interface Factory {

    /**
     * Extracting preferred ports from pathComputationRequestInput.
     *
     * <p>This is the recommended method of determining if a node/port combination
     * is preferred by the client.
     *
     * <p>Pseudocode example:
     * <pre>
     *     Factory.portPreference(PCRI).preferredPort("ROADM-B-SRG1", "SRG1-PP1-TXRX");
     * </pre>
     *
     * @return Client port preference
     */
    Preference portPreference(PathComputationRequestInput pathComputationRequestInput);

}
