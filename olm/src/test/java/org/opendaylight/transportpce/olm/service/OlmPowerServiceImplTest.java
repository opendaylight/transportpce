/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmPowerServiceImplTest  extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerServiceImplTest.class);
    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private OlmPowerService olmPowerService;
    private PowerMgmt powerMgmtMock;
    @InjectMocks
    private OlmPowerService olmPowerServiceMock;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;



    @Before
    public void setUp() {
        this.mountPoint = new MountPointStub(this.getDataBroker());
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.crossConnectImpl121 = new CrossConnectImpl121(this.deviceTransactionManager);
        this.crossConnectImpl221 = new CrossConnectImpl221(this.deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager, this.mappingUtils,
                this.crossConnectImpl121, this.crossConnectImpl221);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(this.deviceTransactionManager);
        this.openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(this.deviceTransactionManager);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
                openRoadmInterfacesImpl121, openRoadmInterfacesImpl221);
        this.portMappingVersion22 =
                new PortMappingVersion221(getDataBroker(), this.deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
                new PortMappingVersion121(getDataBroker(), this.deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22, this.mappingUtils,
                this.portMappingVersion121);
        this.powerMgmt = new PowerMgmt(this.getDataBroker(), this.openRoadmInterfaces, this.crossConnect,
            this.deviceTransactionManager);
        this.olmPowerService = new OlmPowerServiceImpl(this.getDataBroker(), this.powerMgmt,
            this.deviceTransactionManager, this.portMapping);
        this.powerMgmtMock = Mockito.mock(PowerMgmt.class);
        this.olmPowerServiceMock = new OlmPowerServiceImpl(this.getDataBroker(), this.powerMgmtMock,
            this.deviceTransactionManager, this.portMapping);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void dummyTest() {
        OlmPowerServiceImpl olmPowerServiceImpl = (OlmPowerServiceImpl) this.olmPowerService;
        olmPowerServiceImpl.init();
        olmPowerServiceImpl.close();
    }


    @Test
    public void testGetPm() {
        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        GetPmOutput output = this.olmPowerService.getPm(input);
        Assert.assertEquals(new GetPmOutputBuilder().build(), output);
        Assert.assertEquals(null, output.getResourceId());
    }

    @Test
    public void testServicePowerSetupSuccess() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        Mockito.when(this.powerMgmtMock.setPower(Mockito.any())).thenReturn(true);
        ServicePowerSetupOutput output = this.olmPowerServiceMock.servicePowerSetup(input);
        Assert.assertEquals(new ServicePowerSetupOutputBuilder().setResult("Success").build(), output);
        Assert.assertEquals("Success", output.getResult());
    }

    @Test
    public void testServicePowerSetupFailed() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        Mockito.when(this.powerMgmtMock.setPower(Mockito.any())).thenReturn(false);
        ServicePowerSetupOutput output = this.olmPowerServiceMock.servicePowerSetup(input);
        Assert.assertEquals("Failed", output.getResult());
    }

    @Test
    public void testServicePowerTurnDownSuccess() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        Mockito.when(this.powerMgmtMock.powerTurnDown(Mockito.any())).thenReturn(true);
        ServicePowerTurndownOutput output = this.olmPowerServiceMock.servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Success").build(), output);
        Assert.assertEquals("Success", output.getResult());
    }

    @Test
    public void testServicePowerTurnDownFailed() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        Mockito.when(this.powerMgmtMock.powerTurnDown(Mockito.any())).thenReturn(false);
        ServicePowerTurndownOutput output = this.olmPowerServiceMock.servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Failed").build(), output);
        Assert.assertEquals("Failed", output.getResult());
    }

    @Test
    public void testCalculateSpanlossBase() {
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

    @Test
    public void testCalculateSpanlossBase2() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNetwork();
        TransactionUtils.writeTransaction(this.getDataBroker(), networkIID, network);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !",e);
        }
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

    @Test
    public void testCalculateSpanlossBase3() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNetwork();
        TransactionUtils.writeTransaction(this.getDataBroker(), networkIID, network);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !",e);
        }
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }


    @Test
    public void testCalculateSpanlossBase4() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getEmptyNetwork();
        TransactionUtils.writeTransaction(this.getDataBroker(), networkIID, network);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !",e);
        }
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

    @Test
    public void testCalculateSpanlossBase5() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNullNetwork();
        TransactionUtils.writeTransaction(this.getDataBroker(), networkIID, network);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !",e);
        }
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

//    @Test
//    public void testCalculateSpanlossBase4() throws InterruptedException {
//        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
//        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
//            .augmentation(Network1.class)
//            .build();
//        Network1 network = TransactionUtils.getNetwork2();
//        TransactionUtils.writeTransaction(this.getDataBroker(), networkIID, network);
//
//        Thread.sleep(500);
//
//        List<NodeId> nodes = TransactionUtils.getNodes();
//        Node n;
//        SupportingNode sn;
//
//        List<SupportingNode> snl;
//        NodeId n5 = new NodeId("node 5");
//        KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII2 =
//            InstanceIdentifiers.UNDERLAY_NETWORK_II.child(Node.class, new NodeKey(n5));
//
//        sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
//            .setNodeRef(n5).build();
//        snl = new ArrayList<>();
//        snl.add(sn);
//        n = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
//            .network.NodeBuilder().setNodeId(n5).build();
//        TransactionUtils.writeTransaction(this.getDataBroker(), mappedNodeII2, n);
//        Thread.sleep(500);
//
//        for(int i=0; i < nodes.size(); i++) {
//            KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII =
//                InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(nodes.get(i)));
//            if (i != 0){
//                sn = new SupportingNodeBuilder().setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
//                    .setNodeRef(nodes.get(i-1)).build();
//            }else {
//                sn = new SupportingNodeBuilder().setNodeRef(n5).build();
//            }
//            snl = new ArrayList<>();
//            snl.add(sn);
//            n = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
//                .network.NodeBuilder().setNodeId(nodes.get(i)).setSupportingNode(snl).build();
//            TransactionUtils.writeTransaction(this.getDataBroker(), mappedNodeII, n);
//            Thread.sleep(500);
//        }
//
//        Thread.sleep(1000);
//
//        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
//        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
//        Assert.assertEquals("Failed", output.getResult());
//
//    }

    @Test
    public void testCalculateSpanlossCurrent1() {
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals(null, output);
    }

    @Test
    public void testCalculateSpanlossCurrent2() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNetwork();
        TransactionUtils.writeTransaction(this.getDataBroker(), networkIID, network);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !",e);
        }
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals(null, output);
    }

    @Test
    public void testServicePowerReset() {
        ServicePowerResetInput input = OlmPowerServiceRpcImplUtil.getServicePowerResetInput();
        ServicePowerResetOutput output = this.olmPowerService.servicePowerReset(input);
        Assert.assertEquals(null, output);
    }
}
