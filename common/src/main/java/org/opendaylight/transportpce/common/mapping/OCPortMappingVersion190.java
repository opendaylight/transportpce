/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.opendaylight.transportpce.common.StringConstants.BIDIRECTIONAL;
import static org.opendaylight.transportpce.common.StringConstants.CHASSIS;
import static org.opendaylight.transportpce.common.StringConstants.LINECARD;
import static org.opendaylight.transportpce.common.StringConstants.OPERATINGSYSTEM;
import static org.opendaylight.transportpce.common.StringConstants.PORT;
import static org.opendaylight.transportpce.common.StringConstants.SWITCH;
import static org.opendaylight.transportpce.common.StringConstants.TERMINALCLIENT;
import static org.opendaylight.transportpce.common.StringConstants.TERMINALLINE;
import static org.opendaylight.transportpce.common.StringConstants.TRANSCEIVER;
import static org.opendaylight.transportpce.common.StringConstants.XPDR_MCPROFILE;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.metadata.OCMetaDataTransaction;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.OpenconfigPlatformData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.PlatformComponentState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.anchors.top.Port;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.Components;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.Component;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.ComponentKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.component.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.Subcomponents;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.subcomponents.Subcomponent;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.subcomponents.SubcomponentKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.OpenTerminalMetaData;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.LineCard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.LineCard.XpdrType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.LineCardKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SupportedPort;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SupportedPortKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SwitchFabric;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SwitchFabricKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card._switch.fabric.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card._switch.fabric.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.Transceiver;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.TransceiverKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.operational.modes.OperationalMode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.supported._interface.capability.InterfaceSequence;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.supported._interface.capability.InterfaceSequenceKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.NetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.NodeDatamodelType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.OpenconfigNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mc.capabilities.McCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.switching.pool.lcp.SwitchingPoolLcp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.switching.pool.lcp.SwitchingPoolLcpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.switching.pool.lcp.SwitchingPoolLcpKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.switching.pool.lcp.switching.pool.lcp.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class related to  port mapping  operations for openConfig node.
 * Based on terminal device reference 1.9.0.
 */
public class OCPortMappingVersion190 {

    private static final Logger LOG = LoggerFactory.getLogger(OCPortMappingVersion190.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private  final OCMetaDataTransaction ocMetaDataTransaction;
    private final NetworkTransactionService networkTransactionService;


    /**
     * constructor of OCPortMappingVersion190.
     * @param dataBroker
     *            data broker
     * @param deviceTransactionManager
     *            deviceTransactionManager
     */
    public OCPortMappingVersion190(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
                                   OCMetaDataTransaction ocMetaDataTransaction,
                                   NetworkTransactionService networkTransactionService) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.ocMetaDataTransaction = ocMetaDataTransaction;
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * This method creates port mapping data for a given device.
     * @param nodeId - input
     *            node ID
     * @param ipAddress - input
     *            ipaddress
     * @return true/false based on status of operation
     */
    public boolean createMappingData(String nodeId, IpAddress ipAddress) {
        LOG.info(PortMappingUtils.CREATE_OC_MAPPING_DATA_LOGMSG, nodeId,"1.9.0");
        NodeInfo nodeInfo = null;
        List<Mapping> portMapList = new ArrayList<>();
        Map<McCapabilitiesKey, McCapabilities> mcCapabilities = new HashMap<>();
        DataObjectIdentifier<Components> componentIId =
                DataObjectIdentifier.builderOfInherited(OpenconfigPlatformData.class, Components.class).build();
        var componentOptional =
                this.deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
                        componentIId, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (componentOptional.isPresent()) {
            Map<ComponentKey, Component> componentMap = componentOptional.orElseThrow().getComponent();
            if (componentMap == null) {
                LOG.error("Component data not found in device for node {}", nodeId);
                return false;
            }
            // Filtering to remove all components that are not required for port-mapping discovery
            List<Component> componentList =
                    componentMap.values().stream().filter(component -> checkComponentType(component, CHASSIS)
                            || checkComponentType(component, LINECARD) || checkComponentType(component, TRANSCEIVER)
                            || checkComponentType(component, PORT) || checkComponentType(component, OPERATINGSYSTEM))
                            .toList();
            var componentSoftwareVersion =
                    componentList.stream().filter(component -> checkComponentType(component, OPERATINGSYSTEM))
                            .findFirst();
            String softwareVersion = null;
            if (componentSoftwareVersion.isPresent()) {
                softwareVersion = componentSoftwareVersion.orElseThrow().getState().getSoftwareVersion();
            }
            var chassisComponent =
                    componentList.stream().filter(component
                            -> (checkComponentType(component, CHASSIS))).findFirst();
            if (chassisComponent.isPresent()) {
                nodeInfo = createNodeInfo(ipAddress , chassisComponent.orElseThrow().getState(), softwareVersion);
            }
            postPortMapping(nodeId, nodeInfo, null, null, null);
            if (!createXpdrPortMapping(nodeId, componentList, portMapList, mcCapabilities)) {
                LOG.warn(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, PortMappingUtils.CREATE, "Xponder");
                return false;
            }
        } else {
            LOG.error(PortMappingUtils.DEVICE_HAS_LOGMSG, nodeId, "no info", "components");
            return false;
        }
        postPortMapping(nodeId, nodeInfo, portMapList, null, mcCapabilities);
        LOG.info("Finished open config port mapping for XPDR {}", nodeId);
        return true;
    }

    /**
     * This method creates node info for a given device.
     * @param ipAddress - input
     *            ipaddress
     * @param state - input
     *           state
     * @param softwareVersion - input
     *            software version
     * @return NodeInfo of device
     */
    private NodeInfo createNodeInfo(IpAddress ipAddress, State state, String softwareVersion) {
        NodeInfoBuilder nodeInfoBldr = new NodeInfoBuilder()
                .setOpenconfigVersion(OpenconfigNodeVersion._190)
                .setNodeType(NodeTypes.Xpdr);
        if (ipAddress != null) {
            nodeInfoBldr.setNodeIpAddress(ipAddress);
        }
        if (state.getDescription() != null) {
            nodeInfoBldr.setNodeModel(state.getDescription());
        }
        if (state.getMfgName() != null) {
            nodeInfoBldr.setNodeVendor(state.getMfgName());
        }
        if (state.getLocation() != null) {
            nodeInfoBldr.setNodeClli(state.getLocation());
        }
        if (softwareVersion != null) {
            nodeInfoBldr.setSwVersion(softwareVersion);
        }
        return nodeInfoBldr.build();
    }

    /**
     * This method creates XPDR port mapping data for a given device.
     * @param nodeId - input
     *            node ID
     * @param componentList - input
     *            component list from device is used to for building the port mapping list
     *            for client and network ports
     * @param portMapList - output
     *           port mapping list is populated based on component list
     * @param mcCapabilities - input
     *           mc capabilities is used build capabilities for device
     * @return true/false based on status of operation
     */
    protected boolean createXpdrPortMapping(String nodeId, List<Component> componentList, List<Mapping> portMapList,
                                          Map<McCapabilitiesKey, McCapabilities> mcCapabilities) {
        List<Component> lineCardComponentList = componentList.stream()
                .filter(component -> checkComponentType(component, LINECARD))
                .toList();
        if (lineCardComponentList.isEmpty()) {
            LOG.error("No LINECARD component found for node {}", nodeId);
            return false;
        }
        OpenTerminalMetaData terminalMetaData = ocMetaDataTransaction.getXPDROpenTerminalMetaData();
        if (terminalMetaData == null) {
            LOG.error("No meta data found for node {}", nodeId);
            return false;
        }
        if (terminalMetaData.getLineCardInfo() == null || terminalMetaData.getLineCardInfo().getLineCard() == null) {
            LOG.error("No line card meta data found for node {}", nodeId);
            return false;
        }
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        Map<String, Set<String>> lcpNamingMap = null;
        Set<Float> frequencyGHzSet = new LinkedHashSet<>();
        int xpdrIndex = 1;
        Map<LineCardKey, LineCard> lineCardMap = terminalMetaData.getLineCardInfo().getLineCard();
        for (Component lineCardComponent : lineCardComponentList) {
            String lineCardName = lineCardComponent.getName();
            String partNo;
            if (lineCardComponent.getState().getPartNo() != null
                    && !lineCardComponent.getState().getPartNo().isEmpty()) {
                partNo = lineCardComponent.getState().getPartNo();
            } else {
                partNo = lineCardComponent.getState().getDescription();
            }
            List<Component> portComponentList =
                    componentList.stream().filter(component -> (checkComponentType(component, PORT)
                                    && component.getState().getParent().equalsIgnoreCase(lineCardName)))
                            .collect(Collectors.toList());
            var lineCardMetaDataEntry =
                    Objects.requireNonNull(lineCardMap).values().stream().filter(lineCard
                            -> lineCard.key().toString().contains(partNo)).findFirst();
            if (lineCardMetaDataEntry.isPresent()) {
                lcpNamingMap =
                        createNetworkLcpMapping(nodeId, portComponentList, lineCardMetaDataEntry.orElseThrow(), lcpMap,
                                mappingMap, xpdrIndex, lineCardComponent.getSubcomponents(), componentList,
                                frequencyGHzSet);
                createClientLcpMapping(nodeId, portComponentList, lcpMap, mappingMap,
                        lineCardMetaDataEntry.orElseThrow(), xpdrIndex, lcpNamingMap, componentList, frequencyGHzSet);
            } else {
                LOG.error("Line card entry doesn't exist in meta data for line card component {}", lineCardName);
            }
            xpdrIndex++;
        }
        createMcCapabilities(mcCapabilities, frequencyGHzSet, nodeId);
        mappingMap.forEach((k,v) -> portMapList.add(v));
        return true;
    }

    /**
     * This method creates mc capabilities of device.
     * @param mcCapabilities - input
     *            mcCapabilities map
     * @param frequencyGHzSet - input
     *            frequency set
     */
    protected void createMcCapabilities(Map<McCapabilitiesKey, McCapabilities> mcCapabilities,
                                      Set<Float> frequencyGHzSet, String nodeId) {
        if (!frequencyGHzSet.isEmpty()) {
            buildMcCapabilities(mcCapabilities, frequencyGHzSet.stream().toList().get(0), nodeId);
            if (frequencyGHzSet.size() > 1) {
                LOG.error("central-frequency-granularity is not the same for all operational-modes of the node");
            }
        }
    }

    /**
     * This method builds mc capabilities builder.
     * @param mcCapabilities - output
     *            mcCapabilities map
     * @param granularityFrequency - input
     *            granularity Frequency
     */
    private void buildMcCapabilities(Map<McCapabilitiesKey, McCapabilities> mcCapabilities ,
                                     Float granularityFrequency, String nodeId) {
        McCapabilitiesBuilder mcCapabilitiesBuilder =
                new McCapabilitiesBuilder().withKey(new McCapabilitiesKey(XPDR_MCPROFILE))
                        .setMcNodeName(XPDR_MCPROFILE);
        mcCapabilitiesBuilder.setCenterFreqGranularity(FrequencyGHz.getDefaultInstance(
                String.valueOf(granularityFrequency)));
        mcCapabilitiesBuilder.setSlotWidthGranularity(FrequencyGHz.getDefaultInstance(
                String.valueOf(granularityFrequency * 2)));
        mcCapabilities.put(mcCapabilitiesBuilder.key(), mcCapabilitiesBuilder.build());
        LOG.info("Finished building mc-capability profile for open config XPDR {}", nodeId);
    }

    /**
     * This method creates network lcp mapping.
     * @param nodeId - input
     *            node id of device
     * @param portComponentList - input
     *            port component list from device of type port
     * @param  lcpMap - output
     *            lcp map is used populate logical connection points for client and network ports
     * @param mappingMap - output
     *           mapping map is used to build mapping  entry for each client and network ports
     * @param lineCardMetaData - input
     *            line card info from meta data
     * @param xpdrIndex - input
     *            xpdr index
     * @param subcomponents - input
     *            sub components of line card components
     * @param componentsList - input
     *            components list
     * @param frequencyGHzSet - output
     *            set of frequencyGHz to populate central frequency
     * @return Map of network with corresponding clients in list
     */
    protected Map<String, Set<String>> createNetworkLcpMapping(String nodeId, List<Component> portComponentList,
                                                             LineCard lineCardMetaData, Map<String, String> lcpMap,
                                                             Map<String, Mapping> mappingMap, int xpdrIndex,
                                                             Subcomponents subcomponents,
                                                             List<Component> componentsList,
                                                             Set<Float> frequencyGHzSet) {
        Map<String, Set<String>> lcpNamingMap = new HashMap<>();
        List<SwitchingPoolLcp> switchingPoolList = new ArrayList<>();
        Map<String,
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.switching.pool.lcp
                        .switching.pool.lcp.NonBlockingList> nbMap = new HashMap<>();
        Map<SwitchFabricKey, SwitchFabric> switchFabricMap = lineCardMetaData.getSwitchFabric();
        List<SwitchFabric> switchFabricList = Objects.requireNonNull(switchFabricMap).values().stream().toList();
        Map<String, NonBlockingList> nblMap = new LinkedHashMap<>();
        for (SwitchFabric switchFabric : switchFabricList) {
            Map<NonBlockingListKey, NonBlockingList> switchFabricNonBlockingMap =
                    switchFabric.getNonBlockingList();
            for (Map.Entry<NonBlockingListKey, NonBlockingList> entry
                    : switchFabricNonBlockingMap.entrySet()) {
                nblMap.put(switchFabric.getSwitchFabricId() + "." + entry.getKey().getNblId(),
                        entry.getValue());
            }
        }
        Map<TransceiverKey, Transceiver> transceiverMetadataMap = getTransceiversListMetaData();
        for (Component portComponent : portComponentList) {
            Port portData = portComponent.getPort();
            Port1 augmentationPort = portData.augmentation(Port1.class);
            String portName = portComponent.getName();
            if (augmentationPort != null
                    && augmentationPort.getOpticalPort().getState()
                    .getOpticalPortType().toString().contains(TERMINALLINE)) {
                Transceiver transceiver = getTransceiverMetaData(componentsList, portComponent, transceiverMetadataMap);
                Set<SupportedIfCapability> supportedIfCapabilities = null;
                if (transceiver != null) {
                    supportedIfCapabilities = createSupportedInterfaceCapability(transceiver);
                    createCentralFrequency(transceiver, frequencyGHzSet);
                } else {
                    LOG.warn("Transceiver meta data doesn't exist for port component {}", portName);
                }
                Set<String> clientSet = new HashSet<>();
                String network;
                Map<SupportedPortKey, SupportedPort> supportedPortMap = lineCardMetaData.getSupportedPort();
                XpdrType xpdrType = lineCardMetaData.getXpdrType();
                var supportedPort =
                        supportedPortMap.values().stream().toList().stream().filter(supportPort
                                -> supportPort.getComponentName().equalsIgnoreCase(portName)).findFirst();
                if (supportedPort.isPresent()) {
                    Uint8 networkPortId = supportedPort.orElseThrow().getId();
                    createLcpMapping(nodeId, portComponent, augmentationPort, StringConstants.NETWORK_TOKEN,
                            networkPortId.intValue(), lcpMap, mappingMap, xpdrType, xpdrIndex, componentsList,
                            supportedIfCapabilities);
                    network =
                            PortMappingUtils.createXpdrLogicalConnectionPort(xpdrIndex, networkPortId.intValue(),
                                    StringConstants.NETWORK_TOKEN);
                    var nblValue =
                            Objects.requireNonNull(nblMap).values().stream().toList().stream()
                                    .filter(nonBlockingList -> Objects.requireNonNull(nonBlockingList
                                                    .getConnectablePort()).contains(networkPortId)).findFirst();
                    if (nblValue.isPresent()) {
                        Set<Uint8> connectablePorts = nblValue.orElseThrow().getConnectablePort();
                        Set<Uint8> connectablePortsSet = new HashSet<>(Objects.requireNonNull(connectablePorts));
                        Objects.requireNonNull(connectablePortsSet).remove(networkPortId);
                        List<Optional<SupportedPort>> clientPorts =
                                getSupportedClientPorts(connectablePortsSet, supportedPortMap);
                        Set<Uint8> neClientIds = clientPortsExistsOnNELineCard(clientPorts, subcomponents);
                        for (Uint8 unit8 : neClientIds) {
                            clientSet.add(PortMappingUtils.createXpdrLogicalConnectionPort(xpdrIndex,
                                    unit8.intValue(), StringConstants.CLIENT_TOKEN));
                        }
                        createSwitchingPool(mappingMap, nblValue.orElseThrow(), xpdrType, clientSet, network, nbMap,
                                nblMap, networkPortId);
                        lcpNamingMap.put(network, clientSet);
                    }
                }
            }
        }
        for (SwitchFabric switchFabric : switchFabricList) {
            Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115
                    .switching.pool.lcp.switching.pool.lcp.NonBlockingListKey,
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.switching
                            .pool.lcp.switching.pool.lcp.NonBlockingList> nonBlockingListMap = new HashMap<>();
            if (switchFabric != null && !nbMap.isEmpty()) {
                Uint8 switchFabricId = switchFabric.getSwitchFabricId();
                for (Map.Entry<String, org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                        .rev250115.switching.pool.lcp.switching.pool.lcp.NonBlockingList> entry : nbMap.entrySet()) {
                    if (entry.getKey().startsWith(switchFabricId.toString())) {
                        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                                .rev250115.switching.pool.lcp.switching.pool.lcp
                                .NonBlockingList nonBlockingList = new NonBlockingListBuilder()
                                .setNblNumber(Uint16.valueOf(entry.getKey().substring(2)))
                                .setLcpList(entry.getValue().getLcpList())
                                .build();
                        nonBlockingListMap.put(nonBlockingList.key(), nonBlockingList);
                    }
                }
                switchingPoolList.add(new SwitchingPoolLcpBuilder()
                        .setSwitchingPoolNumber(switchFabricId.toUint16())
                        .setSwitchingPoolType(SwitchingPoolTypes.valueOf(switchFabric.getSwitchFabricType().getName()))
                        .setNonBlockingList(nonBlockingListMap)
                        .build());
            }
        }
        postPortMapping(nodeId,null,null,switchingPoolList,null);
        LOG.info("Finished building network port mapping for open config XPDR {}", nodeId);
        return  lcpNamingMap;
    }

    /**
     * This Method creates supported interface capability for port.
     * @param transceiver - input
     *          metadata of the transceiver
     * @return
     *      list of the supported if capability
     */
    protected Set<SupportedIfCapability> createSupportedInterfaceCapability(Transceiver transceiver) {
        Set<SupportedIfCapability> supportedIntf = new HashSet<>();
        for (SupportedInterfaceCapability supportedInterfaceCapability :
                Objects.requireNonNull(transceiver.getSupportedInterfaceCapability())) {
            Map<InterfaceSequenceKey, InterfaceSequence> interfaceSequenceMap =
                    supportedInterfaceCapability.getInterfaceSequence();
            List<String> interfaceTypeList = new ArrayList<>();
            if (interfaceSequenceMap != null) {
                for (Map.Entry<InterfaceSequenceKey, InterfaceSequence> interfaceSequenceEntry :
                        interfaceSequenceMap.entrySet()) {
                    interfaceTypeList.add(MappingUtilsImpl.getInterfaceType(interfaceSequenceEntry.getValue()
                            .getInterfaceType().toString()));
                }
            }
            StringBuilder supportedIntfCapBuilder = new StringBuilder();
            for (String intfType : interfaceTypeList) {
                supportedIntfCapBuilder.append("-").append(intfType);
            }
            String supportedIntfCap = "if" + supportedIntfCapBuilder;
            supportedIntf.add(MappingUtilsImpl.ocConvertSupIfCapa(supportedIntfCap));
        }
        return supportedIntf;
    }

    /**
     * This Method checks Supported Client Ports from Metadata on NE LineCard.
     * @param supportedClientPorts - input
     *                 supported client ports from metadata.
     * @param subcomponents - input
     *                 subcomponents of NE LineCard.
     * @return supported client id's.
     */
    private Set<Uint8> clientPortsExistsOnNELineCard(List<Optional<SupportedPort>> supportedClientPorts,
                                                     Subcomponents subcomponents) {
        Set<Uint8> clientIds = new HashSet<>();
        Collection<Subcomponent> lineCardComponentSubcomponents =
                Objects.requireNonNull(subcomponents.getSubcomponent()).values();
        for (var supportedPort : supportedClientPorts) {
            String clientPortName = supportedPort.orElseThrow().getComponentName();
            var subComp =
                    lineCardComponentSubcomponents.stream().filter(subcomponent -> subcomponent.getName()
                            .equals(clientPortName)).findFirst();
            if (subComp.isPresent()) {
                clientIds.add(supportedPort.orElseThrow().getId());
            } else {
                LOG.info("Client Port doesn't exist in NE Line Card {} ", clientPortName);
            }
        }
        return clientIds;
    }

    /**
     * This Method is used to get Supported Client Ports from metadata based on connectable port id's.
     * @param connectablePorts - input
     *                connectable ports from non-blocking list.
     * @param supportedPortMap - input
     *                supported ports from metadata.
     * @return supported objects from metadata.
     */
    public List<Optional<SupportedPort>> getSupportedClientPorts(Set<Uint8> connectablePorts, Map<SupportedPortKey,
            SupportedPort> supportedPortMap) {
        List<Optional<SupportedPort>> supportedClientPorts = new ArrayList<>();
        for (Uint8 uint8 : connectablePorts) {
            var supportedPort =
                    supportedPortMap.values().stream().filter(clientPort -> clientPort.getId().equals(uint8)
                                    && clientPort.getType().toString().contains(TERMINALCLIENT))
                            .findFirst();
            supportedClientPorts.add(supportedPort);
        }
        return supportedClientPorts;
    }

    /**
     * This Method is used to create switching pool for Network ports on Line Card.
     * inputs of this method
     * @param nblVal - input
     *           non-blocking entry of the port.
     * @param xpdrType - input
     *           xpdr type of line card entry from metadata.
     * @param clientList - input
     *           lcp names of clients.
     * @param network - input
     *           lcp name of network.
     * @param nblMap - input
     *           non-blocking list map.
     * @param networkPortId - input
     *            network port id of network port.
     * @param nbMap - output
     *           non-blocking map.
     * @param mappingMap - output
     *             mapping entries of the ports.
     */
    private void createSwitchingPool(Map<String, Mapping> mappingMap,
                                     NonBlockingList nblVal, XpdrType xpdrType, Set<String> clientList, String network,
                                     Map<String,
                                             org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                                                     .portmapping.rev250115.switching.pool.lcp.switching.pool
                                                     .lcp.NonBlockingList> nbMap, Map<String, NonBlockingList> nblMap,
                                     Uint8 networkPortId) {
        if (xpdrType.getName().equalsIgnoreCase(XpdrType.MPDR.getName()) || xpdrType.getName()
                .equalsIgnoreCase(XpdrType.SPDR.getName())) {
            clientList.add(network);
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                    .rev250115.switching.pool.lcp.switching.pool.lcp
                    .NonBlockingList nonBlockingList = new NonBlockingListBuilder()
                    .setNblNumber(nblVal.getNblId().toUint16())
                    .setLcpList(clientList)
                    .build();
            String key = null;
            for (Map.Entry<String, NonBlockingList> entry: nblMap.entrySet()) {
                if (entry.getValue().getConnectablePort().contains(networkPortId)) {
                    key = entry.getKey();
                }
            }
            nbMap.put(key, nonBlockingList);
        } else if (xpdrType.getName().equalsIgnoreCase(XpdrType.TPDR.getName())) {
            if (mappingMap.containsKey(network)) {
                Mapping mapping = mappingMap.get(network);
                String clientConnectionMapLcp = clientList.stream().findFirst().orElseThrow();
                mappingMap.put(network, createXpdrMappingObject(null, null, null,
                        null, null, mapping, clientConnectionMapLcp,
                        null, null,null));
            }
        }
    }

    /**
     * This method creates client lcp mapping.
     * inputs of this method
     * @param nodeId - input
     *            node id of device
     * @param portComponentList - input
     *             port component list from device of type port
     * @param  lcpMap - output
     *           lcp map is used to populate logical connection point for client and network ports
     * @param mappingMap - output
     *           mapping map is used to build mapping entry for each client and network ports
     * @param lineCardMetaData - input
     *            line card meta data from metadata
     * @param xpdrIndex - input
     *            xpdr index
     * @param lcpNamingMap - input
     *           lcp naming map
     * @param componentsList - input
     *           component list
     * @param frequencyGHzSet - output
     *           set of frequencyGHz to populate central frequency
     */
    protected void createClientLcpMapping(String nodeId, List<Component> portComponentList,  Map<String, String> lcpMap,
                                        Map<String, Mapping> mappingMap, LineCard lineCardMetaData,int xpdrIndex,
                                        Map<String, Set<String>> lcpNamingMap, List<Component> componentsList,
                                        Set<Float> frequencyGHzSet) {
        Map<TransceiverKey, Transceiver> transceiverMetadataMap = getTransceiversListMetaData();
        for (Component portComponent : portComponentList) {
            Port portData = portComponent.getPort();
            Port1 augmentationPort = portData.augmentation(Port1.class);
            String portName = portComponent.getName();
            if (augmentationPort != null
                    && augmentationPort.getOpticalPort().getState().getOpticalPortType()
                    .toString().contains(TERMINALCLIENT)) {
                Transceiver transceiver = getTransceiverMetaData(componentsList, portComponent, transceiverMetadataMap);
                LineCard.XpdrType xpdrType = lineCardMetaData.getXpdrType();
                Map<SupportedPortKey, SupportedPort>  supportedPortMap = lineCardMetaData.getSupportedPort();
                List<SupportedPort> supportedPortList = supportedPortMap.values().stream().toList();
                var supportedPortOptional =
                        supportedPortList.stream().filter(supportedPort -> supportedPort.getComponentName()
                                        .equals(portName) && supportedPort.getType().toString()
                                        .contains(TERMINALCLIENT)).findFirst();
                if (supportedPortOptional.isPresent()) {
                    Uint8 supportedId = supportedPortOptional.orElseThrow().getId();
                    String clientLCPName =
                            PortMappingUtils.createXpdrLogicalConnectionPort(xpdrIndex, supportedId.intValue(),
                                    StringConstants.CLIENT_TOKEN);
                    Set<SupportedIfCapability> supportedIfCapabilities = null;
                    if (transceiver != null) {
                        supportedIfCapabilities = createSupportedInterfaceCapability(transceiver);
                        createCentralFrequency(transceiver, frequencyGHzSet);
                    } else {
                        LOG.warn("Transceiver meta data doesn't exist for port component {}", portName);
                    }
                    createLcpMapping(nodeId, portComponent, augmentationPort, StringConstants.CLIENT_TOKEN,
                            supportedId.intValue(), lcpMap, mappingMap, xpdrType, xpdrIndex, componentsList,
                            supportedIfCapabilities);
                    if (xpdrType.getName().equalsIgnoreCase(XpdrType.TPDR.getName())) {
                        String networkConnectionMapLcp = null;
                        for (Map.Entry<String, Set<String>> stringSetEntry : lcpNamingMap.entrySet()) {
                            Set<String> strings = stringSetEntry.getValue();
                            if (strings.contains(clientLCPName)) {
                                networkConnectionMapLcp = stringSetEntry.getKey();
                                break;
                            }
                        }
                        if (mappingMap.containsKey(clientLCPName) && networkConnectionMapLcp != null) {
                            Mapping mapping = mappingMap.get(clientLCPName);
                            mappingMap.put(clientLCPName, createXpdrMappingObject(null, null,
                                    null, null, null, mapping,
                                    networkConnectionMapLcp, null, null, null));
                        }
                    }
                }
            }
        }
        LOG.info("Finished building client port mapping for open config XPDR {}", nodeId);
    }

    /**
     * This method creates lcp mapping.
     * @param nodeId - input
     *            node id of device
     * @param portComponent - input
     *            port component  from device of type port
     * @param  augmentationPort - input
     *            augmentation port of optical port from device
     * @param token - input
     *           network/client type token
     * @param lcpValue - input
     *            index value for network/client port
     * @param lcpMap - input
     *            lcpMap is used for logical connection point for client and network ports
     * @param mappingMap - output
     *            mapping map is used to build mapping entry for each client and network ports
     * @param xpdrType - input
     *            xpdr type
     * @param xpdrIndex - input
     *           xpdr index
     * @param componentsList - input
     *          components List
     * @param supportedIfCapabilities - input
     *           supported if capabilities
     */
    protected void createLcpMapping(String nodeId, Component portComponent, Port1 augmentationPort, String token,
                                  int lcpValue, Map<String, String> lcpMap, Map<String, Mapping> mappingMap,
                                  XpdrType xpdrType, int xpdrIndex, List<Component> componentsList,
                                  Set<SupportedIfCapability> supportedIfCapabilities) {
        String lcpName = PortMappingUtils.createXpdrLogicalConnectionPort(xpdrIndex, lcpValue, token);
        String supportingCircuitPackName = getSupportingCircuitPackName(componentsList, portComponent);
        lcpMap.put(supportingCircuitPackName + '+' + portComponent.getName(), lcpName);
        mappingMap.put(lcpName, createXpdrMappingObject(nodeId, portComponent, augmentationPort,
                supportingCircuitPackName, lcpName, null, null, xpdrType,
                supportedIfCapabilities, token));
    }

    /**
     * This method is to get  supporting circuit packname.
     * @param componentsList - input
     *            components list from device
     * @param portComponent - input
     *            port component from device of type PORT
     * @return String subComponent name
     */
    private String getSupportingCircuitPackName(List<Component> componentsList, Component portComponent) {
        List<Component> transceiverComponentList =
                componentsList.stream().filter(component -> (checkComponentType(component,
                                TRANSCEIVER) && component.getState().getParent()
                                .equalsIgnoreCase(portComponent.getName()))).toList();
        Map<SubcomponentKey, Subcomponent> portSubComponentsMap = portComponent.getSubcomponents().getSubcomponent();
        for (Map.Entry<SubcomponentKey, Subcomponent> entry : Objects.requireNonNull(portSubComponentsMap).entrySet()) {
            String portSubCompName = entry.getKey().getName().toString();
            return transceiverComponentList.stream().filter(component -> component.getName()
                    .contains(portSubCompName)).findFirst().orElseThrow().getName();
        }
        return null;
    }

    /**
     * This method retrieves the list of transceivers declared in the metadata stored in the MD-SAL.
     */
    protected Map<TransceiverKey, Transceiver> getTransceiversListMetaData() {
        return ocMetaDataTransaction.getXPDROpenTerminalMetaData().getTransceiverInfo().getTransceiver();
    }

    /**
     * This method is to get  transceiver metadata from MD-SAL.
     * @param componentsList - input
     *            components list from device
     * @param portComponent - input
     *            port component from device of type PORT
     * @param transceiverMap - input
     *            list of Transceivers declared in the meta-data file
     * @return Transceiver object from metadata
     */
    protected Transceiver getTransceiverMetaData(List<Component> componentsList, Component portComponent,
            Map<TransceiverKey, Transceiver> transceiverMap) {
        Optional<Transceiver> transceiverMetadata;
        List<Component> transceiverComponentList =
                componentsList.stream().filter(component -> (checkComponentType(component,
                        TRANSCEIVER) && component.getState().getParent().contains(portComponent.getName()))).toList();
        Map<SubcomponentKey, Subcomponent> portSubComponentsMap = portComponent.getSubcomponents().getSubcomponent();
        for (Map.Entry<SubcomponentKey, Subcomponent> entry : portSubComponentsMap.entrySet()) {
            String portSubCompName = entry.getKey().getName().toString();
            var transceiverComponent =
                    transceiverComponentList.stream().filter(component -> component.getName()
                            .equalsIgnoreCase(portSubCompName)).findFirst();
            String transceiverPartNo;
            if (transceiverComponent.orElseThrow().getState().getPartNo() != null
                    && !transceiverComponent.orElseThrow().getState().getPartNo().isEmpty()) {
                transceiverPartNo = transceiverComponent.orElseThrow().getState().getPartNo();
            } else {
                transceiverPartNo = transceiverComponent.orElseThrow().getState().getDescription();
            }
            transceiverMetadata =
                    Objects.requireNonNull(transceiverMap).values().stream().filter(transceiver
                            -> transceiver.key().getPartNo().equalsIgnoreCase(transceiverPartNo)).findFirst();
            if (transceiverMetadata.isPresent()) {
                return transceiverMetadata.orElseThrow();
            }
        }
        return null;
    }

    /**
     * This method is to check  component type present in device or not and that it matches the specified type.
     * @param component - input
     *             component
     * @param componentType - input
     *             component type
     * @return true/false
     */
    protected boolean checkComponentType(Component component , String componentType) {
        try {
            if (component.getState() != null) {
                PlatformComponentState.Type type = component.getState().getType();
                if (type != null && type.getOPENCONFIGHARDWARECOMPONENT() != null
                        && type.getOPENCONFIGHARDWARECOMPONENT().toString().contains(componentType)) {
                    return true;
                } else if (type != null && type.getOPENCONFIGSOFTWARECOMPONENT() != null
                        && type.getOPENCONFIGSOFTWARECOMPONENT().toString().contains(componentType)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException exception) {
            LOG.error("state container doesn't exist for component {}", component.getName());
        }
        return false;
    }

    /**
     * This method retrieves central frequency granularity for operational modes.
     * We read the list of supported operational-modes from the NE. For each operational-mode,
     * we consult the specific operational modes catalog and get the CF granularity from the catalog entry.
     * Ideally all modes will have the same granularity.
     * We will use this value as CF granularity and double it to get the slot-width granularity.
     * inputs of this method
     * @param transceiver - input
     *            transceiver object of meta data
     * @param frequencySet - output
     *            frequency set is used populate central frequency by operational mode id
     */
    protected void createCentralFrequency(Transceiver transceiver , Set<Float> frequencySet) {
        CatalogUtils catalogUtils = new CatalogUtils(networkTransactionService);
        if (transceiver.getOperationalModes() != null && transceiver.getOperationalModes().getOperationalMode()
                != null) {
            List<OperationalMode> operationalModeList =
                    Objects.requireNonNull(transceiver.getOperationalModes().getOperationalMode()).values()
                            .stream().toList();
            for (OperationalMode operationalMode : operationalModeList) {
                String centerFreqGranularity = catalogUtils.getCFGranularity(operationalMode.getCatalogId());
                if (centerFreqGranularity != null) {
                    frequencySet.add(Float.parseFloat(centerFreqGranularity));
                }
            }
        } else {
            LOG.error("Operational mode does not exist for Transceiver in metadata");
        }
    }

    /**
     * This method is to update xpdr mapping with connection map lcp.
     * @param nodeId - input
     *            node id of device
     * @param portComponent - input
     *            port component  from device of type port
     * @param  augmentationPort - input
     *            augmentation port of optical port from device
     * @param supportingCircuitPackName - input
     *           supporting circuit pack name
     * @param logicalConnectionPoint - input
     *            logicalConnectionPoint
     * @param mapping - output
     *            mapping map
     * @param connectionMapLcp - input
     *            connection map lcp
     * @param xpdrType - input
     *            xpdrType
     * @param supportedIfCapabilities - input
     *            supported interface capabilities
     * @param token - input
     *           token whether client/network
     * @return Mapping based on status of operation
     */
    private Mapping createXpdrMappingObject(String nodeId, Component portComponent, Port1 augmentationPort,
                                            String supportingCircuitPackName, String logicalConnectionPoint,
                                            Mapping mapping, String connectionMapLcp, XpdrType xpdrType,
                                            Set<SupportedIfCapability> supportedIfCapabilities, String token) {
        if (mapping != null && connectionMapLcp != null) {
            // update existing mapping
            return new MappingBuilder(mapping).setConnectionMapLcp(connectionMapLcp).build();
        }
        return createNewXpdrMapping(nodeId, portComponent, augmentationPort,
                supportingCircuitPackName, logicalConnectionPoint, xpdrType, supportedIfCapabilities, token);
    }

    /**
     * This method to build a new xpdr mapping object.
     * @param nodeId - input
     *            node id of device
     * @param portComponent - input
     *            port component  from device of type port
     * @param  augmentationPort - input
     *            augmentation port of optical port from device
     * @param supportingCircuitPackName -  input
     *           supportingCircuitPackName
     * @param logicalConnectionPoint - input
     *            logicalConnectionPoint
     * @param xpdrType - input
     *            xpdrType
     * @param supportedIfCapabilities - input
     *           supported interface capabilities
     * @param token - input
     *          token whether client/network
     * @return Mapping based on status of operation
     */
    private Mapping createNewXpdrMapping(String nodeId, Component portComponent, Port1 augmentationPort,
                                         String supportingCircuitPackName, String logicalConnectionPoint,
                                         XpdrType xpdrType, Set<SupportedIfCapability> supportedIfCapabilities,
                                         String token) {
        MappingBuilder mpBldr = new MappingBuilder()
                .withKey(new MappingKey(logicalConnectionPoint))
                .setLogicalConnectionPoint(logicalConnectionPoint)
                .setSupportingCircuitPackName(supportingCircuitPackName)
                .setSupportingPort(portComponent.getName())
                .setPortDirection(BIDIRECTIONAL)
                .setLcpHashVal(PortMappingUtils.fnv1size64(nodeId + "-" + logicalConnectionPoint));
        if (augmentationPort.getOpticalPort().getState().getAdminState() != null) {
            mpBldr.setPortAdminState(augmentationPort.getOpticalPort().getState().getAdminState().getName());
        } else if (augmentationPort.getOpticalPort().getConfig().getAdminState() != null) {
            mpBldr.setPortAdminState(augmentationPort.getOpticalPort().getConfig().getAdminState().getName());
        }
        if (xpdrType != null &&  (XpdrNodeTypes.Mpdr.toString().equalsIgnoreCase(xpdrType.toString())
                || XpdrNodeTypes.Switch.toString().equalsIgnoreCase(xpdrType.toString()))) {
            mpBldr.setPortQual(SWITCH + "-" + token.toLowerCase(Locale.ENGLISH));
        } else if (xpdrType != null && XpdrNodeTypes.Tpdr.toString().equalsIgnoreCase(xpdrType.toString())) {
            mpBldr.setPortQual(NodeTypes.Xpdr.getName() + "-" + token.toLowerCase(Locale.ENGLISH));
        }
        if (portComponent.getState().getOperStatus() != null) {
            mpBldr.setPortOperState(portComponent.getState().getOperStatus().implementedInterface().getSimpleName());
        }
        if (xpdrType != null && XpdrNodeTypes.Mpdr.toString().equalsIgnoreCase(xpdrType.toString())) {
            mpBldr.setXpdrType(XpdrNodeTypes.Mpdr);
        } else if (xpdrType != null && XpdrNodeTypes.Tpdr.toString().equalsIgnoreCase(xpdrType.toString())) {
            mpBldr.setXpdrType(XpdrNodeTypes.Tpdr);
        }
        if (supportedIfCapabilities != null) {
            mpBldr.setSupportedInterfaceCapability(supportedIfCapabilities);
        }
        return mpBldr.build();
    }

    /**
     * This method to write port mapping data into MD-SAL.
     * @param nodeId - input
     *            node id
     * @param nodeInfo - input
     *            node info
     * @param  portMapList - input
     *            port mapping list
     * @param splList - input
     *            switching pool lcp list
     * @param mcCapMap - input
     *            mc capabilites
     * @return true/false based on status of operation
     */
    private boolean postPortMapping(String nodeId, NodeInfo nodeInfo, List<Mapping> portMapList,
                                    List<SwitchingPoolLcp> splList, Map<McCapabilitiesKey, McCapabilities> mcCapMap) {
        NodesBuilder nodesBldr = new NodesBuilder().withKey(new NodesKey(nodeId)).setNodeId(nodeId);
        if (nodeInfo != null) {
            nodesBldr.setDatamodelType(NodeDatamodelType.OPENCONFIG);
            nodesBldr.setNodeInfo(nodeInfo);
        }
        if (portMapList != null && !portMapList.isEmpty()) {
            Map<MappingKey, Mapping> mappingMap = new HashMap<>();
            // No element in the list below should be null at this stage
            for (Mapping mapping: portMapList) {
                mappingMap.put(mapping.key(), mapping);
            }
            nodesBldr.setMapping(mappingMap);
        }
        if (splList != null) {
            Map<SwitchingPoolLcpKey,SwitchingPoolLcp> splMap = new HashMap<>();
            // No element in the list below should be null at this stage
            for (SwitchingPoolLcp spl: splList) {
                splMap.put(spl.key(), spl);
            }
            nodesBldr.setSwitchingPoolLcp(splMap);
        }
        if (mcCapMap != null && !mcCapMap.isEmpty()) {
            nodesBldr.setMcCapabilities(mcCapMap);
        }
        Map<NodesKey, Nodes> nodesList = new HashMap<>();
        Nodes nodes = nodesBldr.build();
        nodesList.put(nodes.key(),nodes);
        Network network = new NetworkBuilder().setNodes(nodesList).build();
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        DataObjectIdentifier<Network> nodesIID = DataObjectIdentifier.builder(Network.class).build();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodesIID, network);
        FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();
        try {
            commit.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn(PortMappingUtils.PORTMAPPING_POST_FAIL_LOGMSG, nodeId, network, e);
            return false;
        }
    }
}
