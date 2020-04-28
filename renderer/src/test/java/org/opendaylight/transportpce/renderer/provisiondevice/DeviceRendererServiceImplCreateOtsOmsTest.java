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
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
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
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
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
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmOtnInterface221;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.utils.CreateOtsOmsDataUtils;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.CreateOtsOmsOutput;

public class DeviceRendererServiceImplCreateOtsOmsTest extends AbstractTest {

    private DeviceRendererService deviceRendererService;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private DeviceTransactionManager deviceTransactionManager;
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
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(this.deviceTransactionManager);
        this.openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(this.deviceTransactionManager);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.mappingUtils = Mockito.spy(MappingUtils.class);

        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(mappingUtils)
                .getOpenRoadmVersion(Mockito.anyString());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMappingVersion22 = new PortMappingVersion221(getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        this.portMappingVersion121 = new PortMappingVersion121(getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        PortMapping portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22,
            this.portMappingVersion121);
        FixedFlexInterface fixedFlexInterface = new FixedFlexImpl();
        OpenRoadmInterface121 openRoadmInterface121 = new OpenRoadmInterface121(portMapping,openRoadmInterfaces);
        OpenRoadmInterface221 openRoadmInterface221 = new OpenRoadmInterface221(portMapping,openRoadmInterfaces,
            fixedFlexInterface);
        OpenRoadmOtnInterface221 openRoadmOTNInterface = new OpenRoadmOtnInterface221(portMapping, openRoadmInterfaces);
        this.openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(this.mappingUtils,openRoadmInterface121,
            openRoadmInterface221, openRoadmOTNInterface);

        this.crossConnectImpl121 = new CrossConnectImpl121(this.deviceTransactionManager);
        this.crossConnectImpl221 = new CrossConnectImpl221(this.deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager, this.mappingUtils,
            this.crossConnectImpl121, this.crossConnectImpl221);
        this.crossConnect = Mockito.spy(this.crossConnect);
        this.deviceRendererService = new DeviceRendererServiceImpl(this.getDataBroker(),
            this.deviceTransactionManager, this.openRoadmInterfaceFactory, this.openRoadmInterfaces,
            this.crossConnect, portMapping, null);
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
