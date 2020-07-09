/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import static org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl.LogMessages;

import java.util.ArrayList;
import java.util.List;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.response.parameters.PathDescriptionBuilder;

//writeOrModifyOrDeleteServiceList deprecated method should not raise warnings in tests
@SuppressWarnings("deprecation")
public class ServiceDataStoreOperationsImplTest extends AbstractTest {

    private ServiceDataStoreOperationsImpl serviceDataStoreOperations;
    private PathDescription pathDescription;

    @Before
    public void init() {
        DataBroker dataBroker = this.getNewDataBroker();
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(dataBroker);
        // dummy path description to pass tests
        List<AToZ> atozList = createAToZList();
        List<ZToA> ztoaList = createZToAList();
        AToZDirection atozDirection = new AToZDirectionBuilder().setAToZ(atozList).build();
        ZToADirection ztoaDirection = new ZToADirectionBuilder().setZToA(ztoaList).build();
        this.pathDescription = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128
                .service.path.rpc.result.PathDescriptionBuilder().setAToZDirection(atozDirection)
                .setZToADirection(ztoaDirection).build();
    }

    private List<ZToA> createZToAList() {
        List<ZToA> ztoaList = new ArrayList<>();
        ZToA ztoA = new ZToABuilder().setId("0").setResource(new ResourceBuilder().setResource(new NodeBuilder()
                .setNodeId("XPONDER-3-2").build()).build()).build();
        ZToA ztoA1 = new ZToABuilder().setId("1").setResource(new ResourceBuilder().setResource(new NodeBuilder()
                .setNodeId("XPONDER-1-2").build()).build()).build();
        ztoaList.add(ztoA);
        ztoaList.add(ztoA1);
        return ztoaList;
    }

    private List<AToZ> createAToZList() {
        List<AToZ> atozList = new ArrayList<>();
        AToZ atoZ = new AToZBuilder().setId("0").setResource(new ResourceBuilder().setResource(new NodeBuilder()
                .setNodeId("XPONDER-1-2").build()).build()).build();
        AToZ atoZ1 = new AToZBuilder().setId("1").setResource(new ResourceBuilder().setResource(new NodeBuilder()
                .setNodeId("XPONDER-3-2").build()).build()).build();
        atozList.add(atoZ);
        atozList.add(atoZ1);

        return atozList;
    }

    @Test
    public void modifyIfServiceNotPresent() {
        OperationResult result =
                this.serviceDataStoreOperations.modifyService("service 1", State.InService, AdminStates.InService,
                        LifecycleState.Deployed);
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
        OperationResult createOutput = this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
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
        OperationResult createOutput = this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
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
        OperationResult createOutput = this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
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
        OperationResult result = this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void getServiceShouldReturnTheCorrectServiceForTheCreatedService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput, this.pathDescription);

        Optional<Services> optService = this.serviceDataStoreOperations.getService(createInput.getServiceName());
        Assert.assertTrue(optService.isPresent());
        Assert.assertEquals(createInput.getServiceName(), optService.get().getServiceName());
    }

    @Test
    public void deleteServiceShouldBeSuccessfulForDeletingService() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
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
        this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
        OperationResult result = this.serviceDataStoreOperations.modifyService(createInput.getServiceName(),
            State.InService, AdminStates.InService, LifecycleState.Deployed);
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
        OperationResult result = this.serviceDataStoreOperations.createTempService(createInput, this.pathDescription);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void getTempServiceShouldReturnTheCorrectTempServiceForTheCreatedService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        this.serviceDataStoreOperations.createTempService(createInput, this.pathDescription);

        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> optService = this.serviceDataStoreOperations.getTempService(createInput.getCommonId());
        Assert.assertTrue(optService.isPresent());
        Assert.assertEquals(createInput.getCommonId(), optService.get().getCommonId());
    }

    @Test
    public void deleteTempServiceShouldBeSuccessfulForDeletingTempService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        this.serviceDataStoreOperations.createTempService(createInput, this.pathDescription);
        OperationResult result = this.serviceDataStoreOperations.deleteTempService(createInput.getCommonId());
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void modifyTempServiceIsSuccessfulForPresentTempService() {
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        this.serviceDataStoreOperations.createTempService(createInput, this.pathDescription);
        OperationResult result = this.serviceDataStoreOperations.modifyTempService(
            createInput.getCommonId(), State.InService, AdminStates.InService);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void createServicePathShouldBeSuccessfulForValidInput() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
        ServiceInput serviceInput = new ServiceInput(createInput);
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
            .setPathDescription(new PathDescriptionBuilder()
                .setAToZDirection(new AToZDirectionBuilder().setAToZWavelengthNumber(1L).setRate(1L).build())
                .setZToADirection(new ZToADirectionBuilder().setZToAWavelengthNumber(1L).setRate(1L).build()).build())
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
        this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
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
        this.serviceDataStoreOperations.createService(createInput, this.pathDescription);
        ServiceInput serviceInput = new ServiceInput(createInput);
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
            .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
            .setPathDescription(new PathDescriptionBuilder()
                .setAToZDirection(new AToZDirectionBuilder().setAToZWavelengthNumber(1L).setRate(1L).build())
                .setZToADirection(new ZToADirectionBuilder().setZToAWavelengthNumber(1L).setRate(1L).build()).build())
            .build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).setResponseParameters(responseParameters)
            .build();
        this.serviceDataStoreOperations.createServicePath(serviceInput, pathComputationRequestOutput);

        OperationResult result = this.serviceDataStoreOperations.deleteServicePath(serviceInput.getServiceName());
        Assert.assertTrue(result.isSuccess());
    }
}
