/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.NetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.ParentCircuitPackBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.InfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.connection.map.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.connection.map.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.PartnerPortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.LldpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfig;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMappingVersion121Test {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingVersion121Test.class);
    private static DataBroker dataBroker;
    private static DeviceTransactionManager deviceTransactionManager;
    private static OpenRoadmInterfaces openRoadmInterfaces;
    private static  PortMappingVersion121 portMappingVersion121;

    @Before
    public void setUp() throws Exception {
        //test createMappingData for a xpdr node with 3 network + 1 client + bidirectional & unidirectional ports
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        deviceTransactionManager = mock(DeviceTransactionManager.class);
        openRoadmInterfaces = mock(OpenRoadmInterfaces.class);
        portMappingVersion121 = new PortMappingVersion121(dataBroker,
                deviceTransactionManager,
                openRoadmInterfaces);
    }

    @Test
    public void createMappingDataTestRdm() {
        //mock node info
        final Info info = getInfo2();

        List<Interfaces> interfacesList = Arrays.asList(getInterfaces("i1"),
                getInterfaces("i2"));

        //mock 1 bidirectional port for degree
        Ports ports = getPortsWithInterfaces(interfacesList);
        List<Ports> portsList = Arrays.asList(ports);

        //mock 2 unidirectional ports for degree
        Ports ports2 = getPorts("p2", Port.PortQual.RoadmExternal, "c3", "p3", Direction.Rx);
        Ports ports3 = getPorts("p3", Port.PortQual.RoadmExternal, "c3", "p2", Direction.Tx);
        List<Ports> portsList2 = Arrays.asList(ports2,ports3);

        //mock 2 unidirectional ports for degree, reverse direction
        Ports ports22 = getPorts("p22", Port.PortQual.RoadmExternal, "c5", "p33", Direction.Tx);
        Ports ports33 = getPorts("p33", Port.PortQual.RoadmExternal, "c5", "p22", Direction.Rx);
        List<Ports> portsList22 = Arrays.asList(ports22,ports33);

        //mock 2 unidirectional ports for srg
        Ports ports4 = getPorts("p4", Port.PortQual.RoadmExternal, "c4", "p5", Direction.Rx);
        Ports ports5 = getPorts("p5", Port.PortQual.RoadmExternal, "c4", "p4", Direction.Tx);
        List<Ports> portsList4 = Arrays.asList(ports4,ports5);

        //mock 2 unidirectional ports for srg, reverse direction
        Ports ports44 = getPorts("p44", Port.PortQual.RoadmExternal, "c6", "p55", Direction.Tx);
        Ports ports55 = getPorts("p55", Port.PortQual.RoadmExternal, "c6", "p44", Direction.Rx);
        List<Ports> portsList44 = Arrays.asList(ports44,ports55);

        //mock 6 circuit packs
        final CircuitPacks circuitPackObject = getCircuitPacks(portsList, "c1", "pc1");
        final CircuitPacks circuitPackObject2 = getCircuitPacks(portsList, "c2", "pc2");
        final CircuitPacks circuitPackObject3 = getCircuitPacks(portsList2, "c3", "pc3");
        final CircuitPacks circuitPackObject4 = getCircuitPacks(portsList4, "c4", "pc4");
        final CircuitPacks circuitPackObject5 = getCircuitPacks(portsList22, "c5", "pc5");
        final CircuitPacks circuitPackObject6 = getCircuitPacks(portsList44, "c6", "pc6");

        //mock 6 connection ports
        ConnectionPorts connectionPorts = getConnectionPorts("c1", "p1");
        List<ConnectionPorts> connectionPortsList = new ArrayList<ConnectionPorts>();
        connectionPortsList.add(connectionPorts);

        ConnectionPorts connectionPorts2 = getConnectionPorts("c2", "p1");
        List<ConnectionPorts> connectionPortsList2 = new ArrayList<ConnectionPorts>();
        connectionPortsList2.add(connectionPorts2);

        ConnectionPorts connectionPorts3 = getConnectionPorts("c3", "p2");
        ConnectionPorts connectionPorts4 = getConnectionPorts("c3", "p3");
        List<ConnectionPorts> connectionPortsList3 = new ArrayList<ConnectionPorts>();
        connectionPortsList3.add(connectionPorts3);
        connectionPortsList3.add(connectionPorts4);

        ConnectionPorts connectionPorts5 = getConnectionPorts("c4", "p4");
        ConnectionPorts connectionPorts6 = getConnectionPorts("c4", "p5");
        List<ConnectionPorts> connectionPortsList4 = new ArrayList<ConnectionPorts>();
        connectionPortsList4.add(connectionPorts5);
        connectionPortsList4.add(connectionPorts6);

        ConnectionPorts connectionPorts33 = getConnectionPorts("c5", "p22");
        ConnectionPorts connectionPorts44 = getConnectionPorts("c5", "p33");
        List<ConnectionPorts> connectionPortsList33 = new ArrayList<ConnectionPorts>();
        connectionPortsList33.add(connectionPorts33);
        connectionPortsList33.add(connectionPorts44);

        ConnectionPorts connectionPorts55 = getConnectionPorts("c6", "p44");
        ConnectionPorts connectionPorts66 = getConnectionPorts("c6", "p55");
        List<ConnectionPorts> connectionPortsList44 = new ArrayList<ConnectionPorts>();
        connectionPortsList44.add(connectionPorts55);
        connectionPortsList44.add(connectionPorts66);


        //mock one degree with bidirectional port
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks circuitPacks =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacksBuilder()
                        .setCircuitPackName("c1").build();
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks> circuitPacksList =
                new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks>();
        circuitPacksList.add(circuitPacks);

        final Degree ordmDegreeObject = new DegreeBuilder().setDegreeNumber(Uint16.valueOf(1))
                .setCircuitPacks(circuitPacksList).setConnectionPorts(connectionPortsList).build();

        //mock one srg with bidirectional port
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks srgCircuitPacks =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacksBuilder()
                        .setCircuitPackName("c2").build();
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCircuitPacksList =
                new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>();
        srgCircuitPacksList.add(srgCircuitPacks);

        final SharedRiskGroup ordmSrgObject = new SharedRiskGroupBuilder().setSrgNumber(Uint16.valueOf(1))
                .setCircuitPacks(srgCircuitPacksList).build();

        //mock one degree with 2 unidirectional ports
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks circuitPacks3 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacksBuilder()
                        .setCircuitPackName("c3").build();
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks> circuitPacksList3 =
                new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks>();
        circuitPacksList3.add(circuitPacks3);

        final Degree ordmDegreeObject3 = new DegreeBuilder().setDegreeNumber(Uint16.valueOf(2))
                .setCircuitPacks(circuitPacksList3).setConnectionPorts(connectionPortsList3).build();

        //mock one srg with 2 unidirectional ports
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks srgCircuitPacks4 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacksBuilder()
                        .setCircuitPackName("c4").build();
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCircuitPacksList4 =
                new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>();
        srgCircuitPacksList4.add(srgCircuitPacks4);

        final SharedRiskGroup ordmSrgObject4 = new SharedRiskGroupBuilder().setSrgNumber(Uint16.valueOf(2))
                .setCircuitPacks(srgCircuitPacksList4).build();

        //mock one degree with unidirectional ports, reverse direction
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks circuitPacks5 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacksBuilder()
                        .setCircuitPackName("c5").build();
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks> circuitPacksList5 =
                new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks>();
        circuitPacksList5.add(circuitPacks5);

        final Degree ordmDegreeObject5 = new DegreeBuilder().setDegreeNumber(Uint16.valueOf(3))
                .setCircuitPacks(circuitPacksList5).setConnectionPorts(connectionPortsList33).build();

        //mock one srg with 2 unidirectional ports, reverse direction
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks srgCircuitPacks6 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacksBuilder()
                        .setCircuitPackName("c6").build();
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCircuitPacksList6 =
                new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>();
        srgCircuitPacksList6.add(srgCircuitPacks6);

        final SharedRiskGroup ordmSrgObject6 = new SharedRiskGroupBuilder().setSrgNumber(Uint16.valueOf(3))
                .setCircuitPacks(srgCircuitPacksList6).build();

        //mock lldp configuration
        PortConfig portConfig = new PortConfigBuilder().setIfName("i1")
                .setAdminStatus(PortConfig.AdminStatus.Txandrx).build();
        List<PortConfig> portConfigList = new ArrayList<PortConfig>();
        portConfigList.add(portConfig);

        Protocols protocols =
                new ProtocolsBuilder().addAugmentation(Protocols1.class, new Protocols1Builder()
                        .setLldp(new LldpBuilder().setPortConfig(portConfigList).build()).build()).build();

        //mock responses for deviceTransactionManager calls
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm
                .device.container.org.openroadm.device.Degree> deviceIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm
                                .device.container.org.openroadm.device.Degree.class, new DegreeKey(1));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, deviceIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ordmDegreeObject));

        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm
                .device.container.org.openroadm.device.Degree> deviceIID3 =
                InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm
                                .device.container.org.openroadm.device.Degree.class, new DegreeKey(2));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, deviceIID3,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ordmDegreeObject3));

        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm
                .device.container.org.openroadm.device.Degree> deviceIID5 =
                InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm
                                .device.container.org.openroadm.device.Degree.class, new DegreeKey(3));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, deviceIID5,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ordmDegreeObject5));

        InstanceIdentifier<Protocols> protocoliid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Protocols.class);
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, protocoliid,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(protocols));

        Interface interfaceObject = new InterfaceBuilder().setSupportingCircuitPackName("sc1").build();
        InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(portConfig.getIfName()));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, interfaceIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(interfaceObject));

        InstanceIdentifier<Ports> portID = getChild("c1", "p1");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports));

        InstanceIdentifier<Ports> portID2 = getChild("c3", "p2");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID2,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports2));

        InstanceIdentifier<Ports> portID3 = getChild("c3", "p3");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID3,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports3));

        InstanceIdentifier<Ports> portID22 = getChild("c5", "p22");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID22,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports22));

        InstanceIdentifier<Ports> portID33 = getChild("c5", "p33");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID33,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports33));

        InstanceIdentifier<Ports> portID4 = getChild("c4", "p4");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID4,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports4));

        InstanceIdentifier<Ports> portID5 = getChild("c4", "p5");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID5,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports5));

        InstanceIdentifier<Ports> portID44 = getChild("c6", "p44");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID44, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports44));

        InstanceIdentifier<Ports> portID55 = getChild("c6", "p55");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID55,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports55));

        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, infoIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(info));

        InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c1"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject));

        InstanceIdentifier<CircuitPacks> circuitPacksIID2 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c2"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID2,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject2));

        InstanceIdentifier<CircuitPacks> circuitPacksIID3 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c3"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID3,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject3));

        InstanceIdentifier<CircuitPacks> circuitPacksIID4 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c4"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID4,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject4));

        InstanceIdentifier<CircuitPacks> circuitPacksIID5 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c5"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID5,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject5));

        InstanceIdentifier<CircuitPacks> circuitPacksIID6 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c6"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID6,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject6));

        InstanceIdentifier<SharedRiskGroup> srgIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(SharedRiskGroup.class, new SharedRiskGroupKey(1));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, srgIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ordmSrgObject));

        InstanceIdentifier<SharedRiskGroup> srgIID4 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(SharedRiskGroup.class, new SharedRiskGroupKey(2));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, srgIID4,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ordmSrgObject4));

        InstanceIdentifier<SharedRiskGroup> srgIID6 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(SharedRiskGroup.class, new SharedRiskGroupKey(3));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, srgIID6,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ordmSrgObject6));

        Interface ifc1 = new InterfaceBuilder()
                .setType(OpticalTransport.class).build();
        Interface ifc2 = new InterfaceBuilder()
                .setType(OpenROADMOpticalMultiplex.class).build();
        try {
            when(openRoadmInterfaces.getInterface("node", "i1"))
                    .thenReturn(Optional.of(ifc1));
            when(openRoadmInterfaces.getInterface("node", "i2"))
                    .thenReturn(Optional.of(ifc2));
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("Failed to mock interafce.", e);
            fail();
        }

        //test createMappingData with a node with 3 dgree + 3 srg + bidirectional & unidirectional ports
        assertTrue("creating mappingdata for existed node returns true",
                portMappingVersion121.createMappingData("node"));

        //verify 2 interfaces were processed
        try {
            verify(openRoadmInterfaces).getInterface("node", "i1");
            verify(openRoadmInterfaces).getInterface("node", "i2");
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("Failed to read interface", e);
            fail();
        }

        //assert all portmappings have been created for the roadm node
        ReadTransaction rr = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Network> mappingIID =
                InstanceIdentifier.create(Network.class);
        Network network = new NetworkBuilder().build();
        try {
            Optional<Network> optionalNetwork =
                    rr.read(LogicalDatastoreType.CONFIGURATION, mappingIID).get();
            if (optionalNetwork.isPresent()) {
                network = optionalNetwork.get();
            }

        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to read mapping.", e);
            fail();

        }
        List<String> testMappings = List.of("SRG2-PP1-RX", "SRG3-PP1-RX", "SRG1-PP1-TXRX", "SRG3-PP1-TX",
                "DEG1-TTP-TXRX", "SRG2-PP1-TX", "DEG2-TTP-RX", "DEG2-TTP-TX", "DEG3-TTP-RX", "DEG3-TTP-TX");
        List<String> mappings = new ArrayList<>();
        for (int i = 0; i < testMappings.size(); i++) {
            mappings.add(network.getNodes().get(0).getMapping().get(i).getLogicalConnectionPoint());
        }
        assertTrue("test mapping are equals to mapping" , testMappings.equals(mappings));

        //test updateMapping
        assertTrue("update mapping for node returns true", portMappingVersion121
                .updateMapping("node", network.getNodes().get(0).getMapping().get(0)));

        //test createMapping for non-existent roadm node
        assertFalse("create non existed roadm node returns false" ,
                portMappingVersion121.createMappingData("node2"));

        //test updateMapping for null roadm node
        assertFalse("updating null roadm node returns false",
                portMappingVersion121.updateMapping(null, network.getNodes().get(0).getMapping().get(0)));

    }

    @Test
    public void createMappingDataTestXpdr() {
        //mock node info
        final Info info = getInfo();

        //mock 1 bidirectional port for network
        Ports ports = new PortsBuilder().setPortName("p1").setPortQual(Port.PortQual.XpdrNetwork)
                .setPortDirection(Direction.Bidirectional).build();
        List<Ports> portsList = new ArrayList<>();
        portsList.add(ports);

        //mock 1 bidirectional port for client
        Ports ports11 = new PortsBuilder().setPortName("p11").setPortQual(Port.PortQual.XpdrClient)
                .setPortDirection(Direction.Bidirectional).build();
        List<Ports> portsList11 = new ArrayList<>();
        portsList11.add(ports11);

        //mock 2 unidirectional ports for network
        Ports ports2 = getPorts("p2", Port.PortQual.XpdrNetwork, "c3", "p3", Direction.Rx);
        Ports ports3 = getPorts("p3", Port.PortQual.XpdrNetwork, "c3", "p2", Direction.Tx);
        List<Ports> portsList2 = new ArrayList<>();
        portsList2.add(ports2);
        portsList2.add(ports3);


        //mock 2 unidirectional ports for network, reverse direction
        Ports ports4 = getPorts("p4", Port.PortQual.XpdrNetwork, "c4", "p5", Direction.Tx);
        Ports ports5 = getPorts("p5", Port.PortQual.XpdrNetwork, "c4", "p4", Direction.Rx);
        List<Ports> portsList4 = new ArrayList<>();
        portsList4.add(ports4);
        portsList4.add(ports5);

        //mock connection map
        Destination destination = new DestinationBuilder().setCircuitPackName("c2").setPortName("p11").build();
        List<Destination> destinationList = new ArrayList<>();
        destinationList.add(destination);
        ConnectionMap connectionMap = getConnectionMap(destinationList);
        List<ConnectionMap> connectionMapList = new ArrayList<>();
        connectionMapList.add(connectionMap);

        //mock reponses for deviceTransactionManager
        InstanceIdentifier<Ports> portID = getChild("c1", "p1");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports));

        InstanceIdentifier<Ports> portID11 = getChild("c2", "p11");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID11,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports11));

        InstanceIdentifier<Ports> portID2 = getChild("c3", "p2");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID2,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports2));

        InstanceIdentifier<Ports> portID3 = getChild("c3", "p3");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID3,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports3));

        InstanceIdentifier<Ports> portID4 = getChild("c4", "p4");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID4,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports4));

        InstanceIdentifier<Ports> portID5 = getChild("c4", "p5");
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, portID5,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(ports5));

        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, infoIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(info));

        //mock 4 circuit packs
        CircuitPacks circuitPackObject = getCircuitPacks(portsList, "c1", "pc1");
        CircuitPacks circuitPackObject2 = getCircuitPacks(portsList11, "c2", "pc2");
        CircuitPacks circuitPackObject3 = getCircuitPacks(portsList2, "c3", "pc3");
        CircuitPacks circuitPackObject4 = getCircuitPacks(portsList4, "c4", "pc4");

        InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c1"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject));

        InstanceIdentifier<CircuitPacks> circuitPacksIID2 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c2"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID2,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject2));

        InstanceIdentifier<CircuitPacks> circuitPacksIID3 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c3"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID3,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject3));

        InstanceIdentifier<CircuitPacks> circuitPacksIID4 = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey("c4"));
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, circuitPacksIID4,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(circuitPackObject4));


        List<CircuitPacks> circuitPackArrayList = Arrays.asList(circuitPackObject, circuitPackObject2,
                circuitPackObject3, circuitPackObject4);

        OrgOpenroadmDevice deviceObject = new OrgOpenroadmDeviceBuilder().setCircuitPacks(circuitPackArrayList)
                .setConnectionMap(connectionMapList).build();
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        when(deviceTransactionManager.getDataFromDevice("node",
                    LogicalDatastoreType.OPERATIONAL, deviceIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
                .thenReturn(Optional.of(deviceObject));


        //test createMappingData for xpdr node with 2 network + 1 client + unidirectional & bidirectional ports
        assertTrue("returns true when create mapping ", portMappingVersion121.createMappingData("node"));

        //assert all portmappings have been created for the xpdr node
        ReadTransaction rr = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Network> mappingIID =
                InstanceIdentifier.create(Network.class);
        Network network = new NetworkBuilder().build();
        try {
            Optional<Network> optionalNetwork =
                    rr.read(LogicalDatastoreType.CONFIGURATION, mappingIID).get();
            if (optionalNetwork.isPresent()) {
                network = optionalNetwork.get();
            }

        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Failed to read mapping.", e);
            fail();
        }
        List<String> testMappings = List.of("XPDR1-CLIENT1", "XPDR1-NETWORK5", "XPDR1-NETWORK4", "XPDR1-NETWORK3",
                "XPDR1-NETWORK2", "XPDR1-NETWORK1");
        List<String> mappings = new ArrayList<>();
        for (int i = 0; i < testMappings.size(); i++) {
            mappings.add(network.getNodes().get(0).getMapping().get(i).getLogicalConnectionPoint());
        }
        assertTrue("test mapping are equals to mapping", testMappings.equals(mappings));

    }

    @NonNull
    private KeyedInstanceIdentifier<Ports, PortsKey> getChild(String c4, String p5) {
        return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(c4))
                .child(Ports.class, new PortsKey(p5));
    }

    private ConnectionMap getConnectionMap(List<Destination> destinationList) {
        return new ConnectionMapBuilder().setConnectionMapNumber(Uint32.valueOf(1))
                .setSource(new SourceBuilder().setCircuitPackName("c1").setPortName("p1").build())
                .setDestination(destinationList).build();
    }

    private Info getInfo() {
        return new InfoBuilder().setNodeNumber(Uint32.valueOf(1)).setClli("clli")
                .setNodeType(NodeTypes.Xpdr).setModel("model").setVendor("vendor")
                .setIpAddress(new IpAddress(new Ipv4Address("10.1.1.1")))
                .setMaxDegrees(Uint16.valueOf(2)).setMaxSrgs(Uint16.valueOf(2))
                .setNodeId("node").build();
    }

    private Ports getPorts(String p2, Port.PortQual roadmExternal, String c3, String p3, Direction rx) {
        return new PortsBuilder().setPortName(p2).setPortQual(roadmExternal)
                .setPartnerPort(new PartnerPortBuilder().setCircuitPackName(c3).setPortName(p3).build())
                .setPortDirection(rx).build();
    }

    private ConnectionPorts getConnectionPorts(String c1, String p1) {
        return new ConnectionPortsBuilder().setCircuitPackName(c1).setPortName(p1).build();
    }

    private CircuitPacks getCircuitPacks(List<Ports> portsList, String c1, String pc1) {
        return new CircuitPacksBuilder().setCircuitPackName(c1)
                .setParentCircuitPack(new ParentCircuitPackBuilder()
                        .setCircuitPackName(pc1).build()).setPorts(portsList).build();
    }


    private Ports getPortsWithInterfaces(List<Interfaces> interfacesList) {
        return new PortsBuilder().setPortName("p1").setPortQual(Port.PortQual.RoadmExternal)
                .setPortDirection(Direction.Bidirectional).setInterfaces(interfacesList).build();
    }

    private Info getInfo2() {
        return new InfoBuilder().setNodeNumber(Uint32.valueOf(1)).setClli("clli")
                .setNodeType(NodeTypes.Rdm).setModel("model").setVendor("vendor")
                .setIpAddress(new IpAddress(new Ipv4Address("10.1.1.1")))
                .setMaxDegrees(Uint16.valueOf(3)).setMaxSrgs(Uint16.valueOf(3))
                .build();
    }

    private Interfaces getInterfaces(String i1) {
        return new InterfacesBuilder().setInterfaceName(i1).build();
    }

}
