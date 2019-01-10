/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.impl;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.pce.stub.RpcProviderRegistryStub;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PceService;

public class PceProviderTest extends AbstractTest {

    private RpcProviderRegistry rpcProviderRegistry;
    private NotificationPublishService notificationPublishService;
    private PathComputationService pathComputationService;
    private BindingAwareBroker.RpcRegistration<PceService> rpcRegistration;
    private PceProvider pceProvider;

    @Before
    public void setUp() {
        rpcProviderRegistry = new RpcProviderRegistryStub();
        notificationPublishService = this.getDataStoreContextUtil().createNotificationPublishService();
        pathComputationService = new PathComputationServiceImpl(this.getDataBroker(), notificationPublishService);
        pceProvider = new PceProvider(rpcProviderRegistry, pathComputationService);
    }

    @Test
    public void testIntialization(){
        pceProvider.init();
        pceProvider.close();
    }
}
