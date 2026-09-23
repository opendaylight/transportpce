/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openconfiginterface;

import java.util.List;
import java.util.Set;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaceConfiguration;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev230208.AdminStateType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenConfigInterfaceFactory - façade for version-specific OpenConfig interface implementations.
 * Uses OSGI registry to discover and use registered version-specific services.
 * Default version: 2.0.0
 */
public class OpenConfigInterfaceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigInterfaceFactory.class);
    private static final String DEFAULT_VERSION = "2.0.0";

    private final OpenConfigInterfaceRegistry registry;

    public OpenConfigInterfaceFactory(OpenConfigInterfaceRegistry registry) {
        this.registry = registry;
    }

    /**
     * Get version-specific service from OSGI registry.
     *
     * @param version the OpenConfig version (e.g. "2.0.0", "5.6.0")
     * @return the registered service for this version
     * @throws IllegalStateException if service not found
     */
    private OpenConfigInterfaceConfiguration getService(String version) {
        if (version == null || version.isEmpty()) {
            version = DEFAULT_VERSION;
        }

        OpenConfigInterfaceConfiguration service = registry.getService(version);
        if (service != null) {
            return service;
        }

        throw new IllegalStateException("OpenConfigInterfaceConfiguration service not found for version: " + version);
    }

    /**
     * Enable/disable admin-state of client/network port (defaults to OC2.0.0).
     */
    public Set<String> configurePortAdminState(String nodeId, String logicalConnPoint, AdminStateType adminStateType)
            throws OpenConfigInterfacesException {
        return configurePortAdminState(DEFAULT_VERSION, nodeId, logicalConnPoint, adminStateType);
    }

    /**
     * Enable/disable admin-state of client/network port for specified version.
     */
    public Set<String> configurePortAdminState(String version, String nodeId, String logicalConnPoint,
                                                AdminStateType adminStateType) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configurePortAdminState(nodeId, logicalConnPoint, adminStateType);
    }

    /**
     * Disable the admin-state of the given port (defaults to OC2.0.0).
     */
    public Set<String> disablePortAdminState(String nodeId, String supportingPort)
            throws OpenConfigInterfacesException {
        return disablePortAdminState(DEFAULT_VERSION, nodeId, supportingPort);
    }

    /**
     * Disable the admin-state of the given port for specified version.
     */
    public Set<String> disablePortAdminState(String version, String nodeId, String supportingPort)
            throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.disablePortAdminState(nodeId, supportingPort);
    }

    /**
     * Configure optical channel of network components (defaults to OC2.0.0).
     */
    public String configureNetworkOpticalChannel(String nodeId, String logicalConnPoint, ServicePathInput input)
            throws OpenConfigInterfacesException {
        return configureNetworkOpticalChannel(DEFAULT_VERSION, nodeId, logicalConnPoint, input);
    }

    /**
     * Configure optical channel of network components for specified version.
     */
    public String configureNetworkOpticalChannel(String version, String nodeId, String logicalConnPoint,
                                                  ServicePathInput input) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configureNetworkOpticalChannel(nodeId, logicalConnPoint, input);
    }

    /**
     * Configure optical channel of client components (defaults to OC2.0.0).
     */
    public Set<String> configureClientOpticalChannel(String nodeId, String logicalConnPoint, String componentProperty)
            throws OpenConfigInterfacesException {
        return configureClientOpticalChannel(DEFAULT_VERSION, nodeId, logicalConnPoint, componentProperty);
    }

    /**
     * Configure optical channel of client components for specified version.
     */
    public Set<String> configureClientOpticalChannel(String version, String nodeId, String logicalConnPoint,
                                                      String componentProperty) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configureClientOpticalChannel(nodeId, logicalConnPoint, componentProperty);
    }

    /**
     * Configure client optical channel for rollback (defaults to OC2.0.0).
     */
    public Set<String> configureClientOpticalChannel(String nodeId, Set<String> opticalChannelIds,
                                                     String componentProperty) throws OpenConfigInterfacesException {
        return configureClientOpticalChannel(DEFAULT_VERSION, nodeId, opticalChannelIds, componentProperty);
    }

    /**
     * Configure client optical channel for rollback for specified version.
     */
    public Set<String> configureClientOpticalChannel(String version, String nodeId, Set<String> opticalChannelIds,
                                                      String componentProperty) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configureClientOpticalChannel(nodeId, opticalChannelIds, componentProperty);
    }

    /**
     * Configure interfaces of client components (defaults to OC2.0.0).
     */
    public Set<String> configureClientInterface(String nodeId, String logicalConnPoint, boolean enableState)
            throws OpenConfigInterfacesException {
        return configureClientInterface(DEFAULT_VERSION, nodeId, logicalConnPoint, enableState);
    }

    /**
     * Configure interfaces of client components for specified version.
     */
    public Set<String> configureClientInterface(String version, String nodeId, String logicalConnPoint,
                                                 boolean enableState) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configureInterface(nodeId, logicalConnPoint, enableState);
    }

    /**
     * Configure client interfaces for rollback (defaults to OC2.0.0).
     */
    public Set<String> configureClientInterface(String nodeId, Set<String> interfaceIds, boolean enableState)
            throws OpenConfigInterfacesException {
        return configureClientInterface(DEFAULT_VERSION, nodeId, interfaceIds, enableState);
    }

    /**
     * Configure client interfaces for rollback for specified version.
     */
    public Set<String> configureClientInterface(String version, String nodeId, Set<String> interfaceIds,
                                                 boolean enableState) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configureInterface(nodeId, interfaceIds, enableState);
    }

    /**
     * Configure transceiver tx-laser (defaults to OC2.0.0).
     */
    public String configureTransceiversTxLaser(String nodeId, String logicalConnPoint,
                    List<Integer> channelIndexes, boolean isTxLaserEnabled)
            throws OpenConfigInterfacesException {
        return configureTransceiversTxLaser(DEFAULT_VERSION, nodeId, logicalConnPoint, channelIndexes,
                isTxLaserEnabled);
    }

    /**
     * Configure transceiver tx-laser for specified version.
     */
    public String configureTransceiversTxLaser(String version, String nodeId, String logicalConnPoint,
                    List<Integer> channelIndexes, boolean isTxLaserEnabled) throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.configureTransceiversTxLaser(nodeId, logicalConnPoint, channelIndexes, isTxLaserEnabled);
    }

    /**
     * Disable tx-laser (defaults to OC2.0.0).
     */
    public String disableTxLaser(String nodeId, String transceiver) throws OpenConfigInterfacesException {
        return disableTxLaser(DEFAULT_VERSION, nodeId, transceiver);
    }

    /**
     * Disable tx-laser for specified version.
     */
    public String disableTxLaser(String version, String nodeId, String transceiver)
            throws OpenConfigInterfacesException {
        OpenConfigInterfaceConfiguration service = getService(version);
        return service.disableTxLaser(nodeId, transceiver);
    }
}
