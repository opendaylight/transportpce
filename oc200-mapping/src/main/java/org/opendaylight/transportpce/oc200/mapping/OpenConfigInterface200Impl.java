/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.oc200.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaceConfiguration;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.interfaces.top.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.PlatformComponentPropertiesConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.anchors.top.PortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.properties.top.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.properties.top.properties.Property;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.properties.top.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.properties.top.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210.physical.channel.top.PhysicalChannelsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210.physical.channel.top.physical.channels.Channel;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210.physical.channel.top.physical.channels.ChannelBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210.physical.channel.top.physical.channels.ChannelKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210.port.transceiver.top.TransceiverBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.Component1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.OpticalChannelBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.OpticalPortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.optical.port.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.optical.port.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev230208.AdminStateType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev230208.FrequencyType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260908.mapping.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev230126.EthernetCsmacd;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenConfigInterface200Impl - OSGI service implementation for OC2.0.0 devices.
 * Handles OpenConfig component and interface configuration with platform@2022-12-20 and terminal-device@2021-07-29.
 * Registered with version property "2.0.0" for dynamic discovery by renderer via OSGI service registry.
 */
@Component(service = OpenConfigInterfaceConfiguration.class, property = "openconfig.version=2.0.0")
public class OpenConfigInterface200Impl implements OpenConfigInterfaceConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigInterface200Impl.class);
    private static final String VERSION = "2.0.0";

    private final PortMapping portMapping;
    private final OpenConfigInterfaces openConfigInterfaces;

    @Activate
    public OpenConfigInterface200Impl(
            @Reference(cardinality = ReferenceCardinality.OPTIONAL) PortMapping portMapping,
            @Reference(cardinality = ReferenceCardinality.OPTIONAL)
            OpenConfigInterfaces openConfigInterfaces) {
        this.portMapping = portMapping;
        this.openConfigInterfaces = openConfigInterfaces;
        LOG.info("OpenConfigInterface200Impl OSGI service activated for OC version {}", VERSION);
    }

    @Override
    public String getOpenConfigVersion() {
        return VERSION;
    }

    @Override
    public Set<String> configurePortAdminState(String nodeId, String logicalConnPoint, Object adminStateType)
            throws OpenConfigInterfacesException {
        LOG.info("Configuring OpenConfig port admin state to {} on node {}",
                (adminStateType != null ? adminStateType.toString() : "null"), nodeId);
        Set<String> portIds = new HashSet<>();
        try {
            Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
            ComponentBuilder portComponentBuilder =
                    createPortComponentBuilder(mapping.getSupportingPort(),
                            (AdminStateType) adminStateType);
            openConfigInterfaces.configureComponent(nodeId, portComponentBuilder);
            portIds.add(portComponentBuilder.getName());
            LOG.info("Successfully updated port admin state to {} at port {} on node {}",
                    adminStateType, mapping.getSupportingPort(), nodeId);
        } catch (OpenConfigInterfacesException e) {
            throw new OpenConfigInterfacesException(portIds, e);
        }
        return portIds;
    }

    @Override
    public Set<String> disablePortAdminState(String nodeId, String supportingPort) throws
            OpenConfigInterfacesException {
        LOG.info("Disabling OpenConfig port admin state at port {} on node {}", supportingPort, nodeId);
        Set<String> portIds = new HashSet<>();
        ComponentBuilder portComponentBuilder = createPortComponentBuilder(supportingPort, AdminStateType.DISABLED);
        openConfigInterfaces.configureComponent(nodeId, portComponentBuilder);
        portIds.add(portComponentBuilder.getName());
        LOG.info("Successfully updated port admin state to {} at port {} on node {} ",
                AdminStateType.DISABLED.getName(), supportingPort, nodeId);
        return portIds;
    }

    @Override
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

    @Override
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

    private List<ComponentBuilder> getClientOpticalChannelBuilders(Set<String> supportedOpticalChannels,
                                                                   String componentProperty) {
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.properties.top
                .properties.property.ConfigBuilder configBuilder = new org.opendaylight.yang.gen.v1.http.openconfig
                .net.yang.platform.rev221220.platform.component.properties.top.properties.property.ConfigBuilder();
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

    @Override
    public String configureNetworkOpticalChannel(String nodeId, String logicalConnPoint, Object input)
            throws OpenConfigInterfacesException {
        ServicePathInput servicePathInput = (ServicePathInput) input;
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        boolean isAend = false;
        if (servicePathInput.getAEndApiInfo() != null && servicePathInput.getAEndApiInfo().getNodeId() != null) {
            isAend = servicePathInput.getAEndApiInfo().getNodeId().equals(nodeId);
        }
        LOG.info("Configuring operational mode and central frequency for {} on node {}",
                mapping.getSupportingPort(), nodeId);
        List<ComponentBuilder> opticalChannelBuilders =
                createOpticalChannelComponentBuilder(mapping, servicePathInput, isAend);
        StringBuilder opticalChannelComponentNames = new StringBuilder();
        for (ComponentBuilder opticalChannelBuilder : opticalChannelBuilders) {
            openConfigInterfaces.configureComponent(nodeId, opticalChannelBuilder);
            opticalChannelComponentNames.append(opticalChannelBuilder.getName()).append(" ");
        }
        LOG.info("Successfully configured operational mode and central frequency for {} on node {}",
                opticalChannelComponentNames, nodeId);
        return opticalChannelComponentNames.toString();
    }

    @Override
    public String configureTransceiversTxLaser(String nodeId, String logicalConnPoint,
                    List<Integer> channelIndexes, boolean isTxLaserEnabled) throws OpenConfigInterfacesException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        LOG.info("Configuring OpenConfig tx-laser as {} on the channels {} for transceiver {} on node {}",
                isTxLaserEnabled, channelIndexes.toString(), mapping.getSupportingCircuitPackName(), nodeId);
        ComponentBuilder lineTransceiverComponentBuilder = createTransceiverComponentBuilder(
                mapping.getSupportingCircuitPackName(), channelIndexes, isTxLaserEnabled);
        openConfigInterfaces.configureComponent(nodeId, lineTransceiverComponentBuilder);
        LOG.info("Successfully configured OpenConfig tx-laser for transceiver {} on node {}",
                mapping.getSupportingCircuitPackName(), nodeId);
        return lineTransceiverComponentBuilder.getName();
    }

    @Override
    public String disableTxLaser(String nodeId, String transceiver) throws OpenConfigInterfacesException {
        LOG.info("Disabling tx-laser for transceiver {} on node {}", transceiver, nodeId);
        ComponentBuilder lineTransceiverComponentBuilder =
                createTransceiverComponentBuilder(transceiver, List.of(1), false);
        openConfigInterfaces.configureComponent(nodeId, lineTransceiverComponentBuilder);
        LOG.info("Successfully disabled tx-laser for transceiver {} on node: {}", transceiver, nodeId);
        return lineTransceiverComponentBuilder.getName();
    }

    private ComponentBuilder createTransceiverComponentBuilder(String componentName, List<Integer> channelIndexes,
                    boolean txLaserEnabled) {

        Map<ChannelKey, Channel> channelMap = new LinkedHashMap<>();
        for (Integer index : channelIndexes) {
            var channelConfig = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210
                    .physical.channel.top.physical.channels.channel.ConfigBuilder()
                    .setTxLaser(txLaserEnabled)
                    .build();
            var channel = new ChannelBuilder()
                    .setIndex(Uint16.valueOf(index))
                    .setConfig(channelConfig).build();
            channelMap.put(channel.key(), channel);
        }
        var physicalChannels = new PhysicalChannelsBuilder().setChannel(channelMap).build();
        var transceiver = new TransceiverBuilder().setPhysicalChannels(physicalChannels).build();
        var transceiverAug = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.transceiver.rev230210
                .Component1Builder().setTransceiver(transceiver).build();
        return new ComponentBuilder().setName(componentName).addAugmentation(transceiverAug);
    }

    private ComponentBuilder createPortComponentBuilder(String supportingPort, AdminStateType adminStateType) {
        Config config = new ConfigBuilder().setAdminState(adminStateType).build();
        Port1 port1 = new Port1Builder().setOpticalPort(new OpticalPortBuilder().setConfig(config).build()).build();
        PortBuilder portBuilder = new PortBuilder().addAugmentation(port1);
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.setName(supportingPort);
        componentBuilder.setPort(portBuilder.build());
        return componentBuilder;
    }

    private List<ComponentBuilder> createOpticalChannelComponentBuilder(Mapping portMap, ServicePathInput input,
        boolean isAend) {
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729
                .terminal.optical.channel.top.optical.channel.ConfigBuilder configBuilder = new org.opendaylight
                .yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.optical
                .channel.ConfigBuilder();
        Uint16 operationalMode = isAend
            ? Uint16.valueOf(input.getAendOperationalMode())
            : Uint16.valueOf(input.getZendOperationalMode());
        configBuilder.setOperationalMode(operationalMode);
        LOG.debug("Configuring OpenConfig network optical channel with operational mode {} at port {}",
                operationalMode.intValue(), portMap.getSupportingPort());
        if (input.getCenterFreq() != null) {
            FrequencyType frequency = FrequencyType
                    .getDefaultInstance(String.valueOf((long) (input.getCenterFreq().getValue()
                            .doubleValue() * 1_000_000)));
            configBuilder.setFrequency(frequency);
            LOG.debug("Configuring OpenConfig network optical channel with central frequency {} MHz at port {}",
                    frequency.getValue(), portMap.getSupportingPort());
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

    @Override
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

    @Override
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

    public List<InterfaceBuilder> getClientInterfaceBuilders(Set<String> supportedInterfaces, boolean enableState) {
        ArrayList<InterfaceBuilder> interfaceBuilderList = new ArrayList<>();
        if (supportedInterfaces != null) {
            for (String supportedInterface : supportedInterfaces) {
                InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
                interfaceBuilder.setName(supportedInterface);
                org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.interfaces.top.interfaces
                        ._interface.ConfigBuilder configBuilder = new org.opendaylight.yang.gen.v1.http.openconfig.net
                        .yang.interfaces.rev221025.interfaces.top.interfaces._interface.ConfigBuilder();
                configBuilder.setName(supportedInterface);
                configBuilder.setType(EthernetCsmacd.VALUE);
                configBuilder.setEnabled(enableState);
                interfaceBuilder.setConfig(configBuilder.build());
                interfaceBuilderList.add(interfaceBuilder);
            }
        }
        return interfaceBuilderList;
    }
}
