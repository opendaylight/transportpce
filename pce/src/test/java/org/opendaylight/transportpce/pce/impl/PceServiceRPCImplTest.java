/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.DataUtils;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class PceServiceRPCImplTest extends AbstractTest {

    private NotificationPublishService notificationPublishService;
    private PathComputationService pathComputationService;
    private PceServiceRPCImpl pceServiceRPCImpl;

    @Before
    public void setUp() {
        notificationPublishService = this.getDataStoreContextUtil().createNotificationPublishService();
        pathComputationService = new PathComputationServiceImpl(this.getDataBroker(), notificationPublishService);
        pceServiceRPCImpl = new PceServiceRPCImpl(pathComputationService);
    }

    @Test
    public void testCancelResourceReserve() throws ExecutionException, InterruptedException {

        CancelResourceReserveInput input = DataUtils.getCancelResourceReserveInput();
        ListenableFuture<RpcResult<CancelResourceReserveOutput>> output = pceServiceRPCImpl
            .cancelResourceReserve(input);
        Assert.assertEquals(true, output.get().isSuccessful());

    }

    @Test
    public void testPathComputationRequest() throws ExecutionException, InterruptedException {
        PathComputationRequestInput input = PceTestData.getPCE_simpletopology_test1_request();
        ListenableFuture<RpcResult<PathComputationRequestOutput>> output = pceServiceRPCImpl
            .pathComputationRequest(input);
        Assert.assertEquals(true, output.get().isSuccessful());
    }

}
