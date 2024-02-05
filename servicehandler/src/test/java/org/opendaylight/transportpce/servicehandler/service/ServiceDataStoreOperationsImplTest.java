/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl.LogMessages;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

//writeOrModifyOrDeleteServiceList deprecated method should not raise warnings in tests
@SuppressWarnings("deprecation")
public class ServiceDataStoreOperationsImplTest extends AbstractTest {

    private ServiceDataStoreOperationsImpl serviceDataStoreOperations;

    @BeforeEach
    void init() {
        DataBroker dataBroker = this.getNewDataBroker();
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(dataBroker);
    }

    @Test
    void modifyIfServiceNotPresent() {
        OperationResult result = this.serviceDataStoreOperations
            .modifyService("service 1", State.InService, AdminStates.InService);
        assertFalse(result.isSuccess());
        assertEquals(LogMessages.SERVICE_NOT_FOUND, result.getResultMessage());
    }

    @Test
    void writeOrModifyOrDeleteServiceListNotPresentWithNoWriteChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        String result = serviceDataStoreOperations
            .writeOrModifyOrDeleteServiceList("serviceCreateInput", createInput, pathComputationRequestOutput, 3);
        assertEquals(LogMessages.SERVICE_NOT_FOUND, result);
    }

    @Test
    void writeOrModifyOrDeleteServiceListNotPresentWithWriteChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        String result = serviceDataStoreOperations
            .writeOrModifyOrDeleteServiceList("service 1", createInput, pathComputationRequestOutput, 2);
        assertNull(result);
    }

    @Test
    void writeOrModifyOrDeleteServiceListPresentWithModifyChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        this.serviceDataStoreOperations.createService(createInput);
        String result = serviceDataStoreOperations
            .writeOrModifyOrDeleteServiceList("service 1", createInput, pathComputationRequestOutput, 0);
        assertNull(result);
    }

    @Test
    void writeOrModifyOrDeleteServiceListPresentWithDeleteChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        this.serviceDataStoreOperations.createService(createInput);
        String result = serviceDataStoreOperations
            .writeOrModifyOrDeleteServiceList("service 1", createInput, pathComputationRequestOutput, 1);
        assertNull(result);
    }

    @Test
    void writeOrModifyOrDeleteServiceListPresentWithNoValidChoice() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        this.serviceDataStoreOperations.createService(createInput);
        String result = serviceDataStoreOperations
            .writeOrModifyOrDeleteServiceList("service 1",createInput, pathComputationRequestOutput, 2);
        assertNull(result);
    }

    @Test
    void getServiceFromEmptyDataStoreShouldBeEmpty() {
        Optional<Services> optService = this.serviceDataStoreOperations.getService("service 1");
        assertFalse(optService.isPresent());
    }

    @Test
    void createServiceShouldBeSuccessForValidInput() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        OperationResult result = this.serviceDataStoreOperations.createService(createInput);
        assertTrue(result.isSuccess());
    }

    @Test
    void getServiceShouldReturnTheCorrectServiceForTheCreatedService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);

        Optional<Services> optService = this.serviceDataStoreOperations.getService(createInput.getServiceName());
        assertTrue(optService.isPresent());
        assertEquals(createInput.getServiceName(), optService.orElseThrow().getServiceName());
    }

    @Test
    void deleteServiceShouldBeSuccessfulForDeletingService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        OperationResult result = this.serviceDataStoreOperations.deleteService(createInput.getServiceName());
        assertTrue(result.isSuccess());
    }

    @Test
    void deleteServiceShouldBeSuccessEvenIfServiceDoNotExists() {
        OperationResult result = this.serviceDataStoreOperations.deleteService("Any service");
        assertTrue(result.isSuccess());
    }

    @Test
    void modifyServiceIsSuccessfulForPresentService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput);
        OperationResult result = this.serviceDataStoreOperations.modifyService(createInput.getServiceName(),
            State.InService, AdminStates.InService);
        assertTrue(result.isSuccess());
    }

    @Test
    void getTempServiceFromEmptyDataStoreShouldBeEmpty() {
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list
                .Services> optService = this.serviceDataStoreOperations.getTempService("service 1");
        assertFalse(optService.isPresent());
    }

    @Test
    void createTempServiceShouldBeSuccessForValidInput() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        PathDescription pathDescription = ServiceDataUtils.createPathDescription(0,1, 0, 1);
        OperationResult result = this.serviceDataStoreOperations.createTempService(createInput, pathDescription);
        assertTrue(result.isSuccess());
    }

    @Test
    void getTempServiceShouldReturnTheCorrectTempServiceForTheCreatedService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        PathDescription pathDescription = ServiceDataUtils.createPathDescription(0,1, 0, 1);
        this.serviceDataStoreOperations.createTempService(createInput, pathDescription);

        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list
                .Services> optService = this.serviceDataStoreOperations.getTempService(createInput.getCommonId());
        assertTrue(optService.isPresent());
        assertEquals(createInput.getCommonId(), optService.orElseThrow().getCommonId());
    }

    @Test
    void deleteTempServiceShouldBeSuccessfulForDeletingTempService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        PathDescription pathDescription = ServiceDataUtils.createPathDescription(0,1, 0, 1);
        this.serviceDataStoreOperations.createTempService(createInput, pathDescription);
        OperationResult result = this.serviceDataStoreOperations.deleteTempService(createInput.getCommonId());
        assertTrue(result.isSuccess());
    }

    @Test
    void modifyTempServiceIsSuccessfulForPresentTempService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        PathDescription pathDescription = ServiceDataUtils.createPathDescription(0,1, 0, 1);
        this.serviceDataStoreOperations.createTempService(createInput, pathDescription);
        OperationResult result = this.serviceDataStoreOperations.modifyTempService(
            createInput.getCommonId(), State.InService, AdminStates.InService);
        assertTrue(result.isSuccess());
    }

    @Test
    void createServicePathShouldBeSuccessfulForValidInput() {
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
        OperationResult result = this.serviceDataStoreOperations
            .createServicePath(serviceInput, pathComputationRequestOutput);
        assertTrue(result.isSuccess());
    }

    @Test
    void createServicePathShouldFailForInvalidInput() {
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
        OperationResult result = this.serviceDataStoreOperations
            .createServicePath(serviceInput, pathComputationRequestOutput);
        assertFalse(result.isSuccess());
    }

    @Test
    void deleteServicePathShouldBeSuccessForDeletingServicePath() {
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
        assertTrue(result.isSuccess());
    }
}