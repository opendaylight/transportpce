/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl710;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion710;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.OlmUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({OlmUtils.class,PowerMgmtVersion121.class})
@PowerMockIgnore("org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.*")
public class PowerMgmtPowerMockTest extends AbstractTest {

    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl22;
    private CrossConnectImpl710 crossConnectImpl710;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl22;
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;
    private PortMappingVersion710 portMappingVersion710;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;

    @Before
    public void setUp() {
        this.mountPoint = new MountPointStub(this.getDataBroker());
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.mappingUtils = Mockito.spy(new MappingUtilsImpl(getDataBroker()));
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(mappingUtils)
                .getOpenRoadmVersion(Mockito.anyString());
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        this.crossConnectImpl22 = new CrossConnectImpl221(deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(deviceTransactionManager, this.mappingUtils, this.crossConnectImpl121,
                this.crossConnectImpl22, this.crossConnectImpl710);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        this.openRoadmInterfacesImpl22 = new OpenRoadmInterfacesImpl221(deviceTransactionManager);
        this.openRoadmInterfacesImpl710 = new OpenRoadmInterfacesImpl710(deviceTransactionManager);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl((this.deviceTransactionManager),
                this.mappingUtils,this.openRoadmInterfacesImpl121,this.openRoadmInterfacesImpl22,
            this.openRoadmInterfacesImpl710);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMappingVersion22 =
                new PortMappingVersion221(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
                new PortMappingVersion121(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion710 =
            new PortMappingVersion710(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion710,
            this.portMappingVersion22, this.portMappingVersion121);
        this.portMapping = Mockito.spy(this.portMapping);
        this.powerMgmt = new PowerMgmtImpl(this.getDataBroker(), this.openRoadmInterfaces, this.crossConnect,
                this.deviceTransactionManager);
    }

    @Test
    public void testSetPowerMockingUtil() {
        PowerMockito.mockStatic(OlmUtils.class);
        PowerMockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getXpdrNodesFromNodesBuilderDeg()));
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);

    }

    @Test
    public void testSetPowerMockingUtilNetwokType() throws OpenRoadmInterfaceException {
        PowerMockito.mockStatic(OlmUtils.class);
        PowerMockito.mockStatic(PowerMgmtVersion121.class);
        PowerMockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getXpdrNodesFromNodesBuilderNetwork()));
        Map<String, Double> txPowerRangeMap = new HashMap<>();
        PowerMockito.when(PowerMgmtVersion121.getXponderPowerRange(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                ArgumentMatchers.eq(deviceTransactionManager)))
                .thenReturn(txPowerRangeMap);
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121Spy = PowerMockito.mock(OpenRoadmInterfacesImpl121.class);
        Mockito.when(openRoadmInterfacesImpl121Spy.getInterface(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());
        PowerMgmtImpl powerMgmtImpl = getNewPowerMgmt(openRoadmInterfacesImpl121Spy,this.crossConnect);
        boolean output = powerMgmtImpl.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPowerMockingUtilNetwokTypeMoreThanOneNode() throws OpenRoadmInterfaceException {
        PowerMockito.mockStatic(OlmUtils.class);
        PowerMockito.mockStatic(PowerMgmtVersion121.class);
        PowerMockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getXpdrNodesFromNodesBuilderNetwork()));
        Map<String, Double> txPowerRangeMap = new HashMap<>();
        PowerMockito.when(PowerMgmtVersion121
                .getXponderPowerRange(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        ArgumentMatchers.eq(deviceTransactionManager)))
                .thenReturn(txPowerRangeMap);
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121Spy = PowerMockito.mock(OpenRoadmInterfacesImpl121.class);
        Mockito.when(openRoadmInterfacesImpl121Spy.getInterface(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());
        PowerMgmtImpl powerMgmtImpl = getNewPowerMgmt(openRoadmInterfacesImpl121Spy,this.crossConnect);
        boolean output = powerMgmtImpl.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPowerXpdrNodes() {
        PowerMockito.mockStatic(OlmUtils.class);
        PowerMockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getXpdrNodesFromNodesBuilderDeg()));
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPowerRdmNodesReturnInterfaceEmpty() throws OpenRoadmInterfaceException {
        PowerMockito.mockStatic(OlmUtils.class);
        Mockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getRdmNodesFromNodesBuilder()));
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121Spy = PowerMockito.mock(OpenRoadmInterfacesImpl121.class);
        Mockito.when(openRoadmInterfacesImpl121Spy.getInterface(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());
        PowerMgmtImpl powerMgmtImpl = getNewPowerMgmt(openRoadmInterfacesImpl121Spy,this.crossConnect);
        boolean output = powerMgmtImpl.setPower(input);
        Assert.assertEquals(false, output);
    }

    @Test
    public void testSetPowerRdmNodesThrowsException() throws OpenRoadmInterfaceException {
        PowerMockito.mockStatic(OlmUtils.class);
        Mockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getRdmNodesFromNodesBuilder()));
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121Spy = PowerMockito.mock(OpenRoadmInterfacesImpl121.class);
        Mockito.when(openRoadmInterfacesImpl121Spy.getInterface(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new OpenRoadmInterfaceException("error thrown by unit tests "));
        PowerMgmtImpl powerMgmtImpl = getNewPowerMgmt(openRoadmInterfacesImpl121Spy,this.crossConnect);
        boolean output = powerMgmtImpl.setPower(input);
        Assert.assertEquals(false, output);
    }

    @Test
    public void testSetPowerRdmNodesReturnInterface() throws OpenRoadmInterfaceException {
        PowerMockito.mockStatic(OlmUtils.class);
        Mockito.when(OlmUtils.getNode(Mockito.anyString(), ArgumentMatchers.eq(getDataBroker())))
                .thenReturn(Optional.of(getRdmNodesFromNodesBuilder()));
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121Spy = PowerMockito.mock(OpenRoadmInterfacesImpl121.class);

        Ots ots = new OtsBuilder().setSpanLossTransmit(new RatioDB(new BigDecimal(23))).build();
        Interface1Builder intf1Builder = new Interface1Builder();
        Mockito.when(openRoadmInterfacesImpl121Spy.getInterface(Mockito.anyString(), Mockito.anyString())).thenReturn(
                Optional.of(new InterfaceBuilder().addAugmentation(intf1Builder.setOts(ots).build())
                        .build()));
        CrossConnect crossConnectMock = Mockito.mock(CrossConnectImpl.class);
        Mockito.when(crossConnectMock
                .setPowerLevel(Mockito.anyString(), OpticalControlMode.Power.getName(), Mockito.any(),
                        Mockito.anyString())).thenReturn(true);
        PowerMgmtImpl powerMgmtImpl = getNewPowerMgmt(openRoadmInterfacesImpl121Spy,crossConnectMock);
        boolean output = powerMgmtImpl.setPower(input);
        Assert.assertEquals(true, output);
    }

    private PowerMgmtImpl getNewPowerMgmt(OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121Spy,
            CrossConnect crossConnectMock) {
        OpenRoadmInterfacesImpl openRoadmInterfacesSpy = new OpenRoadmInterfacesImpl((this.deviceTransactionManager),
                this.mappingUtils, openRoadmInterfacesImpl121Spy, this.openRoadmInterfacesImpl22,
            this.openRoadmInterfacesImpl710);
        openRoadmInterfacesSpy = PowerMockito.spy(openRoadmInterfacesSpy);
        return new PowerMgmtImpl(this.getDataBroker(), openRoadmInterfacesSpy, crossConnectMock,
                this.deviceTransactionManager);
    }

    private Nodes getXpdrNodesFromNodesBuilderDeg() {
        MappingBuilder mappingBuilder = getMappingBuilderDeg();
        Mapping mapping = mappingBuilder.build();
        return new NodesBuilder().setNodeId("node 1")
                .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).build())
                .setMapping(Map.of(mapping.key(),mapping))
                .build();
    }

    private Nodes getXpdrNodesFromNodesBuilderNetwork() {
        MappingBuilder mappingBuilder = getMappingBuilderNetWork();
        Mapping mapping = mappingBuilder.build();
        return new NodesBuilder().setNodeId("node 1")
                .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr)
                        .setOpenroadmVersion(OpenroadmNodeVersion._121)
                        .build())
                .setMapping(Map.of(mapping.key(),mapping))
                .build();
    }

    private Nodes getRdmNodesFromNodesBuilder() {
        MappingBuilder mappingBuilder = getMappingBuilderDeg();
        Mapping mapping = mappingBuilder.build();
        return new NodesBuilder().setNodeId("node 1").setNodeInfo(
                new NodeInfoBuilder().setNodeType(NodeTypes.Rdm)
                        .setOpenroadmVersion(OpenroadmNodeVersion._121)
                        .build())
                .setMapping(Map.of(mapping.key(),mapping))
                .build();
    }

    private MappingBuilder getMappingBuilderDeg() {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.withKey(new MappingKey("deg"));
        mappingBuilder.setLogicalConnectionPoint("logicalConnPoint");
        mappingBuilder.setSupportingOts("OTS");
        mappingBuilder.setSupportingCircuitPackName("2/0");
        mappingBuilder.setSupportingOms("OMS");
        mappingBuilder.setSupportingPort("8080");
        mappingBuilder.setSupportingCircuitPackName("circuit1");
        InstanceIdentifier<Mapping> portMappingIID =
                InstanceIdentifier.builder(Network.class).child(Nodes.class, new NodesKey("node 1"))
                        .child(Mapping.class, new MappingKey("deg")).build();
        return mappingBuilder;
    }

    private MappingBuilder getMappingBuilderNetWork() {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.withKey(new MappingKey("network"));
        mappingBuilder.setLogicalConnectionPoint("logicalConnPoint");
        mappingBuilder.setSupportingOts("OTS");
        mappingBuilder.setSupportingCircuitPackName("2/0");
        mappingBuilder.setSupportingOms("OMS");
        mappingBuilder.setSupportingPort("8080");
        mappingBuilder.setSupportingCircuitPackName("circuit1");
        InstanceIdentifier<Mapping> portMappingIID =
                InstanceIdentifier.builder(Network.class).child(Nodes.class, new NodesKey("node 1"))
                        .child(Mapping.class, new MappingKey("network")).build();
        return mappingBuilder;
    }

}
