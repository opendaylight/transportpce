/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import static org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl.LogMessages;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

//writeOrModifyOrDeleteServiceList deprecated method should not raise warnings in tests
@SuppressWarnings("deprecation")
public class ServiceDataStoreOperationsImplTest extends AbstractTest {

    private ServiceDataStoreOperationsImpl serviceDataStoreOperations;

    @Before
    public void init() {
        DataBroker dataBroker = this.getNewDataBroker();
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(dataBroker);
    }

    @Test
    public void modifyIfServiceNotPresent() {
        OperationResult result =
                this.serviceDataStoreOperations.modifyService("service 1", State.InService, AdminStates.InService);
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(LogMessages.SERVICE_NOT_FOUND, result.getResultMessage());
    }

    @Test
    public void writeOrModifyOrDeleteServiceListNotPresentWithNoWriteChoice() {

        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("serviceCreateInput",
            createInput, pathComputationRequestOutput, 3);

        Assert.assertEquals(LogMessages.SERVICE_NOT_FOUND, result);
    }

    @Test
    public void writeOrModifyOrDeleteServiceListNotPresentWithWriteChoice() {

        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 2);

        Assert.assertNull(result);
    }

    @Test
    public void writeOrModifyOrDeleteServiceListPresentWithModifyChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        this.serviceDataStoreOperations.createService(createInput);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 0);
        Assert.assertNull(result);
    }

    @Test
    public void writeOrModifyOrDeleteServiceListPresentWithDeleteChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();

        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        this.serviceDataStoreOperations.createService(createInput);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 1);
        Assert.assertNull(result);
    }

    @Test
    public void writeOrModifyOrDeleteServiceListPresentWithNoValidChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        this.serviceDataStoreOperations.createService(createInput);
        String result = serviceDataStoreOperations.writeOrModifyOrDeleteServiceList("service 1",
            createInput, pathComputationRequestOutput, 2);
        Assert.assertNull(result);

    }

    @Test
    public void getServiceFromEmptyDataStoreShouldBeEmpty() {
        Optional<Services> optService = this.serviceDataStoreOperations.getService("service 1");
        Assert.assertFalse(optService.isPresent());
    }

    @Test
    public void createServiceShouldBeSuccessForValidInput() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        OperationResult result = this.serviceDataStoreOperations.createService(createInput);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void getServiceShouldReturnTheCorrectServiceForTheCreatedService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);

        Optional<Services> optService = this.serviceDataStoreOperations.getService(createInput.getServiceName());
        Assert.assertTrue(optService.isPresent());
        Assert.assertEquals(createInput.getServiceName(), optService.get().getServiceName());
    }

    @Test
    public void deleteServiceShouldBeSuccessfulForDeletingService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        OperationResult result = this.serviceDataStoreOperations.deleteService(createInput.getServiceName());
        Assert.assertTrue(result.isSuccess());
    }

//    @Test
//    public void deleteServiceShouldBeFailedIfServiceDoNotExists() {
//        OperationResult result = this.serviceDataStoreOperations.deleteService("Any service");
//        Assert.assertFalse(result.isSuccess());
//    }

    @Test
    public void modifyServiceIsSuccessfulForPresentService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        OperationResult result = this.serviceDataStoreOperations.modifyService(createInput.getServiceName(),
            State.InService, AdminStates.InService);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void getTempServiceFromEmptyDataStoreShouldBeEmpty() {
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> optService = this.serviceDataStoreOperations.getTempService("service 1");
        Assert.assertFalse(optService.isPresent());
    }

    @Test
    public void createTempServiceShouldBeSuccessForValidInput() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        OperationResult result = this.serviceDataStoreOperations.createTempService(createInput);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void getTempServiceShouldReturnTheCorrectTempServiceForTheCreatedService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        this.serviceDataStoreOperations.createTempService(createInput);

        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> optService = this.serviceDataStoreOperations.getTempService(createInput.getCommonId());
        Assert.assertTrue(optService.isPresent());
        Assert.assertEquals(createInput.getCommonId(), optService.get().getCommonId());
    }

    @Test
    public void deleteTempServiceShouldBeSuccessfulForDeletingTempService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        this.serviceDataStoreOperations.createTempService(createInput);
        OperationResult result = this.serviceDataStoreOperations.deleteTempService(createInput.getCommonId());
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void modifyTempServiceIsSuccessfulForPresentTempService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        this.serviceDataStoreOperations.createTempService(createInput);
        OperationResult result = this.serviceDataStoreOperations.modifyTempService(
            createInput.getCommonId(), State.InService, AdminStates.InService);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void createServicePathShouldBeSuccessfulForValidInput() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        ServiceInput serviceInput = new ServiceInput(createInput);
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
            .setPathDescription(new PathDescriptionBuilder()
                .setAToZDirection(new AToZDirectionBuilder()
                        .setAToZWavelengthNumber(Uint32.valueOf(1)).setRate(Uint32.valueOf(1)).build())
                .setZToADirection(new ZToADirectionBuilder()
                        .setZToAWavelengthNumber(Uint32.valueOf(1)).setRate(Uint32.valueOf(1)).build()).build())
            .build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).setResponseParameters(responseParameters)
            .build();
        OperationResult result =
            this.serviceDataStoreOperations.createServicePath(serviceInput, pathComputationRequestOutput);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void createServicePathShouldFailForInvalidInput() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        ServiceInput serviceInput = new ServiceInput(createInput);
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
            .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        ResponseParameters responseParameters = new ResponseParametersBuilder().build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).setResponseParameters(responseParameters)
            .build();
        OperationResult result =
            this.serviceDataStoreOperations.createServicePath(serviceInput, pathComputationRequestOutput);
        Assert.assertFalse(result.isSuccess());
    }

    @Test
    public void deleteServicePathShouldBeSuccessForDeletingServicePath() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        ServiceInput serviceInput = new ServiceInput(createInput);
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
            .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
            .setPathDescription(new PathDescriptionBuilder()
                .setAToZDirection(new AToZDirectionBuilder()
                        .setAToZWavelengthNumber(Uint32.valueOf(1)).setRate(Uint32.valueOf(1)).build())
                .setZToADirection(new ZToADirectionBuilder()
                        .setZToAWavelengthNumber(Uint32.valueOf(1)).setRate(Uint32.valueOf(1)).build()).build())
            .build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).setResponseParameters(responseParameters)
            .build();
        this.serviceDataStoreOperations.createServicePath(serviceInput, pathComputationRequestOutput);

        OperationResult result = this.serviceDataStoreOperations.deleteServicePath(serviceInput.getServiceName());
        Assert.assertTrue(result.isSuccess());
    }
}
