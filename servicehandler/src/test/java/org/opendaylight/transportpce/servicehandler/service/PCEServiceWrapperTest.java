/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInputBuilder;

public class PCEServiceWrapperTest extends AbstractTest {

    @Mock
    private PathComputationService pathComputationServiceMock;
    @Mock
    private NotificationPublishService notificationPublishServiceMock;
    @InjectMocks
    private PCEServiceWrapper pceServiceWrapperMock;

    private AutoCloseable closeable;

    @Before
    public void openMocks() throws NoSuchMethodException {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void performPCENullSdncRequestHeader() {
        ServiceCreateInput input =  ServiceDataUtils.buildServiceCreateInput();
        input = new ServiceCreateInputBuilder(input).setSdncRequestHeader(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_YES,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                pceResponse.getConfigurationResponseCommon().getResponseCode());
        Mockito.verifyNoInteractions(this.pathComputationServiceMock);
    }

    @Test
    public void performPCENullServiceName() {
        ServiceCreateInput input = ServiceDataUtils.buildServiceCreateInput();
        input = new ServiceCreateInputBuilder(input).setServiceName(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_YES,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                pceResponse.getConfigurationResponseCommon().getResponseCode());
        Mockito.verifyNoInteractions(this.pathComputationServiceMock);
    }

    @Test
    public void performPCENullCommonId() {
        TempServiceCreateInput input = ServiceDataUtils.buildTempServiceCreateInput();
        input = new TempServiceCreateInputBuilder(input).setCommonId(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_YES,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                pceResponse.getConfigurationResponseCommon().getResponseCode());
        Mockito.verifyNoInteractions(this.pathComputationServiceMock);
    }


    @Test
    public void cancelPCEResourceNullServiceName() {
        CancelResourceReserveOutput pceResponse =
                this.pceServiceWrapperMock.cancelPCEResource(null, ServiceNotificationTypes.ServiceDeleteResult);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_YES,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                pceResponse.getConfigurationResponseCommon().getResponseCode());
        Mockito.verifyNoInteractions(this.pathComputationServiceMock);
    }

    @Test
    public void cancelPCEResourceValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        CancelResourceReserveOutput output = new CancelResourceReserveOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<CancelResourceReserveOutput> response = ServiceDataUtils.returnFuture(output);
        Mockito.when(this.pathComputationServiceMock.cancelResourceReserve(any(CancelResourceReserveInput.class)))
                .thenReturn(response);
        CancelResourceReserveOutput pceResponse =
                this.pceServiceWrapperMock.cancelPCEResource("service 1", ServiceNotificationTypes.ServiceDeleteResult);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_NO,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_OK,
                pceResponse.getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals("PCE calculation in progress",
                pceResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.pathComputationServiceMock).cancelResourceReserve(any(CancelResourceReserveInput.class));
    }

    @Test
    public void performPCEValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput output = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<PathComputationRequestOutput> response = ServiceDataUtils.returnFuture(output);
        Mockito.when(this.pathComputationServiceMock.pathComputationRequest(any(PathComputationRequestInput.class)))
                .thenReturn(response);
        ServiceCreateInput input =  ServiceDataUtils.buildServiceCreateInput();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_NO,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_OK,
                pceResponse.getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals("PCE calculation in progress",
                pceResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.pathComputationServiceMock).pathComputationRequest((any(PathComputationRequestInput.class)));
    }

    @Test
    public void performPCETempValid() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        PathComputationRequestOutput output = new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        ListenableFuture<PathComputationRequestOutput> response = ServiceDataUtils.returnFuture(output);
        Mockito.when(this.pathComputationServiceMock.pathComputationRequest(any(PathComputationRequestInput.class)))
                .thenReturn(response);
        TempServiceCreateInput input = ServiceDataUtils.buildTempServiceCreateInput();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapperMock.performPCE(input, true);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_NO,
                pceResponse.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, pceResponse.getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals("PCE calculation in progress",
                pceResponse.getConfigurationResponseCommon().getResponseMessage());
        verify(this.pathComputationServiceMock).pathComputationRequest((any(PathComputationRequestInput.class)));
    }

    @After public void releaseMocks() throws Exception {
        closeable.close();
    }
}
