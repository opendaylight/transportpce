/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import java.util.concurrent.ExecutionException;
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
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerTurndownOutputBuilder;
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

    /*
     * initial setup before test cases
     */
    @Before
    public void setUp() {
        this.mountPoint = new MountPointStub(this.getDataBroker());
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl((this.deviceTransactionManager));
        this.portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        this.powerMgmt = new PowerMgmt(this.getDataBroker(), this.openRoadmInterfaces, this.crossConnect,
            this.deviceTransactionManager);
        this.olmPowerService = new OlmPowerServiceImpl(this.getDataBroker(), this.powerMgmt,
            this.deviceTransactionManager, this.portMapping);
        this.powerMgmtMock = Mockito.mock(PowerMgmt.class);
        this.olmPowerServiceMock = new OlmPowerServiceImpl(this.getDataBroker(), this.powerMgmtMock,
            this.deviceTransactionManager, this.portMapping);
        MockitoAnnotations.initMocks(this);
    }

    /*
     * dummy tests for init and close functions in OlmPowerServiceImpl class (not yet implemented)
     */
    @Test
    public void dummyTest() {
        OlmPowerServiceImpl olmPowerServiceImpl = (OlmPowerServiceImpl) this.olmPowerService;
        olmPowerServiceImpl.init();
        olmPowerServiceImpl.close();
    }

    /*
     * test getPm function in OlmPowerServiceImpl class
     */
    @Test
    public void testGetPm() {
        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        GetPmOutput output = this.olmPowerService.getPm(input);
        Assert.assertEquals(new GetPmOutputBuilder().build(), output);
        Assert.assertEquals(null, output.getResourceId());
    }

    /*
     * test servicePowerSetup function in OlmPowerServiceImpl class in case of mocking powerMgmtMock.setPower
     * to return success
     */
    @Test
    public void testServicePowerSetupSuccess() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        Mockito.when(this.powerMgmtMock.setPower(Mockito.any())).thenReturn(true);
        ServicePowerSetupOutput output = this.olmPowerServiceMock.servicePowerSetup(input);
        Assert.assertEquals(new ServicePowerSetupOutputBuilder().setResult("Success").build(), output);
        Assert.assertEquals("Success", output.getResult());
    }

    /*
     * test servicePowerSetup function in OlmPowerServiceImpl class in case of mocking powerMgmtMock.setPower
     * to return failure
     */
    @Test
    public void testServicePowerSetupFailed() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        Mockito.when(this.powerMgmtMock.setPower(Mockito.any())).thenReturn(false);
        ServicePowerSetupOutput output = this.olmPowerServiceMock.servicePowerSetup(input);
        Assert.assertEquals("Failed", output.getResult());
    }

    /*
     * test getServicePowerTurndownInput function in OlmPowerServiceImpl class in case of mocking powerMgmtMock.setPower
     * to return success
     */
    @Test
    public void testServicePowerTurnDownSuccess() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        Mockito.when(this.powerMgmtMock.powerTurnDown(Mockito.any())).thenReturn(true);
        ServicePowerTurndownOutput output = this.olmPowerServiceMock.servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Success").build(), output);
        Assert.assertEquals("Success", output.getResult());
    }

    /*
     * test getServicePowerTurndownInput function in OlmPowerServiceImpl class in case of mocking powerMgmtMock.setPower
     * to return failure
     */
    @Test
    public void testServicePowerTurnDownFailed() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        Mockito.when(this.powerMgmtMock.powerTurnDown(Mockito.any())).thenReturn(false);
        ServicePowerTurndownOutput output = this.olmPowerServiceMock.servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Failed").build(), output);
        Assert.assertEquals("Failed", output.getResult());
    }

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and network object isn't
     * stored in Configuration Datastore
     */
    @Test
    public void testCalculateSpanlossBase() {
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and network object is
     * stored in Configuration Datastore
     */
    @Test
    public void testCalculateSpanlossBase2() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
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

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and network object is
     * stored in Configuration Datastore with different input
     */
    @Test
    public void testCalculateSpanlossBase3() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
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

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and empty network
     * object is stored in Configuration Datastore
     */
    @Test
    public void testCalculateSpanlossBase4() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
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

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and empty network
     * object is stored in Configuration Datastore with different input
     */
    @Test
    public void testCalculateSpanlossBase5() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
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

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and empty network
     * object is stored in Configuration Datastore with different input
     */
    @Test
    public void testCalculateSpanlossBase5_2() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNetwork3();
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

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and network object of
     * TransactionUtils.getNetwork2() object is stored in Opertional Datastore
     */
    @Test
    public void testCalculateSpanlossBase4_2() throws InterruptedException, ExecutionException {

        TransactionUtils.prepareTestDataInStore(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and network object of
     * TransactionUtils.getNetwork2() object is stored in Opertional Datastore
     */
    @Test
    public void testCalculateSpanlossBase4_3() throws InterruptedException, ExecutionException {

        TransactionUtils.prepareTestDataInStore2(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }


    /*
     * test calculateSpanlossBase function in OlmPowerServiceImpl class in case of failure and network object of
     * TransactionUtils.getNetwork2() object is stored in Opertional Datastore
     */
    @Test
    public void testCalculateSpanlossBase4_4() throws InterruptedException, ExecutionException {

        TransactionUtils.prepareTestDataInStore3(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput2();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
        Assert.assertEquals("Failed", output.getResult());

    }

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return
     */
    @Test
    public void testCalculateSpanlossCurrent1() {
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals(null, output);
    }

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return and the network
     * object is stored in configuration datastore
     */
    @Test
    public void testCalculateSpanlossCurrent2() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
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

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return and the network
     * object is stored in configuration datastore with different network object stored
     */
    @Test
    public void testCalculateSpanlossCurrent2_2() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
            .augmentation(Network1.class)
            .build();
        Network1 network = TransactionUtils.getNetwork3();
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

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return and the network
     * object is stored in configuration datastore
     */
    @Test
    public void testCalculateSpanlossCurrent3() throws InterruptedException, ExecutionException {
        TransactionUtils.prepareTestDataInStore(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals("Failed", output.getResult());
    }

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return and the network
     * object is stored in configuration datastore
     */
    @Test
    public void testCalculateSpanlossCurrent4() throws InterruptedException, ExecutionException {
        TransactionUtils.prepareTestDataInStore2(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals("Failed", output.getResult());
    }

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return and the network
     * object is stored in configuration datastore
     */
    @Test
    public void testCalculateSpanlossCurrent5() throws InterruptedException, ExecutionException {
        TransactionUtils.prepareTestDataInStore3(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals("Failed", output.getResult());
    }

    /*
     * test calculateSpanlossCurrent function in OlmPowerServiceImpl class in case of null return and the network
     * object is stored in configuration datastore
     */
    @Test
    public void testCalculateSpanlossCurrent6() throws InterruptedException, ExecutionException {
        TransactionUtils.prepareTestDataInStore4(this.getDataBroker(), this.mountPointService);
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        CalculateSpanlossCurrentOutput output = this.olmPowerService.calculateSpanlossCurrent(input);
        Assert.assertEquals("Failed", output.getResult());
    }


    /*
     * test servicePowerReset function in OlmPowerServiceImpl class in case of null return
     */
    @Test
    public void testServicePowerReset() {
        ServicePowerResetInput input = OlmPowerServiceRpcImplUtil.getServicePowerResetInput();
        ServicePowerResetOutput output = this.olmPowerService.servicePowerReset(input);
        Assert.assertEquals(null, output);
    }


}
