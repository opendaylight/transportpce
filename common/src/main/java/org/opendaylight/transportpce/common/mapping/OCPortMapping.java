/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * This class related to  port mapping  operations for openconfig node.
 */
public interface OCPortMapping {

    /**
     * This method creates logical to physical port mapping for a given device.
     * @param nodeId
     *            node ID
     * @param nodeVersion
     *            node version
     * @param ipAddress
     *           ipaddress
     * @return true/false based on status of operation
     */
    boolean createMappingData(String nodeId, String nodeVersion, IpAddress ipAddress);
}
