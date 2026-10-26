/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_7_1;

import java.util.Objects;
import org.opendaylight.transportpce.common.mapping.MappingUtils;

/**
 * Finds the OpenROADM version of a node, for operations that cannot go on without a supported one.
 *
 * <p>An operation that does not know how to talk to the device must fail rather than return a result that looks
 * like a missing resource or a successful change.
 */
public class DeviceVersionResolver {

    private final MappingUtils mappingUtils;

    /**
     * Creates a resolver.
     *
     * @param mappingUtils reads the node's OpenROADM version from the port mapping
     */
    public DeviceVersionResolver(MappingUtils mappingUtils) {
        this.mappingUtils = Objects.requireNonNull(mappingUtils, "mappingUtils cannot be null");
    }

    /**
     * Returns the OpenROADM version of a node.
     *
     * @param nodeId the node
     * @param operation what the caller is about to do, such as {@code "delete interface X"}, named in the error
     * @return the node's version
     * @throws OpenRoadmInterfaceException if the node has no version in the port mapping, or one that is not
     *     supported
     */
    public DeviceVersion resolve(String nodeId, String operation) throws OpenRoadmInterfaceException {
        String version = mappingUtils.getOpenRoadmVersion(nodeId);
        if (OPENROADM_DEVICE_VERSION_2_2_1.equals(version)) {
            return DeviceVersion.V2_2_1;
        }
        if (OPENROADM_DEVICE_VERSION_7_1.equals(version)) {
            return DeviceVersion.V7_1;
        }
        throw new OpenRoadmInterfaceException("Could not %s on node %s: unknown OpenROADM version %s"
            .formatted(operation, nodeId, version));
    }
}
