/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
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
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.utils.CreateOtsOmsDataUtils;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.CreateOtsOmsOutput;

public class DeviceRendererServiceImplCreateOtsOmsTest extends AbstractTest {

    private DeviceRendererService deviceRendererService;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private DeviceTransactionManager deviceTransactionManager;

    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(this.deviceTransactionManager);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        PortMapping portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
                this.openRoadmInterfaces);
        this.openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(portMapping,
                this.openRoadmInterfaces);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.crossConnect = Mockito.spy(this.crossConnect);
        this.deviceRendererService = new DeviceRendererServiceImpl(this.getDataBroker(),
            this.deviceTransactionManager, this.openRoadmInterfaceFactory, this.openRoadmInterfaces,
            this.crossConnect, portMapping);
    }

    @Test
    public void testCreateOtsOmsWhenDeviceIsNotMounted() throws OpenRoadmInterfaceException {
        setMountPoint(null);
        CreateOtsOmsInput input = CreateOtsOmsDataUtils.buildCreateOtsOms();
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals("node 1 is not mounted on the controller",
                result.getResult());
    }

    @Test
    public void testCreateOtsOmsWhenDeviceIsMountedWithNoMapping() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        CreateOtsOmsInput input = CreateOtsOmsDataUtils.buildCreateOtsOms();
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        Assert.assertFalse(result.isSuccess());
    }

    @Test
    public void testCreateOtsOmsWhenDeviceIsMountedWithMapping() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        CreateOtsOmsInput input = CreateOtsOmsDataUtils.buildCreateOtsOms();
        writePortMapping(input);
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        Assert.assertTrue(result.isSuccess());
    }

    private void writePortMapping(CreateOtsOmsInput input) {
        MountPointUtils.writeMapping(
            input.getNodeId(),
            input.getLogicalConnectionPoint(),
            this.deviceTransactionManager
        );
    }

}
