/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;

public interface FrequenciesService {

    /**
     * Allocate frequency in network topology.
     *
     * @param atoZDirection AToZDirection: a to z path
     * @param ztoADirection ZToADirection: z to a path
     */
    void allocateFrequencies(AToZDirection atoZDirection, ZToADirection ztoADirection);

    /**
     * Release frequency in network topology.
     *
     * @param atoZDirection AToZDirection: a to z path
     * @param ztoADirection ZToADirection: z to a path
     */
    void releaseFrequencies(AToZDirection atoZDirection, ZToADirection ztoADirection);

}
