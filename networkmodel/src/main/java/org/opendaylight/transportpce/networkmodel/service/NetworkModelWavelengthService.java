/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ZToADirection;

public interface NetworkModelWavelengthService {

    /**
     * Remove wavelength from available and add it to used wavelength list.
     *
     * @param atoZDirection AToZDirection: a to z path
     * @param ztoADirection ZToADirection: z to a path
     */
    void useWavelengths(AToZDirection atoZDirection, ZToADirection ztoADirection);

    /**
     * Remove wavelength from used and add it to available wavelength list.
     *
     * @param atoZDirection AToZDirection: a to z path
     * @param ztoADirection ZToADirection: z to a path
     */
    void freeWavelengths(AToZDirection atoZDirection, ZToADirection ztoADirection);

}
