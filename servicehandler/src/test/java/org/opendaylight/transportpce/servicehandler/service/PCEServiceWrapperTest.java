/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
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
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInputBuilder;

@ExtendWith(MockitoExtension.class)
public class PCEServiceWrapperTest extends AbstractTest {

    @Mock
    private PathComputationService pathComputationServiceMock;
    @Mock
    private NotificationPublishService notificationPublishServiceMock;
    @InjectMocks
    private PCEServiceWrapper pceServiceWrapperMock;


    @Test
    void performPCENullSdncRequestHeader() {
        ServiceCreateInput input =  ServiceDataUtils.buildServiceCreateInput();
        input = new ServiceCreateInputBuilder(input).setSdncRequestHeader(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        assertEquals(ResponseCodes.FINAL_ACK_YES, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED, pceResponse.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.pathComputationServiceMock);
    }

    @Test
    void performPCENullServiceName() {
        ServiceCreateInput input = ServiceDataUtils.buildServiceCreateInput();
        input = new ServiceCreateInputBuilder(input).setServiceName(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        assertEquals(ResponseCodes.FINAL_ACK_YES, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED, pceResponse.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.pathComputationServiceMock);
    }

    @Test
    void performPCENullCommonId() {
        TempServiceCreateInput input = ServiceDataUtils.buildTempServiceCreateInput();
        input = new TempServiceCreateInputBuilder(input).setCommonId(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        assertEquals(ResponseCodes.FINAL_ACK_YES, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED, pceResponse.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.pathComputationServiceMock);
    }


    @Test
    void cancelPCEResourceNullServiceName() {
        CancelResourceReserveOutput pceResponse =
                this.pceServiceWrapperMock.cancelPCEResource(null, ServiceNotificationTypes.ServiceDeleteResult);
        assertEquals(ResponseCodes.FINAL_ACK_YES, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_FAILED, pceResponse.getConfigurationResponseCommon().getResponseCode());
        verifyNoInteractions(this.pathComputationServiceMock);
    }

    @Test
    void cancelPCEResourceValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        CancelResourceReserveOutput output = new CancelResourceReserveOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<CancelResourceReserveOutput> response = ServiceDataUtils.returnFuture(output);
        when(this.pathComputationServiceMock.cancelResourceReserve(any(CancelResourceReserveInput.class)))
            .thenReturn(response);
        CancelResourceReserveOutput pceResponse = this.pceServiceWrapperMock
            .cancelPCEResource("service 1", ServiceNotificationTypes.ServiceDeleteResult);
        assertEquals(ResponseCodes.FINAL_ACK_NO, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_OK, pceResponse.getConfigurationResponseCommon().getResponseCode());
        assertEquals("PCE calculation in progress", pceResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.pathComputationServiceMock).cancelResourceReserve(any(CancelResourceReserveInput.class));
    }

    @Test
    void performPCEValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput output = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<PathComputationRequestOutput> response = ServiceDataUtils.returnFuture(output);
        when(this.pathComputationServiceMock.pathComputationRequest(any(PathComputationRequestInput.class)))
            .thenReturn(response);
        ServiceCreateInput input =  ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        assertEquals(ResponseCodes.FINAL_ACK_NO, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_OK, pceResponse.getConfigurationResponseCommon().getResponseCode());
        assertEquals("PCE calculation in progress", pceResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.pathComputationServiceMock).pathComputationRequest((any(PathComputationRequestInput.class)));
    }

    @Test
    void performPCETempValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput output = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<PathComputationRequestOutput> response = ServiceDataUtils.returnFuture(output);
        when(this.pathComputationServiceMock.pathComputationRequest(any(PathComputationRequestInput.class)))
            .thenReturn(response);
        TempServiceCreateInput input = ServiceDataUtils.buildTempServiceCreateInput();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        assertEquals(ResponseCodes.FINAL_ACK_NO, pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(ResponseCodes.RESPONSE_OK, pceResponse.getConfigurationResponseCommon().getResponseCode());
        assertEquals("PCE calculation in progress", pceResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.pathComputationServiceMock).pathComputationRequest((any(PathComputationRequestInput.class)));
    }
}