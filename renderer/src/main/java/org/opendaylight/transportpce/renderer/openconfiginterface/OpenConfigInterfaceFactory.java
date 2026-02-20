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
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev230208.AdminStateType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev251001.ServicePathInput;

public class OpenConfigInterfaceFactory {

    private final OpenConfigInterface200 openConfigInterface200;

    public OpenConfigInterfaceFactory(PortMapping portMapping, OpenConfigInterfaces openConfigInterfaces) {
        this.openConfigInterface200 = new OpenConfigInterface200(portMapping, openConfigInterfaces);
    }

    /**
     * Enable/disable admin-state of client/network port associated to the logical connection point.
     */
    public Set<String> configurePortAdminState(String nodeId, String logicalConnPoint, AdminStateType adminStateType)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.configurePortAdminState(nodeId, logicalConnPoint, adminStateType);
    }

    /**
     * Disable the admin-state of the given port.
     * Typically used during roll back.
     */
    public Set<String> disablePortAdminState(String nodeId, String supportingPort)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.disablePortAdminState(nodeId, supportingPort);
    }

    /**
     * Configures optical channel of network components.
     */
    public String configureNetworkOpticalChannel(String nodeId, String logicalConnPoint, ServicePathInput input)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.configureNetworkOpticalChannel(nodeId, logicalConnPoint, input);
    }

    /**
     * Configures optical channel of client components.
     */
    public Set<String> configureClientOpticalChannel(String nodeId, String logicalConnPoint, String componentProperty)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.configureClientOpticalChannel(nodeId, logicalConnPoint, componentProperty);
    }

    /**
     * Used during rollback operation of client optical channel.
     */
    public Set<String> configureClientOpticalChannel(String nodeId, Set<String> opticalChannelIds,
                                                     String componentProperty) throws OpenConfigInterfacesException {
        return openConfigInterface200.configureClientOpticalChannel(nodeId, opticalChannelIds, componentProperty);
    }

    /**
     * Configures interfaces of client components.
     */
    public Set<String> configureClientInterface(String nodeId, String logicalConnPoint, boolean enableState)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.configureInterface(nodeId, logicalConnPoint, enableState);
    }

    /**
     * Used during rollback operation of client interfaces.
     */
    public Set<String> configureClientInterface(String nodeId, Set<String> interfaceIds, boolean enableState)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.configureInterface(nodeId, interfaceIds, enableState);
    }

    public String configureTransceiversTxLaser(String nodeId, String logicalConnPoint,
                    List<Integer> channelIndexes, boolean isTxLaserEnabled)
            throws OpenConfigInterfacesException {
        return openConfigInterface200.configureTransceiversTxLaser(nodeId, logicalConnPoint, channelIndexes,
                isTxLaserEnabled);
    }

    public String disableTxLaser(String nodeId, String transceiver) throws OpenConfigInterfacesException {
        return openConfigInterface200.disableTxLaser(nodeId, transceiver);
    }
}
