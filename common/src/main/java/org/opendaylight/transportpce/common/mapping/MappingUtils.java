/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilities;

public interface MappingUtils {

    /*
        This method returns the OpenROADM yang release
        supported by the device.
     */
    String getOpenRoadmVersion(String nodeId);

    /**
     * Get list of mc capabilities for node with nodeId.
     * @param nodeId String
     * @return the list of McCapabilities for the node.
     */
    List<McCapabilities> getMcCapabilitiesForNode(String nodeId);
}
