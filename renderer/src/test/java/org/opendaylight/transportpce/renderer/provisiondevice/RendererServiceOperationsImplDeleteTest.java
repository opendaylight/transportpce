/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class RendererServiceOperationsImplDeleteTest extends AbstractTest {

    private DeviceTransactionManager deviceTransactionManager;
    private RendererServiceOperationsImpl rendererServiceOperations;
    private final DeviceRendererService deviceRenderer = Mockito.mock(DeviceRendererService.class);
    private final OtnDeviceRendererService otnDeviceRendererService = Mockito.mock(OtnDeviceRendererService.class);
    private final PortMapping portMapping = Mockito.mock(PortMapping.class);
    private final CrossConnect crossConnect = Mockito.mock(CrossConnect.class);
    private TransportpceOlmService olmService;

    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
    }

    @Before
    public void setUp() {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        this.olmService = Mockito.spy(this.olmService);
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(deviceRenderer,
            otnDeviceRendererService, olmService, getDataBroker(), notificationPublishService, portMapping);
    }


    @Test
    public void serviceDeleteOperationPp() throws ExecutionException, InterruptedException, TimeoutException {
        writePathDescription();
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        serviceDeleteInputBuilder.setServiceHandlerHeader((new ServiceHandlerHeaderBuilder())
            .setRequestId("request1").build());
        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect).deleteCrossConnect(Mockito.anyString(),
            Mockito.anyString(), Mockito.eq(false));
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf("100"))
            .setTxDirection(new TxDirectionBuilder().setPort(new PortBuilder().setPortName("port-name").build())
                .build())
            .setNodeId(new NodeIdType("optical-node1"))
            .build();
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setConnectionType(ConnectionType.Service)
            .setServiceAEnd(serviceAEnd)
            .build();
        Mockito.when(portMapping.getMapping(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(null);
        Mockito.when(deviceRenderer.deleteServicePath(Mockito.any()))
            .thenReturn(new ServicePathOutputBuilder().setSuccess(true).build());
        ServiceDeleteOutput serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build(), service).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK,
            serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceDeleteOperationNoDescription() throws InterruptedException, ExecutionException {
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setServiceAEnd(new ServiceAEndBuilder()
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder().setPort(new PortBuilder().setPortName("port-name").build())
                    .build())
                .setNodeId(new NodeIdType("optical-node1"))
                .build())
            .build();
        Mockito.when(portMapping.getMapping(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(null);
        Mockito.doReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
            .setResult("Failed").build()).buildFuture()).when(this.olmService).servicePowerTurndown(Mockito.any());
        ServiceDeleteOutput serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build(), service).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
            serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.crossConnect, Mockito.times(0))
                .deleteCrossConnect(Mockito.any(), Mockito.any(), Mockito.eq(false));
    }

    @Test
    public void serviceDeleteOperationTearDownFailedAtoZ() throws ExecutionException, InterruptedException {
        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect).deleteCrossConnect(Mockito.anyString(),
            Mockito.anyString(), Mockito.eq(false));
        Mockito.doReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
            .setResult("Failed").build()).buildFuture()).when(this.olmService).servicePowerTurndown(Mockito.any());

        writePathDescription();
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        serviceDeleteInputBuilder.setServiceHandlerHeader((new ServiceHandlerHeaderBuilder())
                .setRequestId("request1").build());
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf("100"))
            .setTxDirection(new TxDirectionBuilder().setPort(new PortBuilder().setPortName("port-name").build())
                .build())
            .setNodeId(new NodeIdType("optical-node1"))
            .build();
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setConnectionType(ConnectionType.Service)
            .setServiceAEnd(serviceAEnd)
            .build();
        Mockito.when(portMapping.getMapping(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(null);
        ListenableFuture<ServiceDeleteOutput> serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build(), service);
        ServiceDeleteOutput output = serviceDeleteOutput.get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                output.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node1"), Mockito.any(),
            Mockito.eq(false));
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node2"), Mockito.any(),
            Mockito.eq(false));
    }

    @Test
    public void serviceDeleteOperationTearDownFailedZtoA() throws ExecutionException, InterruptedException {
        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect).deleteCrossConnect(Mockito.anyString(),
            Mockito.anyString(), Mockito.eq(false));
        Mockito.when(this.olmService.servicePowerTurndown(Mockito.any()))
            .thenReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
                .setResult("Success").build()).buildFuture())
            .thenReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
                .setResult("Failed").build()).buildFuture());

        writePathDescription();
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        serviceDeleteInputBuilder.setServiceHandlerHeader((new ServiceHandlerHeaderBuilder())
            .setRequestId("request1").build());
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf("100"))
            .setTxDirection(new TxDirectionBuilder().setPort(new PortBuilder().setPortName("port-name").build())
                .build())
            .setNodeId(new NodeIdType("optical-node1"))
            .build();
        Services service = new ServicesBuilder()
            .setServiceName("service 1")
            .setConnectionType(ConnectionType.Service)
            .setServiceAEnd(serviceAEnd)
            .build();
        Mockito.when(portMapping.getMapping(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(null);
        ServiceDeleteOutput serviceDeleteOutput =
                this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build(), service).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
            serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.olmService, Mockito.times(2)).servicePowerTurndown(Mockito.any());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node1"), Mockito.any(),
            Mockito.eq(false));
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node2"), Mockito.any(),
            Mockito.eq(false));
    }

    private void writePathDescription() throws ExecutionException, InterruptedException {
        ServicePathsBuilder servicePathsBuilder = new ServicePathsBuilder();
        servicePathsBuilder.setPathDescription(ServiceDeleteDataUtils
            .createTransactionPathDescription(StringConstants.PP_TOKEN));
        servicePathsBuilder.setServiceAEnd(ServiceDeleteDataUtils.getServiceAEndBuild().build())
            .setServiceZEnd(ServiceDeleteDataUtils.getServiceZEndBuild().build());
        servicePathsBuilder.withKey(new ServicePathsKey("service 1"));
        servicePathsBuilder.setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId("Request 1")
            .build());
        InstanceIdentifier<ServicePaths> servicePathsInstanceIdentifier = InstanceIdentifier.create(
            ServicePathList.class).child(ServicePaths.class, new ServicePathsKey("service 1"));
        TransactionUtils.writeTransaction(
            this.deviceTransactionManager,
            "node1" + StringConstants.PP_TOKEN,
            LogicalDatastoreType.OPERATIONAL,
            servicePathsInstanceIdentifier,
            servicePathsBuilder.build());
    }
}
