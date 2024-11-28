/*
 * Copyright © 2020 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.CLIENT_TOKEN;
import static org.opendaylight.transportpce.common.StringConstants.LINECARD;
import static org.opendaylight.transportpce.common.StringConstants.NETWORK_TOKEN;
import static org.opendaylight.transportpce.common.StringConstants.OPENCONFIG_DEVICE_VERSION_1_9_0;
import static org.opendaylight.transportpce.common.StringConstants.PORT;
import static org.opendaylight.transportpce.common.StringConstants.TERMINALCLIENT;
import static org.opendaylight.transportpce.common.StringConstants.TERMINALLINE;
import static org.opendaylight.transportpce.common.StringConstants.TRANSCEIVER;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.Port1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.OpticalPort;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common.port.top.OpticalPortBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.AdminStateType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.OPTICALPORTTYPE;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.TERMINALCLIENT;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729.TERMINALLINE;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public class OCPortMappingTest {
    DataBroker dataBroker = null;
    private OCPortMappingVersion190 ocPortMappingVersion190;
    private OCPortMapping ocPortMapping;
    DeviceTransactionManager deviceTransactionManager = null;
    NetworkTransactionService networkTransactionService = null;
    private OCMetaDataTransaction ocMetaDataTransaction;
    private OCPortMappingVersion190 ocPortMappingVersion190Test;

    @BeforeEach
    void setUp() throws Exception {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        ocPortMappingVersion190 = mock(OCPortMappingVersion190.class);
        ocMetaDataTransaction = mock(OCMetaDataTransaction.class);
        ocPortMapping = new OCPortMappingImpl(dataBroker, ocPortMappingVersion190);
        ocPortMappingVersion190Test = new OCPortMappingVersion190(dataBroker, deviceTransactionManager,
                ocMetaDataTransaction, networkTransactionService);
        ocPortMapping = new OCPortMappingImpl(dataBroker, ocPortMappingVersion190);
    }

    @Test
    void createOCMappingDataTest() {
        Ipv4Address ipv4Address = new Ipv4Address("127.0.0.1");
        IpAddress ipAddress = new IpAddress(ipv4Address);
        when(ocPortMappingVersion190.createMappingData("node",ipAddress)).thenReturn(true);
        assertTrue(ocPortMapping.createMappingData("node", OPENCONFIG_DEVICE_VERSION_1_9_0, ipAddress));

        assertFalse(ocPortMapping.createMappingData("node", "test",ipAddress));
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
        ocPortMappingVersion190Test = Mockito.spy(ocPortMappingVersion190Test);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(component, PORT);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(component, LINECARD);
        doReturn(getTestMetaData()).when(ocMetaDataTransaction).getXPDROpenTerminalMetaData();
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
        ocPortMappingVersion190Test = Mockito.spy(ocPortMappingVersion190Test);
        List<Transceiver> transceiver = Objects.requireNonNull(getTestMetaData().getTransceiverInfo()
                .getTransceiver()).values().stream().toList();
        doReturn(transceiver.get(0)).when(ocPortMappingVersion190Test).getTransceiverMetaData(componentList,
                componentPort);
        doReturn(supportedIntf).when(ocPortMappingVersion190Test).createSupportedInterfaceCapability(transceiver
                .get(0));
        Set<Float> frequencyGHzSet = new LinkedHashSet<>();
        doNothing().when(ocPortMappingVersion190Test).createCentralFrequency(transceiver.get(0), frequencyGHzSet);
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        Port1 augmentationPort = componentPort.getPort().augmentation(Port1.class);
        doNothing().when(ocPortMappingVersion190Test).createLcpMapping("node1", componentPort, augmentationPort,
                NETWORK_TOKEN, 1, lcpMap, mappingMap, LineCard.XpdrType.MPDR, 1,
                componentList, supportedIntf);
        List<LineCard> lineCardInfo = Objects.requireNonNull(getTestMetaData().getLineCardInfo().getLineCard())
                .values().stream().toList();
        Map<String, Set<String>> lcpNamingMap = ocPortMappingVersion190Test.createNetworkLcpMapping("node1",
                componentList, lineCardInfo.get(0), lcpMap, mappingMap,
                1,componentPort.getSubcomponents(), componentList, frequencyGHzSet);
        assertTrue(lcpNamingMap.containsKey("XPDR1-NETWORK1"));
    }

    @Test
    void createClientLcpMappingTest() {
        Component componentPort =  getClinetPortComponentTestData();
        List<Component>  componentList = new ArrayList<>();
        componentList.add(componentPort);
        Map<String, Set<String>> lcpNamingMap = new HashMap<>();
        Set<String> lcpSet = new HashSet<>();
        lcpSet.add("XPDR1-CLIENT1");
        lcpNamingMap.put("XPDR1-NETWORK1", lcpSet);
        Set<SupportedIfCapability> supportedIntf = new HashSet<>();
        supportedIntf.add(MappingUtilsImpl.ocConvertSupIfCapa("if-OTUCN-ODUCN"));
        ocPortMappingVersion190Test = Mockito.spy(ocPortMappingVersion190Test);
        doReturn(true).when(ocPortMappingVersion190Test).checkComponentType(componentPort, TRANSCEIVER);
        List<Transceiver> transceiver = Objects.requireNonNull(getTestMetaData().getTransceiverInfo()
                .getTransceiver()).values().stream().toList();
        doReturn(transceiver.get(0)).when(ocPortMappingVersion190Test).getTransceiverMetaData(componentList,
                componentPort);
        doReturn(supportedIntf).when(ocPortMappingVersion190Test).createSupportedInterfaceCapability(transceiver
                .get(0));
        Set<Float> frequencyGHzSet = new LinkedHashSet<>();
        doNothing().when(ocPortMappingVersion190Test).createCentralFrequency(transceiver.get(0), frequencyGHzSet);
        Port1 augmentationPort = componentPort.getPort().augmentation(Port1.class);
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        doNothing().when(ocPortMappingVersion190Test).createLcpMapping("node1", componentPort, augmentationPort,
                CLIENT_TOKEN,1, lcpMap, mappingMap, LineCard.XpdrType.MPDR, 1,
                componentList, supportedIntf);
        List<LineCard> lineCardInfo = Objects.requireNonNull(getTestMetaData().getLineCardInfo().getLineCard())
                .values().stream().toList();
        ocPortMappingVersion190Test.createClientLcpMapping("node1", componentList, lcpMap, mappingMap,
                lineCardInfo.get(0), 1,lcpNamingMap, componentList, frequencyGHzSet);
        assertTrue(mappingMap.containsKey("XPDR1-CLIENT2"));
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
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common
                .port.top.optical.port.State opticalState = new  org.opendaylight.yang.gen.v1.http.openconfig.net.yang
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

    Component getClinetPortComponentTestData() {
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
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.line.common.rev190603.transport.line.common
                .port.top.optical.port.State opticalState = new  org.opendaylight.yang.gen.v1.http.openconfig.net.yang
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

    OPTICALPORTTYPE getOpticalPorttype(String portType) {
        OPTICALPORTTYPE opticalporttype;
        if (portType.equalsIgnoreCase(TERMINALLINE)) {
            opticalporttype =  new TERMINALLINE() {
                @Override
                public Class<? extends org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport
                        .types.rev210729.TERMINALLINE> implementedInterface() {
                    return org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729
                            .TERMINALLINE.class;
                }

                @Override
                public String toString() {
                    return TERMINALLINE;
                }
            };
        } else {
            opticalporttype = new TERMINALCLIENT() {
                @Override
                public Class<? extends org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types
                        .rev210729.TERMINALCLIENT> implementedInterface() {
                    return org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev210729
                            .TERMINALCLIENT.class;
                }

                @Override
                public String toString() {
                    return TERMINALCLIENT;
                }
            };
        }
        return opticalporttype;
    }

    OpenTerminalMetaData getTestMetaData() {
        Set<Uint8> lcpList = new HashSet<>();
        lcpList.add(Uint8.valueOf(1));
        lcpList.add(Uint8.valueOf(2));
        NonBlockingList nonBlockingList = new NonBlockingListBuilder().setConnectablePort(lcpList)
                .setNblId(Uint8.valueOf(1))
                .build();
        Map<NonBlockingListKey, NonBlockingList> nonBlockingListMap = new HashMap<>();
        nonBlockingListMap.put(nonBlockingList.key(), nonBlockingList);
        SwitchFabric  switchFabric = new SwitchFabricBuilder().setSwitchFabricId(Uint8.valueOf(1))
                .setSwitchFabricType(SwitchFabric.SwitchFabricType.Blocking)
                .setNonBlockingList(nonBlockingListMap)
                .build();
        Map<SwitchFabricKey, SwitchFabric> switchFabricMap = new HashMap<>();
        switchFabricMap.put(switchFabric.key(), switchFabric);
        SupportedPort supportedPort = new SupportedPortBuilder().setComponentName("cfp2-1")
                .setId(Uint8.valueOf(1))
                .setType(getOpticalPorttype(TERMINALLINE)).build();
        SupportedPort supportedPortClient = new SupportedPortBuilder().setComponentName("qsfp-1")
                .setId(Uint8.valueOf(2))
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
        OperationalMode operationalMode = new OperationalModeBuilder().setModeId(Uint16.valueOf(1))
                .setCatalogId("1").build();
        Map<OperationalModeKey, OperationalMode> operationalModeMap = new HashMap<>();
        operationalModeMap.put(operationalMode.key(), operationalMode);
        OperationalModes operationalModes = new OperationalModesBuilder()
                .setOperationalMode(operationalModeMap).build();
        InterfaceSequence interfaceSequence = new InterfaceSequenceBuilder()
                .setInterfaceType(TRIBUTARYPROTOCOLTYPE.VALUE)
                .setPosition(Uint8.valueOf(1))
                .setMaxInterfaces(Uint16.valueOf(1)).build();
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
        org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.subcomponent.ref.top
                .subcomponents.subcomponent.State state = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                .platform.rev220610.platform.subcomponent.ref.top.subcomponents.subcomponent.StateBuilder()
                .setName("qsfp-1").build();
        Subcomponent subcomponent = new SubcomponentBuilder().setName("qsfp-1").setState(state).build();
        subcomponentKeySubcomponentMap.put(new SubcomponentKey("qsfp-1"),subcomponent);
        Subcomponents subcomponents = new SubcomponentsBuilder()
                .setSubcomponent(subcomponentKeySubcomponentMap).build();
        SupportedPort port = new SupportedPortBuilder().setComponentName("qsfp-1").setId(Uint8.valueOf(1)).build();
        supportedClientPorts.add(Optional.of(port));
        Method method = ocPortMappingVersion190.getClass()
                .getDeclaredMethod("clientPortsExistsOnNELineCard", List.class, Subcomponents.class);
        method.setAccessible(true);
        Set<Uint8> uint8s = (Set<Uint8>) method.invoke(ocPortMappingVersion190,supportedClientPorts,subcomponents);
        Assertions.assertNotNull(uint8s);
    }
}

