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
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
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
