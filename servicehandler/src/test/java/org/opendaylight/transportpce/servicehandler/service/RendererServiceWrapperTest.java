/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.TempServiceDeleteInputBuilder;

/**
 * Test RendererServiceWrapper class.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
@ExtendWith(MockitoExtension.class)
public class RendererServiceWrapperTest extends AbstractTest {
    @Mock
    private RendererServiceOperations rendererServiceOperationsMock;
    @Mock
    private NotificationPublishService notificationPublishService;
    @InjectMocks
    private RendererServiceWrapper rendererServiceWrapperMock;


    @Test
    void performRendererNullServiceHandlerHeader() {
        var serviceDeleteInput = ModelMappingUtils
            .createServiceDeleteInput(new ServiceInput(ServiceDataUtils.buildServiceDeleteInput()));
        serviceDeleteInput = new ServiceDeleteInputBuilder(serviceDeleteInput).setServiceHandlerHeader(null).build();
        ServiceDeleteOutput response = this.rendererServiceWrapperMock.performRenderer(serviceDeleteInput,
                ServiceNotificationTypes.ServiceDeleteResult, null);
        assertEquals(ResponseCodes.FINAL_ACK_YES,
                response.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED,
                response.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    void performRendererNullServiceName() {
        var serviceDeleteInput = ModelMappingUtils
            .createServiceDeleteInput(new ServiceInput(ServiceDataUtils.buildServiceDeleteInput()));
        serviceDeleteInput = new ServiceDeleteInputBuilder(serviceDeleteInput).setServiceName(null).build();
        ServiceDeleteOutput response = this.rendererServiceWrapperMock
            .performRenderer(serviceDeleteInput, ServiceNotificationTypes.ServiceDeleteResult, null);
        assertEquals(ResponseCodes.FINAL_ACK_YES, response.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED, response.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    void performRendererNullCommonId() {
        TempServiceDeleteInput input = new TempServiceDeleteInputBuilder(ServiceDataUtils.buildTempServiceDeleteInput())
            .setCommonId(null).build();
        ServiceDeleteOutput response = this.rendererServiceWrapperMock
            .performRenderer(input, ServiceNotificationTypes.ServiceDeleteResult, null);
        assertEquals(ResponseCodes.FINAL_ACK_YES, response.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED, response.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    void performRendererValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
            .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("Renderer service delete in progress")
            .build();
        ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<ServiceDeleteOutput> response = ServiceDataUtils.returnFuture(output);
        when(this.rendererServiceOperationsMock.serviceDelete(any(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput
                    .class), any()))
            .thenReturn(response);
        ServiceDeleteInput input = ServiceDataUtils.buildServiceDeleteInput();
        var serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        ServiceDeleteOutput rendereResponse = this.rendererServiceWrapperMock
            .performRenderer(serviceDeleteInput, ServiceNotificationTypes.ServiceDeleteResult, null);
        assertEquals(
            ResponseCodes.FINAL_ACK_NO,
            rendereResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_OK, rendereResponse.getConfigurationResponseCommon().getResponseCode());
        assertEquals(
            "Renderer service delete in progress",
            rendereResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.rendererServiceOperationsMock).serviceDelete(any(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput
                    .class), any());
    }

    @Test
    void performRendererTempValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
            .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("Renderer service delete in progress")
            .build();
        ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<ServiceDeleteOutput> response = ServiceDataUtils.returnFuture(output);
        when(this.rendererServiceOperationsMock.serviceDelete(any(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput
                    .class), any()))
            .thenReturn(response);
        TempServiceDeleteInput input = ServiceDataUtils.buildTempServiceDeleteInput();
        var tempServiceDeleteInput = ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        ServiceDeleteOutput rendereResponse = this.rendererServiceWrapperMock
            .performRenderer(tempServiceDeleteInput, ServiceNotificationTypes.ServiceDeleteResult, null);
        assertEquals(
            ResponseCodes.FINAL_ACK_NO,
            rendereResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_OK, rendereResponse.getConfigurationResponseCommon().getResponseCode());
        assertEquals(
            "Renderer service delete in progress",
            rendereResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.rendererServiceOperationsMock).serviceDelete(any(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput
                    .class), any());
    }
}