/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.olm;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
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
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.transportpce.olm.service.OlmPowerServiceImpl;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.stub.RpcProviderRegistryStub;
import org.opendaylight.transportpce.olm.stub.RpcProviderRegistryStub2;
import org.opendaylight.transportpce.test.AbstractTest;

public class OlmProviderTest extends AbstractTest {

    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private OlmPowerService olmPowerService;
    private RpcProviderRegistry rpcProviderRegistry;
    private OlmProvider olmProvider;
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
        this.portMappingVersion22 = new PortMappingVersion221(getDataBroker(), this.deviceTransactionManager,
                this.openRoadmInterfaces);
        this.portMappingVersion121 = new PortMappingVersion121(getDataBroker(), this.deviceTransactionManager,
                this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22, this.mappingUtils,
                this.portMappingVersion121);
        this.powerMgmt = new PowerMgmt(this.getDataBroker(), this.openRoadmInterfaces, this.crossConnect,
            this.deviceTransactionManager);
        this.olmPowerService = new OlmPowerServiceImpl(this.getDataBroker(), this.powerMgmt,
            this.deviceTransactionManager, this.portMapping);
        this.rpcProviderRegistry = new RpcProviderRegistryStub();
        this.olmProvider = new OlmProvider(this.rpcProviderRegistry, this.olmPowerService);

    }


    @Test
    public void testInitAndClose() {
        this.olmProvider.init();
        this.olmProvider.close();
    }


    @Test
    public void testClose2() {
        this.rpcProviderRegistry = new RpcProviderRegistryStub2();
        this.olmProvider = new OlmProvider(this.rpcProviderRegistry, this.olmPowerService);
        this.olmProvider.init();
        this.olmProvider.close();
    }

}
