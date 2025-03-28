/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openconfiginterface;


import java.util.Set;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.AdminStateType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.ServicePathInput;

public class OpenConfigInterfaceFactory {

    private final OpenConfigInterface190 openConfigInterface190;

    public OpenConfigInterfaceFactory(PortMapping portMapping, OpenConfigInterfaces openConfigInterfaces) {
        this.openConfigInterface190 = new OpenConfigInterface190(portMapping, openConfigInterfaces);
    }

    /**
     * Enable/disable admin-state of client/network port associated to the logical connection point.
     */
    public Set<String> configurePortAdminState(String nodeId, String logicalConnPoint, AdminStateType adminStateType)
            throws OpenConfigInterfacesException {
        return openConfigInterface190.configurePortAdminState(nodeId, logicalConnPoint, adminStateType);
    }

    /**
     * Disable the admin-state of the given port.
     * Typically used during roll back.
     */
    public Set<String> disablePortAdminState(String nodeId, String supportingPort)
            throws OpenConfigInterfacesException {
        return openConfigInterface190.disablePortAdminState(nodeId, supportingPort);
    }

    /**
     * Configures optical channel of network components.
     */
    public String configureNetworkOpticalChannel(String nodeId, String logicalConnPoint, ServicePathInput input)
            throws OpenConfigInterfacesException {
        return openConfigInterface190.configureNetworkOpticalChannel(nodeId, logicalConnPoint, input);
    }

    /**
     * Configures optical channel of client components.
     */
    public Set<String> configureClientOpticalChannel(String nodeId, String logicalConnPoint, String componentProperty)
            throws OpenConfigInterfacesException {
        return openConfigInterface190.configureClientOpticalChannel(nodeId, logicalConnPoint, componentProperty);
    }

    /**
     * Used during rollback operation of client optical channel.
     */
    public Set<String> configureClientOpticalChannel(String nodeId, Set<String> opticalChannelIds,
                                                     String componentProperty) throws OpenConfigInterfacesException {
        return openConfigInterface190.configureClientOpticalChannel(nodeId, opticalChannelIds, componentProperty);
    }

    /**
     * Configures interfaces of client components.
     */
    public Set<String> configureClientInterface(String nodeId, String logicalConnPoint, boolean enableState)
            throws OpenConfigInterfacesException {
        return openConfigInterface190.configureInterface(nodeId, logicalConnPoint, enableState);
    }

    /**
     * Used during rollback operation of client interfaces.
     */
    public Set<String> configureClientInterface(String nodeId, Set<String> interfaceIds, boolean enableState)
            throws OpenConfigInterfacesException {
        return openConfigInterface190.configureInterface(nodeId, interfaceIds, enableState);
    }
}
