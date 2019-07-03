/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexImpl;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexInterface;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface121;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.renderer.utils.ServiceImplementationDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.Nodes;

public class DeviceRendererServiceImplSetupTest extends AbstractTest {

    private DeviceTransactionManager deviceTransactionManager;

    private DeviceRendererService deviceRendererService;
    private CrossConnect crossConnect;
    private PortMapping portMapping;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;


    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        this.openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(deviceTransactionManager);
        this.mappingUtils = Mockito.spy(MappingUtils.class);

        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(mappingUtils)
            .getOpenRoadmVersion(Mockito.anyString());

        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMappingVersion22 =
            new PortMappingVersion221(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
            new PortMappingVersion121(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22,
            this.portMappingVersion121);
        this.portMapping = Mockito.spy(this.portMapping);
        this.crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        this.crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(deviceTransactionManager, this.mappingUtils, this.crossConnectImpl121,
            this.crossConnectImpl221);
        this.crossConnect = Mockito.spy(this.crossConnect);


        FixedFlexInterface fixedFlexInterface = new FixedFlexImpl();
        OpenRoadmInterface121 openRoadmInterface121 = new OpenRoadmInterface121(portMapping,openRoadmInterfaces);
        OpenRoadmInterface221 openRoadmInterface221 = new OpenRoadmInterface221(portMapping,openRoadmInterfaces,
            fixedFlexInterface);
        OpenRoadmInterfaceFactory openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(this.mappingUtils,
            openRoadmInterface121,openRoadmInterface221);

        this.deviceRendererService = new DeviceRendererServiceImpl(this.getDataBroker(),
            this.deviceTransactionManager, openRoadmInterfaceFactory, openRoadmInterfaces, crossConnect,
            portMapping);
    }

    @Test
    public void testSetupServiceWhenDeviceIsNotMounted() {
        setMountPoint(null);
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs();
        for (ServicePathDirection servicePathDirection : ServicePathDirection.values()) {
            ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                servicePathDirection);
            Assert.assertFalse(servicePathOutput.isSuccess());
            Assert.assertEquals("node1 is not mounted on the controller",
                servicePathOutput.getResult());
        }
    }

    @Test
    public void testSetupServicemptyPorts() {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        String nodeId = "node1";
        String srcTP = StringConstants.TTP_TOKEN;
        String dstTp = StringConstants.PP_TOKEN;
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
        for (ServicePathDirection servicePathDirection : ServicePathDirection.values()) {
            ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                servicePathDirection);
            Assert.assertFalse(servicePathOutput.isSuccess());
        }
    }

    @Test
    public void testSetupServiceCannotCrossConnect() {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        String nodeId = "node1";
        String srcTP = StringConstants.TTP_TOKEN;
        String dstTp = StringConstants.PP_TOKEN;
        MountPointUtils.writeMapping(nodeId, srcTP, this.deviceTransactionManager);
        MountPointUtils.writeMapping(nodeId, dstTp, this.deviceTransactionManager);
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(this.mappingUtils)
            .getOpenRoadmVersion("node1");
        Mockito.doReturn(java.util.Optional.empty()).when(this.crossConnect).postCrossConnect(nodeId, 20L, srcTP,
            dstTp);
        ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
            ServicePathDirection.A_TO_Z);
        Assert.assertFalse(servicePathOutput.isSuccess());
    }

    @Ignore("need to be reviewed")
    @Test
    public void testSetupService() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        Mockito.doNothing().when(this.openRoadmInterfaces).postEquipmentState(Mockito.anyString(),
            Mockito.anyString(), Mockito.anyBoolean());
        String [] interfaceTokens = {
            StringConstants.NETWORK_TOKEN,
            StringConstants.CLIENT_TOKEN,
            StringConstants.TTP_TOKEN,
            StringConstants.PP_TOKEN
        };

        String nodeId = "node1";

        for (String srcToken : interfaceTokens) {
            String srcTP = "src-" + srcToken;
            MountPointUtils.writeMapping(nodeId, srcTP, this.deviceTransactionManager);
            for (String dstToken : interfaceTokens) {
                String dstTp = "dst-" + dstToken;
                MountPointUtils.writeMapping(nodeId, dstTp, this.deviceTransactionManager);

                boolean connectingUsingCrossConnect = true;
                if (StringConstants.NETWORK_TOKEN.equals(srcToken)
                    || StringConstants.CLIENT_TOKEN.equals(srcToken)) {
                    connectingUsingCrossConnect = false;
                }
                if (StringConstants.NETWORK_TOKEN.equals(dstToken)
                    || StringConstants.CLIENT_TOKEN.equals(dstToken)) {
                    connectingUsingCrossConnect = false;
                }

                List<Nodes> nodes = new ArrayList<>();
                nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
                ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
                Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(this.mappingUtils)
                    .getOpenRoadmVersion("node1");
                for (ServicePathDirection servicePathDirection : ServicePathDirection.values()) {
                    ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                        servicePathDirection);
                    Assert.assertTrue(servicePathOutput.isSuccess());
                    String expectedResult = "Roadm-connection successfully created for nodes: ";
                    if (connectingUsingCrossConnect) {
                        expectedResult = expectedResult + nodeId;
                    }
                    Assert.assertEquals(expectedResult, servicePathOutput.getResult());
                    Assert.assertEquals(1, servicePathOutput.getNodeInterface().size());
                    Assert.assertEquals(nodeId, servicePathOutput.getNodeInterface().get(0).getNodeId());
                    if (!connectingUsingCrossConnect) { // No need to try both directions if not cross connect
                        break;
                    }
                }
            }
        }
    }

    @Test
    public void testSetupServiceNulls() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        String nodeId = "node1";
        String srcTP = null;
        String dstTp = null;
        boolean connectingUsingCrossConnect = true;

        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(this.mappingUtils)
            .getOpenRoadmVersion("node1");
        for (ServicePathDirection servicePathDirection : ServicePathDirection.values()) {
            ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                servicePathDirection);
            Assert.assertTrue(servicePathOutput.isSuccess());
            String expectedResult = "Roadm-connection successfully created for nodes: ";
            expectedResult = expectedResult + nodeId;
            Assert.assertEquals(expectedResult, servicePathOutput.getResult());
            Assert.assertEquals(1, servicePathOutput.getNodeInterface().size());
            Assert.assertEquals(nodeId, servicePathOutput.getNodeInterface().get(0).getNodeId());
        }
    }
}