/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.utils.InjectField;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;

public class RendererListenerImplTest extends AbstractTest {

    @InjectMocks
    private RendererListenerImpl rendererListenerImplMock;
    @Mock
    private PathComputationService pathComputationService;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private ServiceInput serviceInputMock;
    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperationsMock;
    @Mock
    private PCEServiceWrapper pceServiceWrapperMock;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onServiceRpcResultServiceInputIsNull() {
        InjectField.inject(this.rendererListenerImplMock, "tempService", false);
        InjectField.inject(this.rendererListenerImplMock, "input", null);
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceDelete, "service 1", RpcStatusEx.Successful, "");
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(notification.getServiceName()))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(notification.getServiceName()))
                .thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verifyNoMoreInteractions(this.pceServiceWrapperMock);
    }

    @Test
    public void onServiceRpcResultSIRSuccess() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceImplementationRequest, "service 1", RpcStatusEx.Successful, "");
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(notification.getServiceName(), State.InService,
                State.InService))
                .thenReturn(OperationResult.ok("Successful"));
        InjectField.inject(this.rendererListenerImplMock, "tempService", false);
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).modifyService(any(String.class), any(State.class),
                any(State.class));
    }

    @Test
    public void onServiceRpcResultSIRTempSuccess() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceImplementationRequest, "service 1", RpcStatusEx.Successful, "");
        Mockito.when(this.serviceDataStoreOperationsMock.modifyTempService(notification.getServiceName(),
                State.InService, State.InService)).thenReturn(OperationResult.ok("Successful"));
        InjectField.inject(this.rendererListenerImplMock, "tempService", true);
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).modifyTempService(any(String.class), any(State.class),
                any(State.class));
    }

    @Test
    public void onServiceRpcResultSIRFailed() {
        InjectField.inject(this.rendererListenerImplMock, "tempService", false);
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceImplementationRequest, "service 1", RpcStatusEx.Failed, "");
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteService(any(String.class));
    }

    @Test
    public void onServiceRpcResultSIRTempFailed() {
        InjectField.inject(this.rendererListenerImplMock, "tempService", true);
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteTempService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceImplementationRequest, "service 1", RpcStatusEx.Failed, "");
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteTempService(any(String.class));
    }

    @Test
    public void onServiceRpcResultSDSuccess() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceDelete, "service 1", RpcStatusEx.Successful, "");
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(notification.getServiceName());
        Mockito.when(this.pceServiceWrapperMock.cancelPCEResource(notification.getServiceName(),
                ServiceNotificationTypes.ServiceDeleteResult)).thenReturn(null);
        InjectField.inject(this.rendererListenerImplMock, "tempService", false);
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.pceServiceWrapperMock).cancelPCEResource(any(String.class), any(ServiceNotificationTypes.class));
    }

    @Test
    public void onServiceRpcResultSDTempSuccess() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceDelete, "service 1", RpcStatusEx.Successful, "");
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(notification.getServiceName());
        Mockito.when(this.pceServiceWrapperMock.cancelPCEResource(notification.getServiceName(),
                ServiceNotificationTypes.ServiceDeleteResult)).thenReturn(null);
        InjectField.inject(this.rendererListenerImplMock, "tempService", true);
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.pceServiceWrapperMock).cancelPCEResource(any(String.class), any(ServiceNotificationTypes.class));
    }

    @Test
    public void onServiceRpcResultSDFailed() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceDelete, "service 1", RpcStatusEx.Failed, "");
        InjectField.inject(this.rendererListenerImplMock, "tempService", true);
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verifyZeroInteractions(this.pceServiceWrapperMock);
    }

    @Test
    public void onServiceRpcResultSDTempFailed() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp(
                ServicePathNotificationTypes.ServiceDelete, "service 1", RpcStatusEx.Failed, "");
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verifyZeroInteractions(this.pceServiceWrapperMock);
    }

    @Test
    public void onServiceRpcResultSpRepeat() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp();
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(notification.getServiceName(), State.InService,
                State.InService))
                .thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).modifyService(any(String.class), any(State.class),
                any(State.class));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verifyZeroInteractions(this.serviceDataStoreOperationsMock);
    }

    @Test
    public void onServiceRpcResultSpFailed() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildFailedServiceRpcResultSp();
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteService(any(String.class));
    }

    @Test
    public void onServiceRpcResultSpRepeatFailedCompareCase1() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp();
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(notification.getServiceName(), State.InService,
                State.InService)).thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).modifyService(any(String.class), any(State.class),
                any(State.class));
        ServiceRpcResultSp notification2 = new ServiceRpcResultSpBuilder(notification)
                .setNotificationType(ServicePathNotificationTypes.ServiceDelete).build();
        Mockito.when(this.serviceInputMock.getServiceName()).thenReturn(notification.getServiceName());
        Mockito.when(this.pceServiceWrapperMock.cancelPCEResource(notification.getServiceName(),
                ServiceNotificationTypes.ServiceDeleteResult)).thenReturn(null);
        this.rendererListenerImplMock.onServiceRpcResultSp(notification2);
        verify(this.pceServiceWrapperMock).cancelPCEResource(any(String.class), any(ServiceNotificationTypes.class));
    }

    @Test
    public void onServiceRpcResultSpRepeatFailedCompareCase2() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp();
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(notification.getServiceName(), State.InService,
                State.InService)).thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        ServiceRpcResultSp notification2 =
                new ServiceRpcResultSpBuilder(notification).setServiceName("service 2").build();
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(notification2.getServiceName(), State.InService,
                State.InService)).thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification2);
        verify(this.serviceDataStoreOperationsMock, times(2)).modifyService(any(String.class), any(State.class),
                  any(State.class));
    }

    @Test
    public void onServiceRpcResultSpRepeatFailedCompareCase3() {
        ServiceRpcResultSp notification = ServiceDataUtils.buildServiceRpcResultSp();
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(notification.getServiceName(), State.InService,
                State.InService)).thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification);
        verify(this.serviceDataStoreOperationsMock).modifyService(any(String.class), any(State.class),
                any(State.class));
        ServiceRpcResultSp notification2 =
                new ServiceRpcResultSpBuilder(notification).setStatus(RpcStatusEx.Failed).build();
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(any(String.class)))
                .thenReturn(OperationResult.ok("Successful"));
        this.rendererListenerImplMock.onServiceRpcResultSp(notification2);
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(any(String.class));
        verify(this.serviceDataStoreOperationsMock).deleteService(any(String.class));
    }

}
