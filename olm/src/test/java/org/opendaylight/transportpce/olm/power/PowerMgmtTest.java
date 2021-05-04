/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
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
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;

public class PowerMgmtTest extends AbstractTest {
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
    private DataBroker dataBroker;

    @Before
    public void setUp() {
        dataBroker = this.getNewDataBroker();
        this.mountPoint = new MountPointStub(dataBroker);
        this.mountPointService = new MountPointServiceStub(mountPoint);
        // this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.mappingUtils = Mockito.spy(new MappingUtilsImpl(dataBroker));
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
                new PortMappingVersion221(dataBroker, deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
                new PortMappingVersion121(dataBroker, deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion710,
                this.portMappingVersion22, this.portMappingVersion121);
        this.portMapping = Mockito.spy(this.portMapping);
        this.powerMgmt = new PowerMgmtImpl(this.dataBroker, this.openRoadmInterfaces, this.crossConnect,
                this.deviceTransactionManager);
    }

    @Test
    public void testSetPower() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPower2() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPower3() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPower4() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testPowerTurnDown() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        boolean output = this.powerMgmt.powerTurnDown(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testPowerTurnDown2() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput2();
        boolean output = this.powerMgmt.powerTurnDown(input);
        Assert.assertEquals(false, output);
    }

    @Test
    public void testPowerTurnDown3() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput3();
        boolean output = this.powerMgmt.powerTurnDown(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testPowerTurnDown4() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput4();
        boolean output = this.powerMgmt.powerTurnDown(input);
        Assert.assertEquals(false, output);
    }

    @Test
    public void testSetPowerPresentNodes() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.dataBroker, null);
            Thread.sleep(1000);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes2() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction2(nodeId.getValue(), this.dataBroker, null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes3() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.dataBroker, null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }
    /*
    @Test
    public void testSetPowerPresentNodes31() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }*/

    @Test
    public void testSetPowerPresentNodes312() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.dataBroker, "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes32() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.dataBroker, null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes4() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.dataBroker, "network");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
    @Test
    public void testSetPowerPresentNodes41() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }*/

    @Test
    public void testSetPowerPresentNodes42() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.dataBroker, "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes422() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.dataBroker, "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes43() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.dataBroker, null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

}
