/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl22;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion22;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl22;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthServiceImpl;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.stub.MountPointStub;
import org.opendaylight.transportpce.renderer.stub.OlmServiceStub;
import org.opendaylight.transportpce.renderer.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.renderer.utils.ServiceDeleteDataUtils;
import org.opendaylight.transportpce.renderer.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class RendererServiceOperationsImplDeleteTest extends AbstractTest {

    private static final int NUMBER_OF_THREADS = 4;
    private DeviceTransactionManager deviceTransactionManager;
    private RendererServiceOperationsImpl rendererServiceOperations;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private DeviceRendererService deviceRenderer;
    private PortMapping portMapping;
    private CrossConnect crossConnect;
    private NetworkModelWavelengthService networkModelWavelengthService;
    private TransportpceOlmService olmService;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl22 openRoadmInterfacesImpl22;
    private PortMappingVersion22 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl22 crossConnectImpl22;

    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        this.openRoadmInterfacesImpl22 = new OpenRoadmInterfacesImpl22(deviceTransactionManager);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
                openRoadmInterfacesImpl121, openRoadmInterfacesImpl22);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMappingVersion22 =
                new PortMappingVersion22(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
                new PortMappingVersion121(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22, this.mappingUtils,
                this.portMappingVersion121);
        this.crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        this.crossConnectImpl22 = new CrossConnectImpl22(deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(deviceTransactionManager, this.mappingUtils, this.crossConnectImpl121,
                this.crossConnectImpl22);
        this.crossConnect = Mockito.spy(crossConnect);
        OpenRoadmInterfaceFactory openRoadmInterfaceFactory =
                new OpenRoadmInterfaceFactory(portMapping, openRoadmInterfaces);
        this.deviceRenderer = new DeviceRendererServiceImpl(this.getDataBroker(),
                this.deviceTransactionManager, openRoadmInterfaceFactory, openRoadmInterfaces, crossConnect,
            this.portMapping);
    }

    @Before
    public void setUp() {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        this.olmService = Mockito.spy(this.olmService);
        ListeningExecutorService executor =
                MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
        this.networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(getDataBroker());
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(this.deviceRenderer, olmService,
                getDataBroker(), this.networkModelWavelengthService, notificationPublishService);

    }


    @Test
    public void serviceDeleteOperationPp() throws ExecutionException, InterruptedException {
        writePathDescription();
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        serviceDeleteInputBuilder.setServiceHandlerHeader((new ServiceHandlerHeaderBuilder())
                .setRequestId("request1").build());
        Mockito.doReturn(true).when(this.crossConnect).deleteCrossConnect(Mockito.anyString(), Mockito.anyString());
        ServiceDeleteOutput serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build()).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK,
                serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.crossConnect, Mockito.times(2)).deleteCrossConnect(Mockito.any(), Mockito.any());
    }

    @Test
    public void serviceDeleteOperationNoDescription() throws InterruptedException, ExecutionException {
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        ServiceDeleteOutput serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build()).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.any(), Mockito.any());
    }

    @Test
    public void serviceDeleteOperationTearDownFailedAtoZ() throws ExecutionException, InterruptedException {
        Mockito.doReturn(true).when(this.crossConnect).deleteCrossConnect(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
                .setResult("Failed").build()).buildFuture()).when(this.olmService).servicePowerTurndown(Mockito.any());

        writePathDescription();
        ServiceDeleteInputBuilder serviceDeleteInputBuilder = new ServiceDeleteInputBuilder();
        serviceDeleteInputBuilder.setServiceName("service 1");
        serviceDeleteInputBuilder.setServiceHandlerHeader((new ServiceHandlerHeaderBuilder())
                .setRequestId("request1").build());
        ListenableFuture<ServiceDeleteOutput> serviceDeleteOutput
                = this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build());
        ServiceDeleteOutput output = serviceDeleteOutput.get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                output.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node1"), Mockito.any());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node2"), Mockito.any());
    }

    @Test
    public void serviceDeleteOperationTearDownFailedZtoA() throws ExecutionException, InterruptedException {
        Mockito.doReturn(true).when(this.crossConnect).deleteCrossConnect(Mockito.anyString(), Mockito.anyString());
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
        ServiceDeleteOutput serviceDeleteOutput =
                this.rendererServiceOperations.serviceDelete(serviceDeleteInputBuilder.build()).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                serviceDeleteOutput.getConfigurationResponseCommon().getResponseCode());
        Mockito.verify(this.olmService, Mockito.times(2)).servicePowerTurndown(Mockito.any());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node1"), Mockito.any());
        Mockito.verify(this.crossConnect, Mockito.times(0)).deleteCrossConnect(Mockito.eq("node2"), Mockito.any());
    }

    private void writePathDescription() throws ExecutionException, InterruptedException {
        ServicePathsBuilder servicePathsBuilder = new ServicePathsBuilder();
        servicePathsBuilder.setPathDescription(ServiceDeleteDataUtils
            .createTransactionPathDescription(OpenRoadmInterfacesImpl.PP_TOKEN));
        servicePathsBuilder.setServiceAEnd(ServiceDeleteDataUtils.getServiceAEndBuild().build())
            .setServiceZEnd(ServiceDeleteDataUtils.getServiceZEndBuild().build());
        servicePathsBuilder.withKey(new ServicePathsKey("service 1"));
        servicePathsBuilder.setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId("Request 1")
            .build());
        InstanceIdentifier<ServicePaths> servicePathsInstanceIdentifier = InstanceIdentifier.create(
            ServicePathList.class).child(ServicePaths.class, new ServicePathsKey("service 1"));
        TransactionUtils.writeTransaction(
                this.deviceTransactionManager,
                "node1" + OpenRoadmInterfacesImpl.PP_TOKEN,
                LogicalDatastoreType.OPERATIONAL,
                servicePathsInstanceIdentifier,
                servicePathsBuilder.build());
    }
}
