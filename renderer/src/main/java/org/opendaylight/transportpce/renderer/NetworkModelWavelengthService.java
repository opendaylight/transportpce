/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.PathDescription;

public interface NetworkModelWavelengthService {

    /**
     * Remove wavelength from available and add it to used wavelength list.
     *
     * @param pathDescription
     *   path description containing a-to-z and z-to-a path
     */
    void useWavelengths(PathDescription pathDescription);

    /**
     * Remove wavelength from used and add it to available wavelength list.
     *
     * @param pathDescription
     *   path description containing a-to-z and z-to-a path
     */
    void freeWavelengths(PathDescription pathDescription);

}
