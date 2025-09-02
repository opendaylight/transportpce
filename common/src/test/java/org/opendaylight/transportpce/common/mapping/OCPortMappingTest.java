/*
 * Copyright © 2020 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.CLIENT_TOKEN;
import static org.opendaylight.transportpce.common.StringConstants.LINECARD;
import static org.opendaylight.transportpce.common.StringConstants.NETWORK_TOKEN;
import static org.opendaylight.transportpce.common.StringConstants.OPENCONFIG_DEVICE_VERSION_1_9_0;
import static org.opendaylight.transportpce.common.StringConstants.OPTICALCHANNEL;
import static org.opendaylight.transportpce.common.StringConstants.PORT;
import static org.opendaylight.transportpce.common.StringConstants.TERMINALCLIENT;
import static org.opendaylight.transportpce.common.StringConstants.TERMINALLINE;
import static org.opendaylight.transportpce.common.StringConstants.TRANSCEIVER;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.metadata.OCMetaDataTransaction;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.PlatformComponentState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.anchors.top.Port;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.anchors.top.PortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.Component;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.component.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.component.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.Subcomponents;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.SubcomponentsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.subcomponents.Subcomponent;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.subcomponents.SubcomponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top.subcomponents.SubcomponentKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.types.rev220327.OPENCONFIGHARDWARECOMPONENT;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.types.rev220327.TRANSCEIVER;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.OpenconfigTerminalDeviceData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.device.top.TerminalDevice;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.device.top.TerminalDeviceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.chan.assignment.top.LogicalChannelAssignments;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.chan.assignment.top.LogicalChannelAssignmentsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.chan.assignment.top.logical.channel.assignments.Assignment;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.chan.assignment.top.logical.channel.assignments.AssignmentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.chan.assignment.top.logical.channel.assignments.AssignmentKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.ingress.top.Ingress;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.ingress.top.IngressBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.top.LogicalChannels;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.top.LogicalChannelsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.top.logical.channels.Channel;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.top.logical.channels.ChannelBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.logical.channel.top.logical.channels.ChannelKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.OpticalPort;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.OpticalPortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.AdminStateType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.OPTICALCHANNEL;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.OPTICALPORTTYPE;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.TRIBUTARYPROTOCOLTYPE;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.OpenTerminalMetaData;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.OpenTerminalMetaDataBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.LineCardInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.LineCardInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.TransceiverInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.TransceiverInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.LineCard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.LineCardBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.LineCardKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SupportedPort;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SupportedPortBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SupportedPortKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SwitchFabric;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SwitchFabricBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card.SwitchFabricKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card._switch.fabric.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card._switch.fabric.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.line.card.info.line.card._switch.fabric.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.Transceiver;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.TransceiverBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.TransceiverKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.OperationalModes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.OperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.operational.modes.OperationalMode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.operational.modes.OperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.operational.modes.OperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.supported._interface.capability.InterfaceSequence;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.supported._interface.capability.InterfaceSequenceBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.open.terminal.meta.data.transceiver.info.transceiver.supported._interface.capability.InterfaceSequenceKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250902.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250902.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250902.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev250110.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class OCPortMappingTest {
    DataBroker dataBroker = null;
    private OCPortMappingVersion190 ocPortMappingVersion190;
    private PortMapping portMapping;
    DeviceTransactionManager deviceTransactionManager = null;
    NetworkTransactionService networkTransactionService = null;
    private OCMetaDataTransaction ocMetaDataTransaction;
    private OCPortMappingVersion190 ocPortMappingVersion190Test;
    private PortMappingVersion710 portMappingVersion710;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;

    @BeforeEach
    void setUp() {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        ocPortMappingVersion190 = mock(OCPortMappingVersion190.class);
        ocMetaDataTransaction = mock(OCMetaDataTransaction.class);
        deviceTransactionManager = mock(DeviceTransactionManager.class);
        portMapping = new PortMappingImpl(dataBroker, portMappingVersion710, portMappingVersion22,
                portMappingVersion121, ocPortMappingVersion190);
        ocPortMappingVersion190Test = new OCPortMappingVersion190(dataBroker, deviceTransactionManager,
                ocMetaDataTransaction, networkTransactionService);
    }

    @Test
    void createOCMappingDataTest() {
        Ipv4Address ipv4Address = new Ipv4Address("127.0.0.1");
        IpAddress ipAddress = new IpAddress(ipv4Address);
        when(ocPortMappingVersion190.createMappingData("node",ipAddress)).thenReturn(true);
        assertTrue(portMapping.createMappingData("node", OPENCONFIG_DEVICE_VERSION_1_9_0, ipAddress));

        assertFalse(portMapping.createMappingData("node", "test", ipAddress));
    }

    @Test
    void createXpdrMappingTest() {
        PlatformComponentState.Type type;
        type = new PlatformComponentState.Type(OPENCONFIGHARDWARECOMPONENT.VALUE);
        State state = new StateBuilder().setName("linecard").setType(type)
                .setDescription("Linecard component (4 x 400G CFP2-DCO + 16 x 100GbE QSFP28 Ports)")
                .setParent("chassis")
                .build();
        Component component =  new ComponentBuilder()
                .setName("linecard")
                .setState(state).build();
        Component componentPort =  getLinePortComponentTestData();
        List<Component>  componentList = new ArrayList<>();
        componentList.add(component);
        componentList.add(componentPort);
        Map<String, Set<String>> lcpNamingMap = new HashMap<>();
        Set<String> lcpSet = new HashSet<>();
        lcpSet.add("XPDR1-CLIENT1");
        lcpNamingMap.put("XPDR1-NETWORK1", lcpSet);
        ocPortMappingVersion190Test = spy(ocPortMappingVersion190Test);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(component, PORT);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(component, LINECARD);
        doReturn(getTestMetaData()).when(ocMetaDataTransaction).getXPDROpenTerminalMetaData();
        DataObjectIdentifier<TerminalDevice> terminalDeviceIid =
                DataObjectIdentifier.builderOfInherited(OpenconfigTerminalDeviceData.class, TerminalDevice.class)
                        .build();
        LogicalChannels logicalChannels = getLogicalChannelTestData();
        TerminalDevice terminalDevice = new TerminalDeviceBuilder().setLogicalChannels(logicalChannels).build();
        when(deviceTransactionManager.getDataFromDevice("node1",
                LogicalDatastoreType.OPERATIONAL, terminalDeviceIid, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)).thenReturn(Optional.of(terminalDevice));
        List<LineCard> lineCardInfo = Objects.requireNonNull(getTestMetaData().getLineCardInfo().getLineCard())
                .values().stream().toList();
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        Set<Float> frequencyGHzSet = new LinkedHashSet<>();
        doReturn(lcpNamingMap).when(ocPortMappingVersion190Test).createNetworkLcpMapping("node1",
                componentList, lineCardInfo.get(0), lcpMap, mappingMap, 1,componentPort.getSubcomponents(),
                componentList, frequencyGHzSet);
        doNothing().when(ocPortMappingVersion190Test).createClientLcpMapping("node1", componentList, lcpMap,
                mappingMap, lineCardInfo.get(0),1, lcpNamingMap, componentList, frequencyGHzSet);
        List<Mapping> portMapList = new ArrayList<>();
        Map<McCapabilitiesKey, McCapabilities> mcCapabilities = new HashMap<>();
        doNothing().when(ocPortMappingVersion190Test).createMcCapabilities(mcCapabilities, frequencyGHzSet,
                "node1");
        assertTrue(ocPortMappingVersion190Test.createXpdrPortMapping("node123", componentList,
                portMapList, mcCapabilities));
        List<Component>  componentListEmpty = new ArrayList<>();
        assertFalse(ocPortMappingVersion190Test.createXpdrPortMapping("node123", componentListEmpty,
                portMapList, mcCapabilities));

    }

    @Test
    void createNetworkLcpMappingTest() {
        Component componentPort = getLinePortComponentTestData();
        List<Component>  componentList = new ArrayList<>();
        componentList.add(componentPort);
        Set<SupportedIfCapability> supportedIntf = new HashSet<>();
        supportedIntf.add(MappingUtilsImpl.ocConvertSupIfCapa("if-OTUCN-ODUCN"));
        ocPortMappingVersion190Test = spy(ocPortMappingVersion190Test);
        List<Transceiver> transceiver = Objects.requireNonNull(getTestMetaData().getTransceiverInfo()
                .getTransceiver()).values().stream().toList();
        doReturn(null).when(ocPortMappingVersion190Test).getTransceiversListMetaData();
        doReturn(transceiver.get(0)).when(ocPortMappingVersion190Test).getTransceiverMetaData(componentList,
                componentPort, null);
        doReturn(supportedIntf).when(ocPortMappingVersion190Test).createSupportedInterfaceCapability(transceiver
                .get(0));
        String rate = "";
        doReturn(rate).when(ocPortMappingVersion190Test).getRate(transceiver.get(0));
        Set<Float> frequencyGHzSet = new LinkedHashSet<>();
        doNothing().when(ocPortMappingVersion190Test).createCentralFrequency(transceiver.get(0), frequencyGHzSet);
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        Port1 augmentationPort = componentPort.getPort().augmentation(Port1.class);
        doNothing().when(ocPortMappingVersion190Test).createLcpMapping("node1", componentPort, augmentationPort,
                NETWORK_TOKEN, 1, lcpMap, mappingMap, LineCard.XpdrType.MPDR, 1,
                componentList, supportedIntf, null, rate);
        List<LineCard> lineCardInfo = Objects.requireNonNull(getTestMetaData().getLineCardInfo().getLineCard())
                .values().stream().toList();
        Map<String, Set<String>> lcpNamingMap = ocPortMappingVersion190Test.createNetworkLcpMapping("node1",
                componentList, lineCardInfo.get(0), lcpMap, mappingMap,
                1,componentPort.getSubcomponents(), componentList, frequencyGHzSet);
        assertTrue(lcpNamingMap.containsKey("XPDR1-NETWORK1"));
    }

    @Test
    void createClientLcpMappingTest() {
        Map<String, Set<String>> lcpNamingMap = new HashMap<>();
        Set<String> lcpSet = new HashSet<>();
        lcpSet.add("XPDR1-CLIENT1");
        lcpNamingMap.put("XPDR1-NETWORK1", lcpSet);
        Set<SupportedIfCapability> supportedIntf = new HashSet<>();
        supportedIntf.add(MappingUtilsImpl.ocConvertSupIfCapa("if-OTUCN-ODUCN"));
        ocPortMappingVersion190Test = spy(ocPortMappingVersion190Test);
        LogicalChannels logicalChannels = getLogicalChannelTestData();
        DataObjectIdentifier<TerminalDevice> terminalDeviceIid =
                DataObjectIdentifier.builderOfInherited(OpenconfigTerminalDeviceData.class, TerminalDevice.class)
                        .build();
        TerminalDevice terminalDevice = new TerminalDeviceBuilder().setLogicalChannels(logicalChannels).build();
        when(deviceTransactionManager.getDataFromDevice("node1",
                LogicalDatastoreType.OPERATIONAL, terminalDeviceIid, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)).thenReturn(Optional.of(terminalDevice));
        List<Component> componentList = getClientPortComponentTestData();
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(componentList.get(0), PORT);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(componentList.get(1), TRANSCEIVER);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(componentList.get(2), OPTICALCHANNEL);
        List<Transceiver> transceiver = Objects.requireNonNull(getTestMetaData().getTransceiverInfo()
                .getTransceiver()).values().stream().toList();
        doReturn(null).when(ocPortMappingVersion190Test).getTransceiversListMetaData();
        doReturn(transceiver.get(0)).when(ocPortMappingVersion190Test).getTransceiverMetaData(componentList,
                componentList.get(0), null);
        doReturn(supportedIntf).when(ocPortMappingVersion190Test).createSupportedInterfaceCapability(transceiver
                .get(0));
        String rate = "";
        doReturn(rate).when(ocPortMappingVersion190Test).getRate(transceiver.get(0));
        Set<Float> frequencyGHzSet = new LinkedHashSet<>();
        doNothing().when(ocPortMappingVersion190Test).createCentralFrequency(transceiver.get(0), frequencyGHzSet);
        Port1 augmentationPort = componentList.get(0).getPort().augmentation(Port1.class);
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        doNothing().when(ocPortMappingVersion190Test).createLcpMapping("node1", componentList.get(0), augmentationPort,
                CLIENT_TOKEN,1, lcpMap, mappingMap, LineCard.XpdrType.MPDR, 1,
                componentList, supportedIntf, null, rate);
        List<LineCard> lineCardInfo = Objects.requireNonNull(getTestMetaData().getLineCardInfo().getLineCard())
                .values().stream().toList();
        ocPortMappingVersion190Test.createClientLcpMapping("node1", Arrays.asList(componentList.get(0)),
                lcpMap, mappingMap, lineCardInfo.get(0), 1, lcpNamingMap, componentList, frequencyGHzSet);
        assertTrue(mappingMap.containsKey("XPDR1-CLIENT2"));
        /*assertTrue(mappingMap.get("XPDR1-CLIENT2").getOpenconfigInfo()
                .getSupportedInterfaces().contains("logical-channel-11001"));*/
        assertTrue(mappingMap.get("XPDR1-CLIENT2").getOpenconfigInfo().getSupportedOpticalChannels()
                .contains("qsfp-opt-1-1"));
    }

    LogicalChannels getLogicalChannelTestData() {
        var state = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal
                .logical.channel.ingress.top.ingress.StateBuilder().setTransceiver("qsfp-1").build();
        var channelState = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal
                .logical.channel.top.logical.channels.channel.StateBuilder()
                .setTribProtocol(TRIBUTARYPROTOCOLTYPE.VALUE).build();
        Ingress ingress = new IngressBuilder().setState(state).build();
        var assignmentState = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729
                .terminal.logical.chan.assignment.top.logical.channel.assignments.assignment.StateBuilder()
                .setLogicalChannel(Uint32.valueOf("10001")).build();
        var assignmentState2 = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729
                .terminal.logical.chan.assignment.top.logical.channel.assignments.assignment.StateBuilder().build();
        Assignment assignment = new AssignmentBuilder().setState(assignmentState)
                .setIndex(Uint32.valueOf("10001")).build();
        Assignment assignment2 = new AssignmentBuilder().setState(assignmentState2)
                .setIndex(Uint32.valueOf("1000")).build();
        Map<AssignmentKey, Assignment> assignmentMap = new HashMap<>();
        Map<AssignmentKey, Assignment> assignmentMap2 = new HashMap<>();
        assignmentMap.put(assignment.key(), assignment);
        assignmentMap2.put(assignment2.key(), assignment2);
        LogicalChannelAssignments logicalChannelAssignments = new LogicalChannelAssignmentsBuilder()
                .setAssignment(assignmentMap).build();
        LogicalChannelAssignments logicalChannelAssignments2 = new LogicalChannelAssignmentsBuilder()
                .setAssignment(assignmentMap2).build();
        Channel channel = new ChannelBuilder().setIndex(Uint32.valueOf("11001")).setIngress(ingress)
                .setLogicalChannelAssignments(logicalChannelAssignments)
                .setState(channelState).build();
        Channel channel2 = new ChannelBuilder().setIndex(Uint32.valueOf("10001"))
                .setLogicalChannelAssignments(logicalChannelAssignments2)
                .setState(channelState).build();
        Map<ChannelKey, Channel> channelMap = new HashMap<>();
        channelMap.put(channel.key(), channel);
        channelMap.put(channel2.key(), channel2);
        return new LogicalChannelsBuilder().setChannel(channelMap).build();
    }

    Component getLinePortComponentTestData() {
        PlatformComponentState.Type typePort;
        typePort = new PlatformComponentState.Type(OPENCONFIGHARDWARECOMPONENT.VALUE);
        State statePort = new StateBuilder().setName("port").setType(typePort)
                .setDescription("Linecard component (4 x 400G CFP2-DCO + 16 x 100GbE QSFP28 Ports)")
                .setParent("qsfp-1")
                .setPartNo("FIM38750/102")
                .build();
        Subcomponent subcomponent = new SubcomponentBuilder().setName(new String("qsfp-1")).build();
        Map<SubcomponentKey, Subcomponent> subcomponentMap = new HashMap<>();
        subcomponentMap.put(subcomponent.key(), subcomponent);
        Subcomponents subcomponents = new SubcomponentsBuilder().setSubcomponent(subcomponentMap).build();
        var opticalState = new  org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                .transport.line.common.rev190603.transport.line.common.port.top.optical.port.StateBuilder()
                .setOpticalPortType(getOpticalPorttype(TERMINALLINE)).build();
        OpticalPort opticalPort = new OpticalPortBuilder().setState(opticalState).build();
        Port1 port1 = new Port1Builder().setOpticalPort(opticalPort).build();
        Port port = new PortBuilder().addAugmentation(port1).build();
        return new ComponentBuilder()
                .setName("cfp2-1")
                .setState(statePort)
                .setSubcomponents(subcomponents)
                .setPort(port).build();
    }

    List<Component> getClientPortComponentTestData() {
        ArrayList<Component> components = new ArrayList<>();
        components.add(populatePortComponent());
        components.add(populateTransceiverComponent());
        components.add(populateOpticalChannelComponent());
        return components;
    }

    private Component populatePortComponent() {
        State statePort = new StateBuilder()
                .setName("qsfp-1")
                .setType(new PlatformComponentState.Type(
                        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.types.rev220327.PORT.VALUE))
                .setDescription("QSFP PORT 1")
                .setParent("linecard-1")
                .build();
        Subcomponent subcomponent = new SubcomponentBuilder().setName(new String("qsfp-transceiver-1")).build();
        Map<SubcomponentKey, Subcomponent> subcomponentMap = new HashMap<>();
        subcomponentMap.put(subcomponent.key(), subcomponent);
        Subcomponents subcomponents = new SubcomponentsBuilder().setSubcomponent(subcomponentMap).build();
        var opticalState = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                .transport.line.common.rev190603.transport.line.common.port.top.optical.port.StateBuilder()
                .setOpticalPortType(getOpticalPorttype(TERMINALCLIENT))
                .setAdminState(AdminStateType.ENABLED).build();
        OpticalPort opticalPort = new OpticalPortBuilder().setState(opticalState).build();
        Port1 port1 = new Port1Builder().setOpticalPort(opticalPort).build();
        Port port = new PortBuilder().addAugmentation(port1).build();
        return new ComponentBuilder()
                .setName("qsfp-1")
                .setState(statePort)
                .setSubcomponents(subcomponents)
                .setPort(port).build();
    }

    private Component populateTransceiverComponent() {
        State statePort = new StateBuilder()
                .setName("qsfp-transceiver-1")
                .setType(new PlatformComponentState.Type(
                        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.types.rev220327
                                .TRANSCEIVER.VALUE))
                .setDescription("QSFP TRANSCEIVER 1")
                .setParent("qsfp-1")
                .build();
        Subcomponent subcomponent = new SubcomponentBuilder().setName(new String("qsfp-opt-1-1")).build();
        Map<SubcomponentKey, Subcomponent> subcomponentMap = new HashMap<>();
        subcomponentMap.put(subcomponent.key(), subcomponent);
        Subcomponents subcomponents = new SubcomponentsBuilder().setSubcomponent(subcomponentMap).build();
        return new ComponentBuilder()
                .setName("qsfp-transceiver-1")
                .setState(statePort)
                .setSubcomponents(subcomponents).build();
    }

    private Component populateOpticalChannelComponent() {
        State statePort = new StateBuilder()
                .setName("qsfp-opt-1-1")
                .setType(new PlatformComponentState.Type(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport
                        .types.rev210729.OPTICALCHANNEL.VALUE))
                .setDescription("QSFP OPTICAL CHANNEL 1-1")
                .setParent("qsfp-transceiver-1")
                .build();
        return new ComponentBuilder()
                .setName("qsfp-opt-1-1")
                .setState(statePort).build();
    }

    OPTICALPORTTYPE getOpticalPorttype(String portType) {
        return portType.equalsIgnoreCase(TERMINALLINE)
                ? org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.TERMINALLINE.VALUE
                : org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.TERMINALCLIENT.VALUE;
    }

    OpenTerminalMetaData getTestMetaData() {
        Set<Uint8> lcpList = new HashSet<>();
        lcpList.add(Uint8.ONE);
        lcpList.add(Uint8.TWO);
        NonBlockingList nonBlockingList = new NonBlockingListBuilder().setConnectablePort(lcpList)
                .setNblId(Uint8.ONE)
                .build();
        Map<NonBlockingListKey, NonBlockingList> nonBlockingListMap = new HashMap<>();
        nonBlockingListMap.put(nonBlockingList.key(), nonBlockingList);
        SwitchFabric  switchFabric = new SwitchFabricBuilder().setSwitchFabricId(Uint8.ONE)
                .setSwitchFabricType(SwitchFabric.SwitchFabricType.Blocking)
                .setNonBlockingList(nonBlockingListMap)
                .build();
        Map<SwitchFabricKey, SwitchFabric> switchFabricMap = new HashMap<>();
        switchFabricMap.put(switchFabric.key(), switchFabric);
        SupportedPort supportedPort = new SupportedPortBuilder().setComponentName("cfp2-1")
                .setId(Uint8.ONE)
                .setType(getOpticalPorttype(TERMINALLINE)).build();
        SupportedPort supportedPortClient = new SupportedPortBuilder().setComponentName("qsfp-1")
                .setId(Uint8.TWO)
                .setType(getOpticalPorttype(TERMINALCLIENT)).build();
        Map<SupportedPortKey,SupportedPort> supportedPortMap = new HashMap<>();
        supportedPortMap.put(supportedPort.key(), supportedPort);
        supportedPortMap.put(supportedPortClient.key(), supportedPortClient);
        LineCard lineCard = new LineCardBuilder()
                .setPartNo("Linecard component (4 x 400G CFP2-DCO + 16 x 100GbE QSFP28 Ports)")
                .setXpdrType(LineCard.XpdrType.MPDR)
                .setSupportedPort(supportedPortMap)
                .setSwitchFabric(switchFabricMap).build();
        Map<LineCardKey, LineCard> lineCardMap = new HashMap<>();
        lineCardMap.put(lineCard.key(),lineCard);
        OperationalMode operationalMode = new OperationalModeBuilder().setModeId(Uint16.ONE)
                .setCatalogId("1").setRate("400").build();
        Map<OperationalModeKey, OperationalMode> operationalModeMap = new HashMap<>();
        operationalModeMap.put(operationalMode.key(), operationalMode);
        OperationalModes operationalModes = new OperationalModesBuilder()
                .setOperationalMode(operationalModeMap).build();
        InterfaceSequence interfaceSequence = new InterfaceSequenceBuilder()
                .setInterfaceType(TRIBUTARYPROTOCOLTYPE.VALUE)
                .setPosition(Uint8.ONE)
                .setMaxInterfaces(Uint16.ONE).build();
        Map<InterfaceSequenceKey, InterfaceSequence> interfaceSequenceMap = new HashMap<>();
        interfaceSequenceMap.put(interfaceSequence.key(), interfaceSequence);
        SupportedInterfaceCapability supportedInterfaceCapability = new SupportedInterfaceCapabilityBuilder()
                .setInterfaceSequence(interfaceSequenceMap).build();
        List<SupportedInterfaceCapability> supportedInterfaceCapabilityList = new ArrayList<>();
        supportedInterfaceCapabilityList.add(supportedInterfaceCapability);
        Transceiver transceiver = new TransceiverBuilder().setPartNo("FIM38750/102")
                .setOperationalModes(operationalModes)
                .setSupportedInterfaceCapability(supportedInterfaceCapabilityList).build();
        Map<TransceiverKey, Transceiver> transceiverMap = new HashMap<>();
        transceiverMap.put(transceiver.key(), transceiver);
        TransceiverInfo transceiverInfo = new TransceiverInfoBuilder().setTransceiver(transceiverMap).build();
        LineCardInfo lineCardInfo = new LineCardInfoBuilder().setLineCard(lineCardMap).build();
        return new OpenTerminalMetaDataBuilder()
                .setLineCardInfo(lineCardInfo)
                .setTransceiverInfo(transceiverInfo).build();
    }

    @Test
    void clientPortExistsOnNELineCardTest() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        Map<SubcomponentKey, Subcomponent> subcomponentKeySubcomponentMap = new HashMap<>();
        List<Optional<SupportedPort>> supportedClientPorts = new ArrayList<>();
        var state = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                .platform.rev220610.platform.subcomponent.ref.top.subcomponents.subcomponent.StateBuilder()
                .setName("qsfp-1").build();
        Subcomponent subcomponent = new SubcomponentBuilder().setName("qsfp-1").setState(state).build();
        subcomponentKeySubcomponentMap.put(new SubcomponentKey("qsfp-1"),subcomponent);
        Subcomponents subcomponents = new SubcomponentsBuilder()
                .setSubcomponent(subcomponentKeySubcomponentMap).build();
        SupportedPort port = new SupportedPortBuilder().setComponentName("qsfp-1").setId(Uint8.ONE).build();
        supportedClientPorts.add(Optional.of(port));
        Method method = ocPortMappingVersion190.getClass()
                .getDeclaredMethod("clientPortsExistsOnNELineCard", List.class, Subcomponents.class);
        method.setAccessible(true);
        assertNotNull(method.invoke(ocPortMappingVersion190,supportedClientPorts,subcomponents));
    }
}

