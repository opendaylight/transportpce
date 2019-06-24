/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.utils.InjectField;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;


public class PceListenerImplTest extends AbstractTest {

    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperationsMock;
    @Mock
    private RendererServiceOperations rendererServiceOperationsMock;
    @Mock
    private PCEServiceWrapper pceServiceWrapperMock;
    @Mock
    private ServiceInput serviceInputMock;
    @InjectMocks
    private PceListenerImpl pceListenerImplMock;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onServicePathRpcResultServiceInputIsNull() {
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", false);
        InjectField.inject(this.pceListenerImplMock, "input", null);
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.PathComputationRequest, "service 1", RpcStatusEx.Successful, "", true);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verifyNoMoreInteractions(this.serviceDataStoreOperationsMock);
        verifyNoMoreInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    public void onServicePathRpcResultPathDescriptionIsNull() {
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", false);
        InjectField.inject(this.pceListenerImplMock, "input", null);
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.PathComputationRequest, "service 1", RpcStatusEx.Successful, "", false);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verifyNoMoreInteractions(this.serviceDataStoreOperationsMock);
        verifyNoMoreInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    public void onServicePathRpcResultPCRSuccess() {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(this.serviceInputMock.getServiceCreateInput()).thenReturn(serviceCreateInput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceInputMock.getServiceAEnd()).thenReturn(serviceCreateInput.getServiceAEnd());
        Mockito.when(this.serviceInputMock.getServiceZEnd()).thenReturn(serviceCreateInput.getServiceZEnd());
        Mockito.when(this.serviceInputMock.getSdncRequestHeader())
                .thenReturn(serviceCreateInput.getSdncRequestHeader());
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", false);
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.PathComputationRequest, "service 1", RpcStatusEx.Successful, "", true);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verify(this.serviceDataStoreOperationsMock).createService(any(ServiceCreateInput.class));
        verify(this.serviceDataStoreOperationsMock).createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class));
        verify(this.rendererServiceOperationsMock).serviceImplementation(any(ServiceImplementationRequestInput.class));
    }

    @Test
    public void onServicePathRpcResultTempPCRSuccess() {
        TempServiceCreateInput tempServiceCreateInput = ServiceDataUtils.buildTempServiceCreateInput();
        Mockito.when(this.serviceInputMock.getTempServiceCreateInput()).thenReturn(tempServiceCreateInput);
        Mockito.when(this.serviceDataStoreOperationsMock.createTempService(any(TempServiceCreateInput.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceInputMock.getServiceAEnd()).thenReturn(tempServiceCreateInput.getServiceAEnd());
        Mockito.when(this.serviceInputMock.getServiceZEnd()).thenReturn(tempServiceCreateInput.getServiceZEnd());
        Mockito.when(this.serviceInputMock.getSdncRequestHeader())
                .thenReturn(tempServiceCreateInput.getSdncRequestHeader());
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", true);
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.PathComputationRequest, "service 1", RpcStatusEx.Successful, "", true);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verify(this.serviceDataStoreOperationsMock).createTempService(any(TempServiceCreateInput.class));
        verify(this.serviceDataStoreOperationsMock).createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class));
        verify(this.rendererServiceOperationsMock).serviceImplementation(any(ServiceImplementationRequestInput.class));
    }

    @Test
    public void onServicePathRpcResultPCRSuccessFeasabilityCheck() {
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "serviceFeasiblity", true);
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.PathComputationRequest, "service 1", RpcStatusEx.Successful, "", true);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verifyZeroInteractions(this.serviceDataStoreOperationsMock);
        verifyZeroInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    public void onServicePathRpcResultCRRSuccessWithNoReconfigure() {
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.CancelResourceReserve, "service 1", RpcStatusEx.Successful, "", false);
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(notification.getServiceName());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", false);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verify(this.serviceDataStoreOperationsMock).deleteService(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
    }

    @Test
    public void onServicePathRpcResultCRRSuccessWithReconfigure() {
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.CancelResourceReserve, "service 1", RpcStatusEx.Successful, "", false);
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(notification.getServiceName());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", true);
        InjectField.inject(this.pceListenerImplMock, "tempService", false);
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(this.serviceInputMock.getServiceCreateInput()).thenReturn(serviceCreateInput);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verify(this.serviceDataStoreOperationsMock).deleteService(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
        verify(this.pceServiceWrapperMock).performPCE(serviceCreateInput, true);
    }

    @Test
    public void onServicePathRpcResultCRRTempSuccess() {
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.CancelResourceReserve, "service 1", RpcStatusEx.Successful, "", false);
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(notification.getServiceName());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteTempService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", true);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verify(this.serviceDataStoreOperationsMock).deleteTempService(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
    }

    @Test
    public void onServicePathRpcResultCRRFailed() {
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.CancelResourceReserve, "service 1", RpcStatusEx.Failed, "", false);
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", false);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verifyZeroInteractions(this.serviceDataStoreOperationsMock);
        verifyZeroInteractions(this.pceServiceWrapperMock);
    }

    @Test
    public void onServicePathRpcResultCRRTempFailed() {
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult(
                ServicePathNotificationTypes.CancelResourceReserve, "service 1", RpcStatusEx.Failed, "", false);
        InjectField.inject(this.pceListenerImplMock, "serviceReconfigure", false);
        InjectField.inject(this.pceListenerImplMock, "tempService", true);
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        verifyZeroInteractions(this.serviceDataStoreOperationsMock);
        verifyZeroInteractions(this.pceServiceWrapperMock);
    }

    @Test
    public void onServicePathRpcResultRepeat() {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(this.serviceInputMock.getServiceCreateInput()).thenReturn(serviceCreateInput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceInputMock.getServiceAEnd()).thenReturn(serviceCreateInput.getServiceAEnd());
        Mockito.when(this.serviceInputMock.getServiceZEnd()).thenReturn(serviceCreateInput.getServiceZEnd());
        Mockito.when(this.serviceInputMock.getSdncRequestHeader())
                .thenReturn(serviceCreateInput.getSdncRequestHeader());
        this.pceListenerImplMock.onServicePathRpcResult(ServiceDataUtils.buildServicePathRpcResult());
        verify(this.rendererServiceOperationsMock).serviceImplementation(any(ServiceImplementationRequestInput.class));
        this.pceListenerImplMock.onServicePathRpcResult(ServiceDataUtils.buildServicePathRpcResult());
        verifyNoMoreInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    public void onServicePathRpcResultFailed() {
        this.pceListenerImplMock.onServicePathRpcResult(ServiceDataUtils.buildFailedPceServicePathRpcResult());
        verifyZeroInteractions(this.rendererServiceOperationsMock);
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase1() {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(this.serviceInputMock.getServiceCreateInput()).thenReturn(serviceCreateInput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceInputMock.getServiceAEnd()).thenReturn(serviceCreateInput.getServiceAEnd());
        Mockito.when(this.serviceInputMock.getServiceZEnd()).thenReturn(serviceCreateInput.getServiceZEnd());
        Mockito.when(this.serviceInputMock.getSdncRequestHeader())
                .thenReturn(serviceCreateInput.getSdncRequestHeader());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(serviceCreateInput.getServiceName());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServicePathRpcResult notification2 = new ServicePathRpcResultBuilder(notification)
                .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve).build();
        this.pceListenerImplMock.onServicePathRpcResult(notification2);
        verify(this.rendererServiceOperationsMock).serviceImplementation(any(ServiceImplementationRequestInput.class));
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase2() {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(this.serviceInputMock.getServiceCreateInput()).thenReturn(serviceCreateInput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceInputMock.getServiceAEnd()).thenReturn(serviceCreateInput.getServiceAEnd());
        Mockito.when(this.serviceInputMock.getServiceZEnd()).thenReturn(serviceCreateInput.getServiceZEnd());
        Mockito.when(this.serviceInputMock.getSdncRequestHeader())
                .thenReturn(serviceCreateInput.getSdncRequestHeader());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(serviceCreateInput.getServiceName());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServicePathRpcResult notification2 =
                new ServicePathRpcResultBuilder(notification).setServiceName("service 2").build();
        this.pceListenerImplMock.onServicePathRpcResult(notification2);
        verify(this.rendererServiceOperationsMock, times(2))
                .serviceImplementation(any(ServiceImplementationRequestInput.class));
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase3() {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(this.serviceInputMock.getServiceCreateInput()).thenReturn(serviceCreateInput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceInputMock.getServiceAEnd()).thenReturn(serviceCreateInput.getServiceAEnd());
        Mockito.when(this.serviceInputMock.getServiceZEnd()).thenReturn(serviceCreateInput.getServiceZEnd());
        Mockito.when(this.serviceInputMock.getSdncRequestHeader())
                .thenReturn(serviceCreateInput.getSdncRequestHeader());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServicePathRpcResult notification = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListenerImplMock.onServicePathRpcResult(notification);
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(serviceCreateInput.getServiceName());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServicePathRpcResult notification2 =
                new ServicePathRpcResultBuilder(notification).setStatus(RpcStatusEx.Failed).build();
        this.pceListenerImplMock.onServicePathRpcResult(notification2);
        verify(this.rendererServiceOperationsMock, times(1))
                .serviceImplementation(any(ServiceImplementationRequestInput.class));
    }
}
