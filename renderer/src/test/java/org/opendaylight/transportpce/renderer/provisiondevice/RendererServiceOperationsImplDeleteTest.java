/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.renderer.provisiondevice.notification.NotificationSender;
import org.opendaylight.transportpce.renderer.stub.OlmServiceStub;
import org.opendaylight.transportpce.renderer.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.renderer.utils.ServiceDeleteDataUtils;
import org.opendaylight.transportpce.renderer.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.stub.MountPointServiceStub;
import org.opendaylight.transportpce.test.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class RendererServiceOperationsImplDeleteTest extends AbstractTest {

    private DeviceTransactionManager deviceTransactionManager;
    private RendererServiceOperationsImpl rendererServiceOperations;
    private final DeviceRendererService deviceRenderer = mock(DeviceRendererService.class);
    private final OtnDeviceRendererService otnDeviceRendererService = mock(OtnDeviceRendererService.class);
    private final PortMapping portMapping = mock(PortMapping.class);
    private final CrossConnect crossConnect = mock(CrossConnect.class);
    private TransportpceOlmService olmService;

    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
    }

    @BeforeEach
    void setUp() {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        this.olmService = spy(this.olmService);
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(deviceRenderer,
            otnDeviceRendererService, olmService, getDataBroker(), new NotificationSender(notificationPublishService),
            portMapping);
    }


    @Test
    void serviceDeleteOperationPp() throws ExecutionException, InterruptedException, TimeoutException {
        writePathDescription();
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        serviceDeleteInputBuilder.setServiceHandlerHeader((new ServiceHandlerHeaderBuilder())
            .setRequestId("request1").build());
        doReturn(Collections.emptyList())
            .when(this.crossConnect).deleteCrossConnect(anyString(), anyString(), eq(false));
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf("100"))
            .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO),
                    new TxDirectionBuilder().setIndex(Uint8.ZERO).setPort(new PortBuilder().setPortName("port-name")
                        .build()).build()))
            .setNodeId(new NodeIdType("optical-node1"))
            .build();
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setConnectionType(ConnectionType.Service)
            .setServiceAEnd(serviceAEnd)
            .build();
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(null);
        when(deviceRenderer.deleteServicePath(any()))
            .thenReturn(new ServicePathOutputBuilder().setSuccess(true).build());
        ServiceDeleteOutput serviceDeleteOutput = this.rendererServiceOperations
            .serviceDelete(serviceDeleteInputBuilder.build(), service).get();
        assertEquals(ResponseCodes.RESPONSE_OK, serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceDeleteOperationNoDescription() throws InterruptedException, ExecutionException {
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setServiceAEnd(new ServiceAEndBuilder()
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(Map.of(
                        new TxDirectionKey(Uint8.ZERO),
                        new TxDirectionBuilder().setIndex(Uint8.ZERO).setPort(new PortBuilder()
                            .setPortName("port-name").build()).build()))
                .setNodeId(new NodeIdType("optical-node1"))
                .build())
            .build();
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(null);
        doReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
            .setResult("Failed").build()).buildFuture()).when(this.olmService).servicePowerTurndown(any());
        ServiceDeleteOutput serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build(), service).get();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        verify(this.crossConnect, times(0)).deleteCrossConnect(any(), any(), eq(false));
    }

    @Test
    void serviceDeleteOperationTearDownFailedAtoZ() throws ExecutionException, InterruptedException {
        doReturn(Collections.emptyList())
            .when(this.crossConnect).deleteCrossConnect(anyString(),anyString(), eq(false));
        doReturn(RpcResultBuilder.success(new ServicePowerTurndownOutputBuilder().setResult("Failed").build())
                .buildFuture())
            .when(this.olmService).servicePowerTurndown(any());

        writePathDescription();
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setConnectionType(ConnectionType.Service)
            .setServiceAEnd(new ServiceAEndBuilder()
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf("100"))
                .setTxDirection(Map.of(
                    new TxDirectionKey(Uint8.ZERO),
                    new TxDirectionBuilder()
                        .setIndex(Uint8.ZERO)
                        .setPort(new PortBuilder().setPortName("port-name").build())
                        .build()))
                .setNodeId(new NodeIdType("optical-node1"))
                .build())
            .build();
        when(portMapping.getMapping(anyString(), anyString()))
            .thenReturn(null);
        ListenableFuture<ServiceDeleteOutput> serviceDeleteOutput = this.rendererServiceOperations
            .serviceDelete(
                new ServiceDeleteInputBuilder()
                    .setServiceName("service 1")
                    .setServiceHandlerHeader((new ServiceHandlerHeaderBuilder()).setRequestId("request1").build())
                    .build(),
                service);
        ServiceDeleteOutput output = serviceDeleteOutput.get();
        assertEquals(ResponseCodes.RESPONSE_FAILED,
                output.getConfigurationResponseCommon().getResponseCode());
        verify(this.crossConnect, times(0)).deleteCrossConnect(eq("node1"), any(), eq(false));
        verify(this.crossConnect, times(0)).deleteCrossConnect(eq("node2"), any(), eq(false));
    }

    @Test
    void serviceDeleteOperationTearDownFailedZtoA() throws ExecutionException, InterruptedException {
        doReturn(Collections.emptyList())
            .when(this.crossConnect).deleteCrossConnect(anyString(), anyString(), eq(false));
        when(this.olmService.servicePowerTurndown(any()))
            .thenReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder()).setResult("Success").build())
                .buildFuture())
            .thenReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder()).setResult("Failed").build())
                .buildFuture());

        writePathDescription();
        when(portMapping.getMapping(anyString(), anyString()))
            .thenReturn(null);
        ServiceDeleteOutput serviceDeleteOutput = this.rendererServiceOperations.serviceDelete(
                new ServiceDeleteInputBuilder()
                    .setServiceName("service 1")
                    .setServiceHandlerHeader((new ServiceHandlerHeaderBuilder()).setRequestId("request1").build())
                    .build(),
                new ServicesBuilder()
                    .setServiceName("service 1")
                    .setConnectionType(ConnectionType.Service)
                    .setServiceAEnd(new ServiceAEndBuilder()
                        .setServiceFormat(ServiceFormat.Ethernet)
                        .setServiceRate(Uint32.valueOf("100"))
                        .setTxDirection(Map.of(
                            new TxDirectionKey(Uint8.ZERO),
                            new TxDirectionBuilder()
                                .setIndex(Uint8.ZERO)
                                .setPort(new PortBuilder().setPortName("port-name").build())
                                .build()))
                        .setNodeId(new NodeIdType("optical-node1"))
                        .build())
                    .build())
            .get();
        assertEquals(ResponseCodes.RESPONSE_FAILED,
            serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        verify(this.olmService, times(2)).servicePowerTurndown(any());
        verify(this.crossConnect, times(0)).deleteCrossConnect(eq("node1"), any(),eq(false));
        verify(this.crossConnect, times(0)).deleteCrossConnect(eq("node2"), any(),eq(false));
    }

    private void writePathDescription() throws ExecutionException, InterruptedException {
        TransactionUtils.writeTransaction(
                this.deviceTransactionManager,
                "node1" + StringConstants.PP_TOKEN,
                LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(ServicePathList.class)
                    .child(ServicePaths.class, new ServicePathsKey("service 1")),
                new ServicePathsBuilder()
                    .setPathDescription(ServiceDeleteDataUtils
                        .createTransactionPathDescription(StringConstants.PP_TOKEN))
                    .setServiceAEnd(ServiceDeleteDataUtils.getServiceAEndBuild().build())
                    .setServiceZEnd(ServiceDeleteDataUtils.getServiceZEndBuild().build())
                    .withKey(new ServicePathsKey("service 1"))
                    .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId("Request 1").build())
                    .build());
    }
}