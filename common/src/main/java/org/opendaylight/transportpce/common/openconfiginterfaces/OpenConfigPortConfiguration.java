/*
 * Copyright © 2024 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

import org.opendaylight.transportpce.common.mapping.PortMapping;

/**
 * Abstract interface for OpenConfig port configuration, isolating version-specific implementations
 * (OC200 vs OC560) to allow both versions to coexist via OSGI service registration.
 */
public interface OpenConfigPortConfiguration {

    /**
     * Retrieves the OpenConfig version this implementation supports.
     *
     * @return the OpenConfig version string (e.g., "2.0.0" or "5.6.0")
     */
    String getOpenConfigVersion();

    /**
     * Initializes the port configuration handler with required services.
     *
     * @param portMapping the PortMapping service
     * @param openConfigInterfaces the OpenConfigInterfaces service
     */
    void init(PortMapping portMapping, OpenConfigInterfaces openConfigInterfaces);

    /**
     * Configures a port's admin state (enable/disable).
     *
     * @param nodeId the node identifier
     * @param logicalConnPoint the logical connection point
     * @param enable true to enable, false to disable
     * @return the physical port identifier that was configured
     * @throws OpenConfigInterfacesException if configuration fails
     */
    String configurePortAdminState(String nodeId, String logicalConnPoint, boolean enable)
            throws OpenConfigInterfacesException;

    /**
     * Configures a port's frequency.
     *
     * @param nodeId the node identifier
     * @param logicalConnPoint the logical connection point
     * @param frequency the frequency to set
     * @throws OpenConfigInterfacesException if configuration fails
     */
    void configurePortFrequency(String nodeId, String logicalConnPoint, long frequency)
            throws OpenConfigInterfacesException;

    /**
     * Gets the power configuration parameters for a given operational mode.
     *
     * @param operationalMode the operational mode string
     * @return a map with power configuration (MinTx, MaxTx, etc)
     */
    java.util.Map<String, Double> getPortPowerParameters(String operationalMode);
}
