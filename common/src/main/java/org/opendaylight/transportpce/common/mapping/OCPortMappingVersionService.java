/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260908.mapping.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * Service interface for version-specific OpenConfig Port Mapping.
 *
 * <p>Implementations are registered as OSGi services with a version property.
 * This allows dynamic service selection at runtime without classloader conflicts
 * between incompatible YANG model versions (OC200 vs OC560).
 */
public interface OCPortMappingVersionService {

    /**
     * Get the version identifier.
     *
     * @return version string (e.g., "openconfig-200", "openconfig-560")
     */
    String getVersion();

    /**
     * Create mapping data for a node.
     *
     * @param nodeId the node identifier
     * @param ipAddress the IP address of the device
     * @return true if mapping was created successfully
     */
    boolean createMappingData(String nodeId, IpAddress ipAddress);

    /**
     * Update existing mapping data.
     *
     * @param nodeId the node identifier
     * @param oldMapping the existing mapping to update
     * @return true if mapping was updated successfully
     */
    boolean updateMapping(String nodeId, Mapping oldMapping);
}
