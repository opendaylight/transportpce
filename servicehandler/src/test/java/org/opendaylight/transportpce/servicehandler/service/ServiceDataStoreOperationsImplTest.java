/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl;
import org.opendaylight.transportpce.servicehandler.stub.StubRendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;


public class ServiceDataStoreOperationsImplTest extends AbstractTest {

    private ServiceDataStoreOperationsImpl serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private ServicehandlerImpl serviceHandler;
    private RendererServiceOperations rendererServiceOperations;

    public ServiceDataStoreOperationsImplTest() {
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        PathComputationService pathComputationService = new PathComputationServiceImpl(getDataBroker(),
            notificationPublishService);
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService);
        this.rendererServiceOperations = new StubRendererServiceOperations();
        this.serviceHandler = new ServicehandlerImpl(getDataBroker(), pathComputationService,
            this.rendererServiceOperations);
    }


    @Before
    public void init() {
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(this.getDataBroker());
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void modifyIfServiceNotPresent() {
        OperationResult result = this.serviceDataStoreOperations.modifyService("service 1",
            State.InService, State.InService);
        Assert.assertEquals("Service " + "service 1" + " is not present!", result.getResultMessage());
    }

    @Test
    public void writeOrModifyOrDeleteServiceListNotPresentWithNoWriteChoice() {

        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pathComputationRequestOutput = this.pceServiceWrapper.performPCE(createInput,
            true);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("serviceCreateInput",
            createInput, pathComputationRequestOutput, 3);

        Assert.assertEquals("Service is not present ! ", result);

    }

    @Test
    public void writeOrModifyOrDeleteServiceListNotPresentWithWriteChoice() {

        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pathComputationRequestOutput = this.pceServiceWrapper.performPCE(createInput,
            true);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 2);

        Assert.assertEquals(null, result);

    }

    @Test
    public void writeOrModifyOrDeleteServiceListPresentWithModifyChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pathComputationRequestOutput = this.pceServiceWrapper.performPCE(createInput,
            true);
        OperationResult createOutput = this.serviceDataStoreOperations.createService(createInput,
            pathComputationRequestOutput);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 0);
        Assert.assertEquals(null, result);

    }

    @Test
    public void writeOrModifyOrDeleteServiceListPresentWithDeleteChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pathComputationRequestOutput = this.pceServiceWrapper.performPCE(createInput,
            true);
        OperationResult createOutput = this.serviceDataStoreOperations.createService(createInput,
            pathComputationRequestOutput);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 1);
        Assert.assertEquals(null, result);

    }

    @Test
    public void writeOrModifyOrDeleteServiceListPresentWithNoValidChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pathComputationRequestOutput = this.pceServiceWrapper.performPCE(createInput,
            true);
        OperationResult createOutput = this.serviceDataStoreOperations.createService(createInput,
            pathComputationRequestOutput);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 2);
        Assert.assertEquals(null, result);

    }
}