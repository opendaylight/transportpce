/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.LatencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service.constraints.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.service.applicability.g.ServiceApplicabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.response.parameters.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;


public final class PceTestData {

    private PceTestData() {
    }

    public static PathComputationRequestInput getEmptyPCERequest() {
        return new PathComputationRequestInputBuilder()
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request1")
                    .build())
            .build();
    }

    public static PathComputationRequestInput getEmptyPCERequestServiceNameWithRequestId() {
        return new PathComputationRequestInputBuilder()
            .setServiceName("serviceName")
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request1")
                    .build())
            .build();
    }

    public static PathComputationRequestInput getEmptyPCERequestServiceNameWithOutRequestId() {
        return new PathComputationRequestInputBuilder()
            .setServiceName("serviceName")
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().build())
            .build();
    }

    public static PathComputationRequestInput getPathComputationRequestInputWithCoRoutingOrGeneral2() {
        return new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request1")
                    .build())
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceFormat(ServiceFormat.ODU)
                    .setServiceRate(Uint32.valueOf(100))
                    .setClli("clli11")
                    .setNodeId("XPONDER-2-2")
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceFormat(ServiceFormat.ODU)
                    .setServiceRate(Uint32.valueOf(0))
                    .setClli("Some clli11")
                    .setNodeId("XPONDER-1-2")
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setHardConstraints(
                new HardConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(
                        new CoRoutingBuilder()
                            .setServiceIdentifierList(Map.of(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                        .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                                new ServiceIdentifierListBuilder()
                                    .setServiceIdentifier("Some existing-service")
                                    .build()))
                            .build())
                    .build())
            .setSoftConstraints(
                new SoftConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(
                        new CoRoutingBuilder()
                            .setServiceIdentifierList(Map.of(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                        .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                                new ServiceIdentifierListBuilder()
                                    .setServiceIdentifier("Some existing-service")
                                    .build()))
                        .build())
                    .build())
            .build();
    }

    public static PathComputationRequestInput getPathComputationRequestInputWithCoRoutingOrGeneral() {
        return new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request1")
                    .build())
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setServiceRate(Uint32.valueOf(100))
                    .setClli("clli11")
                    .setNodeId("XPONDER-2-2")
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setServiceRate(Uint32.valueOf(0))
                    .setClli("Some clli11")
                    .setNodeId("XPONDER-1-2")
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setHardConstraints(new HardConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(new CoRoutingBuilder()
                        .setServiceIdentifierList(Map.of(
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                    .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                            new ServiceIdentifierListBuilder()
                                .setServiceIdentifier("Some existing-service")
                                .build()))
                        .build())
                    .build())
            .setSoftConstraints(new SoftConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(new CoRoutingBuilder()
                        .setServiceIdentifierList(Map.of(
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                    .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                            new ServiceIdentifierListBuilder()
                                .setServiceIdentifier("Some existing-service")
                                .build()))
                        .build())
                    .build())
            .build();
    }

    public static PathComputationRequestInput getPCERequest() {
        return new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request1")
                    .build())
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setServiceRate(Uint32.valueOf(100))
                    .setClli("clli11")
                    .setNodeId("XPONDER-2-2")
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setServiceRate(Uint32.valueOf(0))
                    .setClli("Some clli11")
                    .setNodeId("XPONDER-1-2")
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                        .build())
            .setHardConstraints(
                new HardConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(
                        new CoRoutingBuilder()
                            .setServiceIdentifierList(Map.of(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                        .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                                new ServiceIdentifierListBuilder()
                                    .setServiceIdentifier("Some existing-service")
                                    .build()))
                            .build())
                    .build())
            .setSoftConstraints(
                new SoftConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(
                        new CoRoutingBuilder()
                            .setServiceIdentifierList(Map.of(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                        .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                                new ServiceIdentifierListBuilder()
                                    .setServiceIdentifier("Some existing-service")
                                    .build()))
                            .build())
                    .build())
            .build();
    }

    public static PathComputationRequestOutput getFailedPCEResultYes() {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(
                new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator("Yes")
                    .setRequestId("request1")
                    .setResponseCode("Path not calculated")
                    .setResponseMessage("Service Name is not set")
                    .build())
            .setResponseParameters(null)
            .build();
    }

    public static PathComputationRequestOutput getPCEResultOk(Long wl) {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(createCommonSuccessResponse())
            .setResponseParameters(
                new ResponseParametersBuilder()
                    .setPathDescription(createPathDescription(0L, wl, 0L, wl))
                    .build())
            .build();
    }

    /**
     * Generate Data for Test 1 request 5-4.
     * <code>{
     * "pce:input": {
     * "pce:service-name": "service 1",
     * "pce:resource-reserve": "true",
     * "pce:service-handler-header": {
     * "pce:request-id": "request 1"
     * },
     * "pce:service-a-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-1-2"
     * },
     * "pce:service-z-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-3-2"
     * },
     * "pce:pce-metric": "hop-count"
     * }
     * }</code>
     *
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput getPCE_test1_request_54() {
        return new PathComputationRequestInputBuilder()
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request 1")
                    .build())
            .setServiceName("service 1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId("XPONDER-1-2")
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId("XPONDER-3-2")
                    .build())
            .build();
    }

    /**
     * Generate Data for Test 1 result 5-4.
     *
     * @param wl WaveLength
     * @return output PathComputationRequestOutput data
     */
    public static PathComputationRequestOutput getPCE_test_result_54(Long wl) {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(createCommonSuccessResponse())
            .setResponseParameters(
                new ResponseParametersBuilder()
                    .setPathDescription(createPathDescription(0L, wl, 0L, wl))
                    .build())
            .build();
    }

    /**
     * Generate Data for Test 2 request 5-4.
     * <code>{
     * "pce:input": {
     * "pce:service-name": "service 1",
     * "pce:resource-reserve": "true",
     * "pce:service-handler-header": {
     * "pce:request-id": "request 1"
     * },
     * "pce:service-a-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-1-2"
     * },
     * "pce:service-z-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-3-2"
     * },
     * "pce:hard-constraints": {
     * "pce:exclude_": {
     * "node-id": [ "OpenROADM-2-2" ]
     * }
     * },
     * "pce:pce-metric": "hop-count"
     * }
     * }</code>
     *
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput getPCE_test2_request_54() {
        return new PathComputationRequestInputBuilder()
                .setServiceHandlerHeader(
                    new ServiceHandlerHeaderBuilder()
                        .setRequestId("request 1")
                        .build())
                .setServiceName("service 1")
                .setResourceReserve(true)
                .setPceRoutingMetric(PceMetric.HopCount)
                .setServiceAEnd(
                    new ServiceAEndBuilder()
                        .setServiceRate(Uint32.valueOf(0))
                        .setNodeId("XPONDER-1-2")
                        .build())
                .setServiceZEnd(
                    new ServiceZEndBuilder()
                        .setServiceRate(Uint32.valueOf(0))
                        .setNodeId("XPONDER-3-2")
                        .build())
                .setHardConstraints(
                    new HardConstraintsBuilder()
                        .setExclude(
                            new ExcludeBuilder()
                                .setNodeId(Arrays.asList(new NodeIdType("OpenROADM-2-2")))
                                .build())
                        .setInclude(
                            new IncludeBuilder()
                                .setNodeId(Arrays.asList(new NodeIdType("XPONDER-1-2")))
                                .build())
                        .setLatency(
                            new LatencyBuilder()
                                .setMaxLatency(BigDecimal.valueOf(3223))
                                .build())
                        .build())
                .build();
    }

    /**
     * Generate Data for Test 2 result 5-4.
     *
     * @return output PathComputationRequestOutput data
     */
    public static PathComputationRequestOutput getPCE_test2_result_54() {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(createCommonSuccessResponse())
            .setResponseParameters(
                new ResponseParametersBuilder()
                    .setPathDescription(createPathDescription(0L, 9L, 0L, 9L))
                    .build())
            .build();
    }

    /**
     * Generate Data for Test 2 request 5-4.
     * <code>{
     * "pce:input": {
     * "pce:service-name": "service 1",
     * "pce:resource-reserve": "true",
     * "pce:service-handler-header": {
     * "pce:request-id": "request 1"
     * },
     * "pce:service-a-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-1-2"
     * },
     * "pce:service-z-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-3-2"
     * },
     * "pce:hard-constraints": {
     * "pce:exclude_": {
     * "node-id": [ "OpenROADM-2-1", "OpenROADM-2-2" ]
     * }
     * },
     * "pce:pce-metric": "hop-count"
     * }
     * }</code>
     *
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput getPCE_test3_request_54() {
        return new PathComputationRequestInputBuilder()
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request 1")
                    .build())
            .setServiceName("service 1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceRate(Uint32.valueOf(100))
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setNodeId("XPONDER-1-2")
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setNodeId("XPONDER-3-2")
                    .build())
            .setHardConstraints(new HardConstraintsBuilder()
                .setExclude(new ExcludeBuilder()
                    .setNodeId(Arrays.asList(new NodeIdType("OpenROADM-2-1"), new NodeIdType("OpenROADM-2-2")))
                    .build())
                .build())
            .build();
    }

    /**
     * Generate Data for Test 3 result 5-4.
     *
     * @return output PathComputationRequestOutput data
     */
    public static PathComputationRequestOutput getPCE_test3_result_54() {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(createCommonSuccessResponse())
            .setResponseParameters(
                new ResponseParametersBuilder()
                    .setPathDescription(createPathDescription(0L, 9L, 0L, 9L))
                    .build())
            .build();
    }

    public static PathComputationRequestInput getPCE_simpletopology_test1_requestSetHardAndSoftConstrains() {
        return new PathComputationRequestInputBuilder()
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request 1")
                    .build())
            .setServiceName("service 1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId("XPONDER-1-2")
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId("XPONDER-3-2")
                    .build())
            .setHardConstraints(new HardConstraintsBuilder().build())
            .setSoftConstraints(new SoftConstraintsBuilder().build())
            .build();
    }

    public static PathComputationRequestInput getPCE_simpletopology_test1_request() {
        return new PathComputationRequestInputBuilder()
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build())
            .setServiceName("service 1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceAEnd(new ServiceAEndBuilder()
                .setServiceRate(Uint32.valueOf(0))
                .setNodeId("XPONDER-1-2")
                .build())
            .setServiceZEnd(new ServiceZEndBuilder()
                .setServiceRate(Uint32.valueOf(0))
                .setNodeId("XPONDER-3-2")
                .build())
            .build();
    }

    public static PathComputationRequestOutput getPCE_simpletopology_test1_result(Long wl) {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(createCommonSuccessResponse())
            .setResponseParameters(
                new ResponseParametersBuilder()
                    .setPathDescription(createPathDescription(0L, wl, 0L, wl))
                    .build())
            .build();
    }

    private static ConfigurationResponseCommon createCommonSuccessResponse() {
        return new ConfigurationResponseCommonBuilder()
            .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
            .setRequestId("request 1")
            .setResponseCode(ResponseCodes.RESPONSE_OK)
            .setResponseMessage("Path is calculated")
            .build();
    }

    private static PathDescription createPathDescription(long azRate, long azWaveLength, long zaRate,
                                                         long zaWaveLength) {
        return new PathDescriptionBuilder()
            .setAToZDirection(new AToZDirectionBuilder()
                .setRate(Uint32.valueOf(azRate))
                .setAToZWavelengthNumber(Uint32.valueOf(azWaveLength))
                .build())
            .setZToADirection(new ZToADirectionBuilder()
                .setRate(Uint32.valueOf(zaRate))
                .setZToAWavelengthNumber(Uint32.valueOf(zaWaveLength))
                .build())
            .build();
    }

    /**
     * Generate Data for Test Diversity test 1 request 5-4.
     * <code>{
     * "pce:input": {
     * "pce:service-name": "service 1",
     * "pce:resource-reserve": "true",
     * "pce:service-handler-header": {
     * "pce:request-id": "request 1"
     * },
     * "pce:service-a-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-1-1"
     * },
     * "pce:service-z-end": {
     * "pce:service-rate": "0",
     * "pce:node-id": "XPONDER-3-1"
     * },
     * "pce:hard-constraints": {
     * "pce:diversity": {
     * "existing-service": ["Path test-1-54"],
     * "existing-service-applicability": {
     * "node": "true"
     * }
     * }
     * },
     * "pce:pce-metric": "hop-count"
     * }
     * }</code>
     *
     * @param base Path Computation Request Input base
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput build_diversity_from_request(PathComputationRequestInput base) {
        return new PathComputationRequestInputBuilder(base)
            .setServiceName("service 2")
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId("XPONDER-1-1")
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId("XPONDER-3-1")
                    .build())
            .setHardConstraints(
                new HardConstraintsBuilder()
                    .setLatency(
                        new LatencyBuilder()
                            .setMaxLatency(BigDecimal.valueOf(3223))
                            .build())
                    .setDiversity(
                        new DiversityBuilder()
                            .setServiceIdentifierList(
                                Map.of(
                                    new ServiceIdentifierListKey(base.getServiceName()),
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                            .diversity.existing.service.constraints.ServiceIdentifierListBuilder()
                                        .setServiceIndentifier(base.getServiceName())
                                        .setServiceApplicability(
                                            new ServiceApplicabilityBuilder()
                                                .setNode(true)
                                                .build())
                                        .build()))
                            .build())
                    .build())
            .build();
    }

    public static ServiceCreateInput buildServiceCreateInput() {
        return new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210
                        .service.create.input.ServiceAEndBuilder()
                    .setClli("clli")
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId(new NodeIdType("XPONDER-1-2"))
                    .setTxDirection(
                        Map.of(
                            new TxDirectionKey(Uint8.ZERO),
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                                .service.endpoint.TxDirectionBuilder()
                            .setPort(new PortBuilder().build())
                            .build()))
                    .setRxDirection(
                        Map.of(
                            new RxDirectionKey(Uint8.ZERO),
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                                    .service.endpoint.RxDirectionBuilder()
                                .setPort(new PortBuilder().build())
                            .build()))
                    .build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210
                        .service.create.input.ServiceZEndBuilder()
                    .setClli("clli")
                    .setServiceRate(Uint32.valueOf(0))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(
                        Map.of(
                            new TxDirectionKey(Uint8.ZERO),
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                                    .service.endpoint.TxDirectionBuilder()
                                .setPort(new PortBuilder().build())
                                .build()))
                    .setRxDirection(
                        Map.of(
                            new RxDirectionKey(Uint8.ZERO),
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                                    .service.endpoint.RxDirectionBuilder()
                                .setPort(new PortBuilder().build())
                                .build()))
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request 1")
                    .build())
            .build();
    }

    public static PathComputationRequestInput getGnpyPCERequest(String nodeA, String nodeZ) {
        return new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId("request1")
                    .build())
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setServiceRate(Uint32.valueOf(100))
                    .setClli("clli11")
                    .setNodeId(nodeA)
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setServiceFormat(ServiceFormat.Ethernet)
                    .setServiceRate(Uint32.valueOf(0))
                    .setClli("Some clli11")
                    .setNodeId(nodeZ)
                    .setTxDirection(
                        new TxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .setRxDirection(
                        new RxDirectionBuilder()
                            .setPort(
                                new PortBuilder()
                                    .setPortDeviceName("Some port-device-name")
                                    .setPortType("Some port-type")
                                    .setPortName("Some port-name")
                                    .setPortRack("Some port-rack")
                                    .setPortShelf("Some port-shelf")
                                    .setPortSlot("Some port-slot")
                                    .setPortSubSlot("Some port-sub-slot")
                                    .build())
                            .build())
                    .build())
            .setHardConstraints(
                new HardConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(
                        new CoRoutingBuilder()
                            .setServiceIdentifierList(
                                Map.of(
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                                    new ServiceIdentifierListBuilder()
                                        .setServiceIdentifier("Some existing-service")
                                        .build()))
                            .build())
                    .build())
            .setSoftConstraints(
                new SoftConstraintsBuilder()
                    .setCustomerCode(Arrays.asList("Some customer-code"))
                    .setCoRouting(
                        new CoRoutingBuilder()
                            .setServiceIdentifierList(
                                Map.of(
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                                    new ServiceIdentifierListBuilder()
                                        .setServiceIdentifier("Some existing-service")
                                        .build()))
                            .build())
                    .build())
            .build();
    }
}
