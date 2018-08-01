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
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.renderer.utils.ServiceImplementationDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.ServicePathInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.ServicePathOutput;

public class DeviceRendererServiceImplSetupTest extends AbstractTest {

    private DeviceTransactionManager deviceTransactionManager;

    private DeviceRendererService deviceRendererService;
    private CrossConnect crossConnect;
    private PortMapping portMapping;
    private OpenRoadmInterfaces openRoadmInterfaces;


    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(this.deviceTransactionManager);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
                this.openRoadmInterfaces);
        this.portMapping = Mockito.spy(this.portMapping);
        OpenRoadmInterfaceFactory openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(this.portMapping,
            this.openRoadmInterfaces);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.crossConnect = Mockito.spy(this.crossConnect);
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
        String srcTP = OpenRoadmInterfacesImpl.TTP_TOKEN;
        String dstTp = OpenRoadmInterfacesImpl.PP_TOKEN;
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
        String srcTP = OpenRoadmInterfacesImpl.TTP_TOKEN;
        String dstTp = OpenRoadmInterfacesImpl.PP_TOKEN;
        MountPointUtils.writeMapping(nodeId, srcTP, this.deviceTransactionManager);
        MountPointUtils.writeMapping(nodeId, dstTp, this.deviceTransactionManager);
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
        Mockito.doReturn(java.util.Optional.empty()).when(this.crossConnect).postCrossConnect(nodeId, 20L, srcTP,
            dstTp);
        ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                    ServicePathDirection.A_TO_Z);
        Assert.assertFalse(servicePathOutput.isSuccess());
    }

    @Test
    public void testSetupService() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        Mockito.doNothing().when(this.openRoadmInterfaces).postEquipmentState(Mockito.anyString(),
            Mockito.anyString(), Mockito.anyBoolean());
        String [] interfaceTokens = {
            OpenRoadmInterfacesImpl.NETWORK_TOKEN,
            OpenRoadmInterfacesImpl.CLIENT_TOKEN,
            OpenRoadmInterfacesImpl.TTP_TOKEN,
            OpenRoadmInterfacesImpl.PP_TOKEN
        };

        String nodeId = "node1";

        for (String srcToken : interfaceTokens) {
            String srcTP = "src-" + srcToken;
            MountPointUtils.writeMapping(nodeId, srcTP, this.deviceTransactionManager);
            for (String dstToken : interfaceTokens) {
                String dstTp = "dst-" + dstToken;
                MountPointUtils.writeMapping(nodeId, dstTp, this.deviceTransactionManager);

                boolean connectingUsingCrossConnect = true;
                if (OpenRoadmInterfacesImpl.NETWORK_TOKEN.equals(srcToken)
                        || OpenRoadmInterfacesImpl.CLIENT_TOKEN.equals(srcToken)) {
                    connectingUsingCrossConnect = false;
                }
                if (OpenRoadmInterfacesImpl.NETWORK_TOKEN.equals(dstToken)
                        || OpenRoadmInterfacesImpl.CLIENT_TOKEN.equals(dstToken)) {
                    connectingUsingCrossConnect = false;
                }

                List<Nodes> nodes = new ArrayList<>();
                nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
                ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);

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
