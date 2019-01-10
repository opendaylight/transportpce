/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.service;

import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.pce.utils.*;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.*;

public class PathComputationServiceImplTest extends AbstractTest {

    private NotificationPublishService notificationPublishService;
    private PathComputationServiceImpl pathComputationServiceImpl;

    @Before
    public void setUp() {
        notificationPublishService = this.getDataStoreContextUtil().createNotificationPublishService();
        pathComputationServiceImpl = new PathComputationServiceImpl(this.getDataBroker(), notificationPublishService);
    }

    @Test
    public void dummyTest(){
        pathComputationServiceImpl.init();
        pathComputationServiceImpl.close();
    }


    @Test
    public void testCancelResourceReserve(){
        CancelResourceReserveInput input = DataUtils.getCancelResourceReserveInput();
        CancelResourceReserveOutput output = pathComputationServiceImpl.cancelResourceReserve(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals("Yes", output.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(input.getServiceHandlerHeader().getRequestId(), output.getConfigurationResponseCommon().getRequestId());
        Assert.assertEquals("Cancelling ResourceReserve failed !", output.getConfigurationResponseCommon().getResponseMessage());
    }

    @Test
    public void testCancelResourceReserve2(){
        notificationPublishService = new NotificationPublishServiceMock2();
        pathComputationServiceImpl = new PathComputationServiceImpl(this.getDataBroker(), notificationPublishService);
        CancelResourceReserveInput input = DataUtils.getCancelResourceReserveInput();
        CancelResourceReserveOutput output = pathComputationServiceImpl.cancelResourceReserve(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals("Yes", output.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(input.getServiceHandlerHeader().getRequestId(), output.getConfigurationResponseCommon().getRequestId());
        Assert.assertEquals("Cancelling ResourceReserve failed !", output.getConfigurationResponseCommon().getResponseMessage());
    }

    @Test
    public void testPathComputationRequest() throws ExecutionException, InterruptedException {
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
            , "topologyData/NW-for-test-5-4.xml");
        PathComputationRequestInput input = PceTestData.getEmptyPCERequest();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("Path not calculated", output.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void testPathComputationRequest2() throws ExecutionException, InterruptedException {
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
        , "topologyData/NW-for-test-5-4.xml");
        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void testPathComputationRequest21() throws ExecutionException, InterruptedException {
        notificationPublishService = new NotificationPublishServiceMock2();
        pathComputationServiceImpl = new PathComputationServiceImpl(this.getDataBroker(), notificationPublishService);
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
            , "topologyData/NW-for-test-5-4.xml");
        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void testPathComputationRequest22() throws ExecutionException, InterruptedException {
        DataStoreUtils.writeTopologyIntoDataStore(this.getDataBroker(), DataStoreUtils.getEmptyNetwork());
        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("500", output.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void testPathComputationRequest23() throws ExecutionException, InterruptedException {
        DataStoreUtils.writeTopologyIntoDataStore(this.getDataBroker(), DataStoreUtils.getNetwork1());
        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("500", output.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void testPathComputationRequest24() throws ExecutionException, InterruptedException {
        DataStoreUtils.writeTopologyIntoDataStore(this.getDataBroker(), DataStoreUtils.getNetwork2());
        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("500", output.getConfigurationResponseCommon().getResponseCode());
    }

    /**
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testPathComputationRequest25() throws ExecutionException, InterruptedException {
        DataStoreUtils.writeTopologyIntoDataStore(this.getDataBroker(), DataStoreUtils.getNetwork3());
        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("500", output.getConfigurationResponseCommon().getResponseCode());
    }

//    @Test
//    public void testPathComputationRequest26() throws ExecutionException, InterruptedException {
//        DataStoreUtils.writeTopologyIntoDataStore(this.getDataBroker(), DataStoreUtils.getNetwork4());
//        PathComputationRequestInput input = PceTestData.getPCE_test1_request_54();
//        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
//        Assert.assertEquals("500", output.getConfigurationResponseCommon().getResponseCode());
//    }

    @Test
    public void testPathComputationRequest3() throws ExecutionException, InterruptedException {
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
            , "topologyData/NW-for-test-5-4.xml");
        PathComputationRequestInput input = PceTestData.getPCE_test2_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
    }


    @Test
    public void testPathComputationRequest4() throws ExecutionException, InterruptedException {
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
            , "topologyData/NW-for-test-5-4.xml");
        PathComputationRequestInput input = PceTestData.getPCE_test3_request_54();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void testPathComputationRequest5() throws ExecutionException, InterruptedException {
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
            , "topologyData/NW-for-test-5-4.xml");
        PathComputationRequestInput input = PceTestData.getPCERequest();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
    }


    @Test
    public void testPathComputationRequest6() throws ExecutionException, InterruptedException {
        PceTestUtils.writeTopologyIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil()
            , "topologyData/NW-simple-topology.xml");
        PathComputationRequestInput input = PceTestData.getPCE_simpletopology_test1_request();
        PathComputationRequestOutput output = pathComputationServiceImpl.pathComputationRequest(input);
        Assert.assertEquals("200", output.getConfigurationResponseCommon().getResponseCode());
    }



}
