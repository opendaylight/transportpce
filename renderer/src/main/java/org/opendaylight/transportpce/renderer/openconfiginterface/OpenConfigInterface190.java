/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openconfiginterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.PlatformComponentPropertiesConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.anchors.top.PortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.properties.top.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.properties.top.properties.Property;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.properties.top.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.properties.top.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.Component1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.OpticalChannelBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.OpticalPortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.optical.port.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.optical.port.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.AdminStateType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.FrequencyType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mapping.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev230126.EthernetCsmacd;

public class OpenConfigInterface190 {

    private final PortMapping portMapping;
    private final OpenConfigInterfaces openConfigInterfaces;

    public OpenConfigInterface190(PortMapping portMapping, OpenConfigInterfaces openConfigInterfaces) {
        this.portMapping = portMapping;
        this.openConfigInterfaces = openConfigInterfaces;
    }

    /**
     * This method updates the admin-state of a line/client port component to enable/disable.
     *
     * @param nodeId node to be provisioned
     * @param logicalConnPoint logical connection point/termination point read from input
     * @param adminStateType admin state of the component, enable/disable
     *
     * @return configured component name.
     * @throws OpenConfigInterfacesException openConfig exception with successfully configured port
     */

    public Set<String> configurePortAdminState(String nodeId, String logicalConnPoint, AdminStateType adminStateType)
            throws OpenConfigInterfacesException {
        Set<String> portIds = new HashSet<>();
        try {
            Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
            ComponentBuilder portComponentBuilder =
                    createPortComponentBuilder(mapping.getSupportingPort(), adminStateType);
            openConfigInterfaces.configureComponent(nodeId, portComponentBuilder);
            portIds.add(portComponentBuilder.getName());
        } catch (OpenConfigInterfacesException e) {
            throw new OpenConfigInterfacesException(portIds, e);
        }
        return portIds;
    }

    /**
     * This method will be used during rollback operation to disable admin-state of network/client component.
     *
     * @param nodeId Node ID
     * @param supportingPort portId of the node to be rolled back
     *
     * @return set of rolled back ports
     * @throws OpenConfigInterfacesException openConfig exception
     */

    public Set<String> disablePortAdminState(String nodeId, String supportingPort) throws
            OpenConfigInterfacesException {
        Set<String> portIds = new HashSet<>();
        ComponentBuilder portComponentBuilder = createPortComponentBuilder(supportingPort, AdminStateType.DISABLED);
        openConfigInterfaces.configureComponent(nodeId, portComponentBuilder);
        portIds.add(portComponentBuilder.getName());
        return portIds;
    }

    /**
     * This method configures client optical-channel components.
     *
     * @param nodeId node to be provisioned
     * @param logicalConnPoint logical connection point/termination point read from input
     * @param componentProperty enable/disable optical output of client optical-channel, FALSE/TRUE
     *
     * @return configured components name
     * @throws OpenConfigInterfacesException openConfig exception with successfully configured
     *         client optical-channel list
     */

    public Set<String> configureClientOpticalChannel(String nodeId, String logicalConnPoint, String componentProperty)
            throws OpenConfigInterfacesException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        List<ComponentBuilder> clientOpticalChannelBuilders = getClientOpticalChannelBuilders(
                mapping.getOpenconfigInfo().getSupportedOpticalChannels(), componentProperty);
        Set<String> opticalChannelComponentNames = new HashSet<>();
        try {
            for (ComponentBuilder clientOpticalBuilder : clientOpticalChannelBuilders) {
                openConfigInterfaces.configureComponent(nodeId, clientOpticalBuilder);
                opticalChannelComponentNames.add(clientOpticalBuilder.getName());
            }
        } catch (OpenConfigInterfacesException e) {
            throw new OpenConfigInterfacesException(opticalChannelComponentNames, e);
        }
        return opticalChannelComponentNames;
    }

    /**
     * This method is used to roll back the configured client optical-channels.
     *
     * @param nodeId Node ID
     * @param opticalChannelIds optical channels to be rolled back
     * @param componentProperty enable/disable optical output of client optical-channel, FALSE/TRUE
     *
     * @return rolled back component name
     * @throws OpenConfigInterfacesException open config exception
     */

    public Set<String> configureClientOpticalChannel(String nodeId, Set<String> opticalChannelIds,
                                                     String componentProperty)
            throws OpenConfigInterfacesException {
        List<ComponentBuilder> clientOpticalChannelBuilders = getClientOpticalChannelBuilders(opticalChannelIds,
                componentProperty);
        Set<String> opticalChannelComponentNames = new HashSet<>();
        for (ComponentBuilder clientOpticalBuilder : clientOpticalChannelBuilders) {
            openConfigInterfaces.configureComponent(nodeId, clientOpticalBuilder);
            opticalChannelComponentNames.add(clientOpticalBuilder.getName());
        }
        return opticalChannelComponentNames;
    }

    /**
     * This method generates ComponentBuilder needed for client optical-channel configuration.
     *
     * @param supportedOpticalChannels set of optical channels to be configured
     * @param componentProperty enable/disable optical output of client optical-channel, FALSE/TRUE
     *
     * @return ComponentBuilders of client optical-channels to be configured
     */

    private List<ComponentBuilder> getClientOpticalChannelBuilders(Set<String> supportedOpticalChannels,
                                                                   String componentProperty) {
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.properties.top
                .properties.property.ConfigBuilder configBuilder = new org.opendaylight.yang.gen.v1.http.openconfig
                .net.yang.platform.rev220610.platform.component.properties.top.properties.property.ConfigBuilder();
        configBuilder.setName("tx-dis");
        configBuilder.setValue(new PlatformComponentPropertiesConfig.Value(componentProperty));

        PropertyBuilder propertyBuilder = new PropertyBuilder();
        propertyBuilder.setName("tx-dis");
        propertyBuilder.setConfig(configBuilder.build());

        HashMap<PropertyKey, Property> propertyMap = new HashMap<>();
        PropertyKey propertyKey = new PropertyKey("tx-dis");
        propertyMap.put(propertyKey, propertyBuilder.build());

        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        propertiesBuilder.setProperty(propertyMap);

        ArrayList<ComponentBuilder> componentBuilders = new ArrayList<>();
        if (supportedOpticalChannels != null) {
            for (String supportedOpticalChannel : supportedOpticalChannels) {
                ComponentBuilder componentBuilder = new ComponentBuilder();
                componentBuilder.setName(supportedOpticalChannel);
                componentBuilder.setProperties(propertiesBuilder.build());
                componentBuilders.add(componentBuilder);
            }
        }
        return componentBuilders;
    }

    /**
     * This method generates ComponentBuilder needed for network optical-channel configuration.
     *
     * @param nodeId NodeID
     * @param logicalConnPoint logical connection point/termination point read from input
     * @param input payload passed from the rest client
     *
     * @return component name which was configured successfully.
     */

    public String configureNetworkOpticalChannel(String nodeId, String logicalConnPoint, ServicePathInput input)
            throws OpenConfigInterfacesException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        List<ComponentBuilder> opticalChannelBuilders = createOpticalChannelComponentBuilder(mapping, input);
        StringBuilder opticalChannelComponentNames = new StringBuilder();
        for (ComponentBuilder opticalChannelBuilder : opticalChannelBuilders) {
            openConfigInterfaces.configureComponent(nodeId, opticalChannelBuilder);
            opticalChannelComponentNames.append(opticalChannelBuilder.getName()).append(" ");
        }
        return opticalChannelComponentNames.toString();
    }

    /**
     * This method is used to generate ComponentBuilder for configuring port admin state.
     *
     * @param supportingPort Port to be configured
     * @param adminStateType enable/disable
     *
     * @return ComponentBuilder with port admin state configuration.
     */

    private ComponentBuilder createPortComponentBuilder(String supportingPort, AdminStateType adminStateType) {
        Config config = new ConfigBuilder().setAdminState(adminStateType).build();
        Port1 port1 = new Port1Builder().setOpticalPort(new OpticalPortBuilder().setConfig(config).build()).build();
        PortBuilder portBuilder = new PortBuilder().addAugmentation(port1);
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.setName(supportingPort);
        componentBuilder.setPort(portBuilder.build());
        return componentBuilder;
    }

    /**
     * This method generates list of ComponentBuilders for optical-channels.
     *
     * @param portMap Mapping containing optical-channel list
     * @param input payload from the rest client
     *
     * @return ComponentBuilder list containing optical-channel configuration.
     */

    private List<ComponentBuilder> createOpticalChannelComponentBuilder(Mapping portMap, ServicePathInput input) {
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729
                .terminal.optical.channel.top.optical.channel.ConfigBuilder configBuilder = new org.opendaylight
                .yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.optical
                .channel.ConfigBuilder();
        configBuilder.setTargetOutputPower(input.getTargetOutputPower());
        configBuilder.setOperationalMode(input.getOperationalMode().getUint16());
        if (input.getCenterFreq() != null) { //input.getCenterFreq().getValue().intValue()
            configBuilder.setFrequency(FrequencyType
                    .getDefaultInstance(String.valueOf((long) (input.getCenterFreq().getValue()
                            .doubleValue() * 1_000_000))));
        }
        OpticalChannel opticalChannel = new OpticalChannelBuilder().setConfig(configBuilder.build()).build();
        Component1Builder component1Builder = new Component1Builder().setOpticalChannel(opticalChannel);
        Set<String> supportedOpticalChannels = portMap.getOpenconfigInfo().getSupportedOpticalChannels();
        List<ComponentBuilder> componentBuilderList = new ArrayList<>();
        if (supportedOpticalChannels != null) {
            for (String supportedOpticalChannel : supportedOpticalChannels) {
                ComponentBuilder componentBuilder = new ComponentBuilder().addAugmentation(component1Builder.build());
                componentBuilder.setName(supportedOpticalChannel);
                componentBuilderList.add(componentBuilder);
            }
        }
        return componentBuilderList;
    }

    /**
     * This method is used to disable/enable optical output of client optical-channel.
     *
     * @param nodeId Node ID
     * @param logicalConnPoint Logical connection point / termination point of respective optical port
     * @param enableState Enable/disable
     *
     * @return client optical channels configured
     * @throws OpenConfigInterfacesException openConfig exception with successfully configured interface list.
     */

    public Set<String> configureInterface(String nodeId, String logicalConnPoint, boolean enableState)
            throws OpenConfigInterfacesException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        List<InterfaceBuilder> clientInterfaceBuilders = getClientInterfaceBuilders(mapping.getOpenconfigInfo()
                        .getSupportedInterfaces(), enableState);
        Set<String> clientInterfaces = new HashSet<>();
        for (InterfaceBuilder clientInterfaceBuilder : clientInterfaceBuilders) {
            try {
                openConfigInterfaces.configureInterface(nodeId, clientInterfaceBuilder);
                clientInterfaces.add(clientInterfaceBuilder.getName());
            } catch (OpenConfigInterfacesException e) {
                throw new OpenConfigInterfacesException(clientInterfaces, e);
            }
        }
        return clientInterfaces;
    }

    /**
     * This method is used during roll back of optical output of client optical-channel.
     *
     * @param nodeId Node ID
     * @param interfaceIds Optical channels to be roll backed
     * @param enableState Enable/disable
     *
     * @return client optical channels rolled back
     * @throws OpenConfigInterfacesException openConfig exception.
     */

    public Set<String> configureInterface(String nodeId, Set<String> interfaceIds, boolean enableState)
            throws OpenConfigInterfacesException {
        List<InterfaceBuilder> clientInterfaceBuilders = getClientInterfaceBuilders(interfaceIds, enableState);
        Set<String> clientInterfaces = new HashSet<>();
        for (InterfaceBuilder clientInterfaceBuilder : clientInterfaceBuilders) {
            openConfigInterfaces.configureInterface(nodeId, clientInterfaceBuilder);
            clientInterfaces.add(clientInterfaceBuilder.getName());
        }
        return clientInterfaces;
    }

    /**
     * This method generated the InterfaceBuilder used to configuring optical output of client optical-channel.
     *
     * @param supportedInterfaces Optical channels to be configured
     * @param enableState enable/disable
     *
     * @return InterfaceBuilders for configuring optical output of client optical-channel
     */

    public List<InterfaceBuilder> getClientInterfaceBuilders(Set<String> supportedInterfaces, boolean enableState) {
        ArrayList<InterfaceBuilder> interfaceBuilderList = new ArrayList<>();
        if (supportedInterfaces != null) {
            for (String supportedInterface : supportedInterfaces) {
                InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
                interfaceBuilder.setName(supportedInterface);
                org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.interfaces
                        ._interface.ConfigBuilder configBuilder = new org.opendaylight.yang.gen.v1.http.openconfig.net
                        .yang.interfaces.rev210406.interfaces.top.interfaces._interface.ConfigBuilder();
                configBuilder.setName(supportedInterface);
                configBuilder.setType(EthernetCsmacd.VALUE);//mandatory parameter
                configBuilder.setEnabled(enableState);
                interfaceBuilder.setConfig(configBuilder.build());
                interfaceBuilderList.add(interfaceBuilder);
            }
        }
        return interfaceBuilderList;
    }
}
