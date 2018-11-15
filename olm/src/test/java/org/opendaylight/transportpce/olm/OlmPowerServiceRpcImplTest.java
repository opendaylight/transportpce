/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.transportpce.olm.service.OlmPowerServiceImpl;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlistBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.LayerRateBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Measurements;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.measurements.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.pm.measurement.PmParameterNameBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.CircuitPackBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class OlmPowerServiceRpcImplTest extends AbstractTest {

    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private OlmPowerService olmPowerService;
    private OlmPowerServiceRpcImpl olmPowerServiceRpc;


    @Before
    public void setUp() {
        this.mountPoint = new MountPointStub(this.getDataBroker());
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl((this.deviceTransactionManager));
        this.portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        this.powerMgmt = new PowerMgmt(this.getDataBroker(), this.openRoadmInterfaces, this.crossConnect,
            this.deviceTransactionManager);
        this.olmPowerService = new OlmPowerServiceImpl(this.getDataBroker(), this.powerMgmt,
            this.deviceTransactionManager, this.portMapping);
        this.olmPowerServiceRpc = new OlmPowerServiceRpcImpl(this.olmPowerService);
    }

    @Test
    public void pmIsNotPresentTest() throws ExecutionException, InterruptedException {
        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        ListenableFuture<RpcResult<GetPmOutput>> output = this.olmPowerServiceRpc.getPm(input);
        Assert.assertEquals(new GetPmOutputBuilder().build(), output.get().getResult());
        Assert.assertEquals(null, output.get().getResult().getResourceId());
        Assert.assertEquals(null, output.get().getResult().getMeasurements());
        Assert.assertEquals(null, output.get().getResult().getGranularity());
        Assert.assertEquals(null, output.get().getResult().getNodeId());
        Assert.assertEquals(null, output.get().getResult().getResourceIdentifier());
        Assert.assertEquals(null, output.get().getResult().getResourceType());
    }

    @Test
    public void testGetPm1() throws ExecutionException, InterruptedException {
        Measurements measurements = new MeasurementsBuilder().setMeasurement(
            new MeasurementBuilder()
                .setPmParameterUnit("unit")
                .setPmParameterName(new PmParameterNameBuilder()
                .setExtension("123")
                .setType(PmNamesEnum.DefectSeconds).build())
                .setPmParameterValue(new PmDataType(BigInteger.valueOf(1234))).build())
            .build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);

        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.Resource resource =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceBuilder()
                .setResource(new CircuitPackBuilder().setCircuitPackName("circuit pack name").build()).build();

        org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Resource resource2 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.ResourceBuilder()
                .setDevice(new DeviceBuilder().setNodeId("node 1").build())
                .setResourceType(new ResourceTypeBuilder()
                    .setExtension("123")
                    .setType(ResourceTypeEnum.Device).build())
                .setResource(resource).build();
        CurrentPm currentPm = new CurrentPmBuilder()
            .setGranularity(PmGranularity._15min)
            .setId("id")
            .setLayerRate(new LayerRateBuilder().build())
            .setMeasurements(measurementsList)
            .setResource(resource2)
            .setRetrievalTime(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715
                .DateAndTime("2018-11-01T12:00:31.456449+06:00")).build();

        List<CurrentPm> currentPmList = new ArrayList<>();
        currentPmList.add(currentPm);

        Optional<CurrentPmlist> currentPmlistOptional = Optional.of(new CurrentPmlistBuilder()
            .setCurrentPm(currentPmList).build());

        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm.output.Measurements
            measurements1 = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm
                .output.MeasurementsBuilder().setPmparameterName("name").setPmparameterValue("1234").build();


        List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm.output
            .Measurements> measurementsList1 = new ArrayList<>();
        measurementsList1.add(measurements1);

        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder().setNodeId(input.getNodeId())
            .setResourceType(input.getResourceType())
            .setResourceIdentifier(input.getResourceIdentifier())
            .setGranularity(input.getGranularity())
            .setMeasurements(measurementsList1);


        ListenableFuture<RpcResult<GetPmOutput>> output = this.olmPowerServiceRpc.getPm(input);
        Assert.assertEquals(new GetPmOutputBuilder().build(), output.get().getResult());
        Assert.assertEquals(null, output.get().getResult().getResourceId());
    }

//    @Test
//    public void testGetPm2() throws ExecutionException, InterruptedException {
//        this.olmPowerService = Mockito.spy(this.olmPowerService);
//        GetPmInput input = new GetPmInputBuilder().setGranularity(PmGranularity._15min).setNodeId("node1")
//            .setResourceIdentifier(new ResourceIdentifierBuilder().setCircuitPackName("circuit pack name")
//                .setResourceName("resource name").build()).setResourceType(ResourceTypeEnum.Device).build();
//        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm.output.Measurements
//        measurements1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm
//            .output.MeasurementsBuilder().setPmparameterName("name").setPmparameterValue("1234").build();
//
//
//        List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm.output
//            .Measurements> measurementsList1 = new ArrayList<>();
//        measurementsList1.add(measurements1);
//
//        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder().setNodeId(input.getNodeId())
//            .setResourceType(input.getResourceType())
//            .setResourceIdentifier(input.getResourceIdentifier())
//            .setGranularity(input.getGranularity())
//            .setMeasurements(measurementsList1);
//
//        Mockito.doReturn(pmOutputBuilder.build()).when(this.olmPowerService).getPm(Mockito.any());
//        ListenableFuture<RpcResult<GetPmOutput>> output = this.olmPowerServiceRpc.getPm(input);
//        Assert.assertEquals(pmOutputBuilder.build(), output.get().getResult());
//        Assert.assertEquals(true, output.get().isSuccessful());
//    }

    @Test
    public void testGetPm3() throws ExecutionException, InterruptedException {
        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        ListenableFuture<RpcResult<GetPmOutput>> output = this.olmPowerServiceRpc.getPm(input);
        Assert.assertEquals(new GetPmOutputBuilder().build(), output.get().getResult());
        Assert.assertEquals(null, output.get().getResult().getResourceId());
        Assert.assertEquals(null, output.get().getResult().getMeasurements());
        Assert.assertEquals(null, output.get().getResult().getGranularity());
        Assert.assertEquals(null, output.get().getResult().getNodeId());
        Assert.assertEquals(null, output.get().getResult().getResourceIdentifier());
        Assert.assertEquals(null, output.get().getResult().getResourceType());
    }

    @Test
    public void testServicePowerSetup1() throws ExecutionException, InterruptedException {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        ListenableFuture<RpcResult<ServicePowerSetupOutput>> output = this.olmPowerServiceRpc.servicePowerSetup(input);
        Assert.assertEquals(new ServicePowerSetupOutputBuilder().setResult("Success").build(), output.get()
                .getResult());
        Assert.assertEquals("Success", output.get().getResult().getResult());
        Assert.assertEquals(true, output.get().isSuccessful());
    }


    @Test
    public void testServicePowerTurndown1() throws ExecutionException, InterruptedException {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        ListenableFuture<RpcResult<ServicePowerTurndownOutput>> output = this.olmPowerServiceRpc
            .servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Success").build(), output.get()
            .getResult());
        Assert.assertEquals("Success", output.get().getResult().getResult());
        Assert.assertEquals(true, output.get().isSuccessful());
    }

    @Test
    public void testCalculateSpanlossBase1() throws ExecutionException, InterruptedException {
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInput();
        ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> output = this.olmPowerServiceRpc
            .calculateSpanlossBase(input);
        Assert.assertEquals(new CalculateSpanlossBaseOutputBuilder().setResult("Failed").build(),
            output.get().getResult());
        Assert.assertEquals("Failed", output.get().getResult().getResult());
        Assert.assertEquals(true, output.get().isSuccessful());
    }

    @Test
    public void testCalculateSpanlossCurrent1() throws ExecutionException, InterruptedException {
        CalculateSpanlossCurrentInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossCurrentInput();
        ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> output = this.olmPowerServiceRpc
            .calculateSpanlossCurrent(input);
        Assert.assertEquals(null, output.get().getResult());
        Assert.assertEquals(true, output.get().isSuccessful());
    }

    @Test
    public void testServicePowerResetInput() throws ExecutionException, InterruptedException {
        ServicePowerResetInput input = OlmPowerServiceRpcImplUtil.getServicePowerResetInput();
        ListenableFuture<RpcResult<ServicePowerResetOutput>> output = this.olmPowerServiceRpc
            .servicePowerReset(input);
        Assert.assertEquals(null, output.get().getResult());
        Assert.assertEquals(true, output.get().isSuccessful());
    }
}
