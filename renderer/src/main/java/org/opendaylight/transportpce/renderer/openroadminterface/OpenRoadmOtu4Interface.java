/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;

public class OpenRoadmOtu4Interface extends OpenRoadmInterfaces {

    public OpenRoadmOtu4Interface(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint) {
        super(db, mps, nodeId, logicalConnPoint);

    }

    /**
     * This methods creates an OTU interface on the given termination point.
     *
     *
     * @return Name of the interface if successful, otherwise return null.
     */

    public String createInterface() {
        // TODO: Implement this method
        return null;
    }

}
