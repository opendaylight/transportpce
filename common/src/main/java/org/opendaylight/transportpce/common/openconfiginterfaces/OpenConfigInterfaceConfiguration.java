/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

import java.util.List;
import java.util.Set;

/**
 * OpenConfigInterfaceConfiguration provides version-agnostic interface for configuring
 * OpenConfig components and interfaces. Implementations are registered via OSGI for
 * specific OpenConfig versions (OC2.0.0, OC5.6.0, etc.).
 *
 * <p>Note: This interface uses Object types for version-specific YANG models to remain
 * completely version-agnostic. Implementations will handle version-specific type casting.
 */
public interface OpenConfigInterfaceConfiguration {

    /**
     * Get the OpenConfig version supported by this implementation.
     *
     * @return OpenConfig version string (e.g., "2.0.0", "5.6.0")
     */
    String getOpenConfigVersion();

    /**
     * Configure port admin state (enable/disable).
     *
     * @param nodeId node to be provisioned
     * @param logicalConnPoint logical connection point/termination point
     * @param adminStateType admin state (version-specific AdminStateType enum as Object)
     *
     * @return set of configured port IDs
     * @throws OpenConfigInterfacesException if configuration fails
     */
    Set<String> configurePortAdminState(String nodeId, String logicalConnPoint, Object adminStateType)
            throws OpenConfigInterfacesException;

    /**
     * Disable port admin state.
     *
     * @param nodeId node ID
     * @param supportingPort port to disable
     *
     * @return set of disabled port IDs
     * @throws OpenConfigInterfacesException if operation fails
     */
    Set<String> disablePortAdminState(String nodeId, String supportingPort) throws OpenConfigInterfacesException;

    /**
     * Configure client optical channels with tx-dis property.
     *
     * @param nodeId node to be provisioned
     * @param logicalConnPoint logical connection point
     * @param componentProperty enable/disable optical output (FALSE/TRUE)
     *
     * @return set of configured optical channel component names
     * @throws OpenConfigInterfacesException if configuration fails
     */
    Set<String> configureClientOpticalChannel(String nodeId, String logicalConnPoint, String componentProperty)
            throws OpenConfigInterfacesException;

    /**
     * Configure client optical channels for rollback.
     *
     * @param nodeId node ID
     * @param opticalChannelIds optical channel IDs to configure
     * @param componentProperty enable/disable value
     *
     * @return set of configured optical channel names
     * @throws OpenConfigInterfacesException if operation fails
     */
    Set<String> configureClientOpticalChannel(String nodeId, Set<String> opticalChannelIds,
                                              String componentProperty) throws OpenConfigInterfacesException;

    /**
     * Configure network optical channel with operational mode and central frequency.
     *
     * @param nodeId node to be provisioned
     * @param logicalConnPoint logical connection point
     * @param input service path input (version-specific ServicePathInput as Object)
     *
     * @return configured component name
     * @throws OpenConfigInterfacesException if configuration fails
     */
    String configureNetworkOpticalChannel(String nodeId, String logicalConnPoint, Object input)
            throws OpenConfigInterfacesException;

    /**
     * Configure transceiver tx-laser on specified channels.
     *
     * @param nodeId node to be provisioned
     * @param logicalConnPoint logical connection point
     * @param channelIndexes list of channel indices
     * @param isTxLaserEnabled true to enable, false to disable
     *
     * @return configured transceiver component name
     * @throws OpenConfigInterfacesException if configuration fails
     */
    String configureTransceiversTxLaser(String nodeId, String logicalConnPoint,
                    List<Integer> channelIndexes, boolean isTxLaserEnabled) throws OpenConfigInterfacesException;

    /**
     * Disable tx-laser for a transceiver.
     *
     * @param nodeId node ID
     * @param transceiver transceiver name
     *
     * @return transceiver component name
     * @throws OpenConfigInterfacesException if operation fails
     */
    String disableTxLaser(String nodeId, String transceiver) throws OpenConfigInterfacesException;

    /**
     * Configure client interface (enable/disable optical output).
     *
     * @param nodeId node ID
     * @param logicalConnPoint logical connection point
     * @param enableState true to enable, false to disable
     *
     * @return set of configured interface names
     * @throws OpenConfigInterfacesException if configuration fails
     */
    Set<String> configureInterface(String nodeId, String logicalConnPoint, boolean enableState)
            throws OpenConfigInterfacesException;

    /**
     * Configure client interface for rollback.
     *
     * @param nodeId node ID
     * @param interfaceIds interface IDs to configure
     * @param enableState enable/disable state
     *
     * @return set of configured interface names
     * @throws OpenConfigInterfacesException if operation fails
     */
    Set<String> configureInterface(String nodeId, Set<String> interfaceIds, boolean enableState)
            throws OpenConfigInterfacesException;
}
