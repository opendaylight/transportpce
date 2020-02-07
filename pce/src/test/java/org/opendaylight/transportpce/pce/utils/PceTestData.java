/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.utils;

import java.util.Arrays;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev191129.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev191129.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev191129.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.GeneralBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.diversity.existing.service.contraints.sp.ExistingServiceApplicability;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.diversity.existing.service.contraints.sp.ExistingServiceApplicabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.response.parameters.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeader;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeaderBuilder;

public final class PceTestData {

    private PceTestData() {
    }

    public static PathComputationRequestInput getEmptyPCERequest() {
        ServiceHandlerHeader serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("request1")
                .build();
        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceHandlerHeader(serviceHandlerHeader)
                .build();
        return input;
    }

    public static PathComputationRequestInput getPCERequest() {
        ServiceHandlerHeader serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("request1")
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(100L)
                .setClli("clli11")
                .setNodeId("XPONDER-2-2")
                .setTxDirection(new TxDirectionBuilder().setPort(
                        new PortBuilder()
                                .setPortDeviceName("Some port-device-name")
                                .setPortType("Some port-type")
                                .setPortName("Some port-name")
                                .setPortRack("Some port-rack")
                                .setPortShelf("Some port-shelf")
                                .setPortSlot("Some port-slot")
                                .setPortSubSlot("Some port-sub-slot")
                                .build()
                ).build())
                .setRxDirection(new RxDirectionBuilder().setPort(
                        new PortBuilder()
                                .setPortDeviceName("Some port-device-name")
                                .setPortType("Some port-type")
                                .setPortName("Some port-name")
                                .setPortRack("Some port-rack")
                                .setPortShelf("Some port-shelf")
                                .setPortSlot("Some port-slot")
                                .setPortSubSlot("Some port-sub-slot")
                                .build()
                ).build())
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(0L)
                .setClli("Some clli11")
                .setNodeId("XPONDER-1-2")
                .setTxDirection(new TxDirectionBuilder().setPort(
                        new PortBuilder()
                                .setPortDeviceName("Some port-device-name")
                                .setPortType("Some port-type")
                                .setPortName("Some port-name")
                                .setPortRack("Some port-rack")
                                .setPortShelf("Some port-shelf")
                                .setPortSlot("Some port-slot")
                                .setPortSubSlot("Some port-sub-slot")
                                .build()
                ).build())
                .setRxDirection(new RxDirectionBuilder().setPort(
                        new PortBuilder()
                                .setPortDeviceName("Some port-device-name")
                                .setPortType("Some port-type")
                                .setPortName("Some port-name")
                                .setPortRack("Some port-rack")
                                .setPortShelf("Some port-shelf")
                                .setPortSlot("Some port-slot")
                                .setPortSubSlot("Some port-sub-slot")
                                .build()
                ).build())
                .build();
        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceMetric(PceMetric.HopCount)
            .setLocallyProtectedLinks(true)
            .setServiceHandlerHeader(serviceHandlerHeader)
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setHardConstraints(new HardConstraintsBuilder()
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .setCoRoutingOrGeneral(new CoRoutingBuilder()
            .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                 .rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
            .setExistingService(Arrays.asList("Some existing-service"))
            .build())
            .build())
            .build())
            .setSoftConstraints(new SoftConstraintsBuilder()
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .setCoRoutingOrGeneral(new CoRoutingBuilder()
            .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                .rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
            .setExistingService(Arrays.asList("Some existing-service"))
            .build())
            .build())
            .build())
            .build();
        return input;
    }

    public static PathComputationRequestOutput getFailedPCEResultYes() {
        PathComputationRequestOutputBuilder outputBuilder = new PathComputationRequestOutputBuilder();
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("Yes")
                .setRequestId("request1")
                .setResponseCode("Path not calculated")
                .setResponseMessage("Service Name is not set");
        outputBuilder.setConfigurationResponseCommon(configurationResponseCommon.build())
                .setResponseParameters(null);
        return outputBuilder.build();
    }

    public static PathComputationRequestOutput getPCEResultOk(Long wl) {
        PathComputationRequestOutputBuilder outputBuilder = new PathComputationRequestOutputBuilder();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(createPathDescription(0L, wl, 0L, wl))
                .build();
        outputBuilder.setConfigurationResponseCommon(createCommonSuccessResponse())
                .setResponseParameters(responseParameters);
        return outputBuilder.build();
    }

    /**
     * Generate Data for Test 1 request 5-4.
     * <code>{
       "pce:input": {
         "pce:service-name": "service 1",
         "pce:resource-reserve": "true",
         "pce:service-handler-header": {
           "pce:request-id": "request 1"
         },
         "pce:service-a-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-1-2"
         },
         "pce:service-z-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-3-2"
         },
         "pce:pce-metric": "hop-count"
       }
     }</code>
     *
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput getPCE_test1_request_54() {
        ServiceHandlerHeader serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-1-2")
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-3-2")
                .build();
        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceHandlerHeader(serviceHandlerHeader)
                .setServiceName("service 1")
                .setResourceReserve(true)
                .setPceMetric(PceMetric.HopCount)
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();
        return input;
    }

    /**
     * Generate Data for Test 1 result 5-4.
     * @param wl WaveLength
     *
     * @return output PathComputationRequestOutput data
     */
    public static PathComputationRequestOutput getPCE_test_result_54(Long wl) {
        PathComputationRequestOutputBuilder outputBuilder = new PathComputationRequestOutputBuilder();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(createPathDescription(0L, wl, 0L, wl))
                .build();
        outputBuilder.setConfigurationResponseCommon(createCommonSuccessResponse())
                .setResponseParameters(responseParameters);
        return outputBuilder.build();
    }

    /**
     * Generate Data for Test 2 request 5-4.
     *<code>{
       "pce:input": {
         "pce:service-name": "service 1",
         "pce:resource-reserve": "true",
         "pce:service-handler-header": {
           "pce:request-id": "request 1"
         },
         "pce:service-a-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-1-2"
         },
         "pce:service-z-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-3-2"
         },
         "pce:hard-constraints": {
           "pce:exclude_": {
             "node-id": [ "OpenROADM-2-2" ]
           }
         },
         "pce:pce-metric": "hop-count"
       }
     }</code>
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput getPCE_test2_request_54() {
        ServiceHandlerHeader serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-1-2")
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-3-2")
                .build();
        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceHandlerHeader(serviceHandlerHeader)
                .setServiceName("service 1")
                .setResourceReserve(true)
                .setPceMetric(PceMetric.HopCount)
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .setHardConstraints(new HardConstraintsBuilder()
                        .setCoRoutingOrGeneral(new GeneralBuilder()
                                .setExclude(new ExcludeBuilder()
                                        .setNodeId(Arrays.asList("OpenROADM-2-2"))
                                        .build())
                                .build())
                        .build())
                .build();
        return input;
    }

    /**
     * Generate Data for Test 2 result 5-4.
     * @return output PathComputationRequestOutput data
     */
    public static PathComputationRequestOutput getPCE_test2_result_54() {
        PathComputationRequestOutputBuilder outputBuilder = new PathComputationRequestOutputBuilder();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(createPathDescription(0L, 9L, 0L, 9L))
                .build();
        outputBuilder.setConfigurationResponseCommon(createCommonSuccessResponse())
                .setResponseParameters(responseParameters);
        return outputBuilder.build();
    }

    /**
     * Generate Data for Test 2 request 5-4.
     *<code>{
       "pce:input": {
         "pce:service-name": "service 1",
         "pce:resource-reserve": "true",
         "pce:service-handler-header": {
           "pce:request-id": "request 1"
         },
         "pce:service-a-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-1-2"
         },
         "pce:service-z-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-3-2"
         },
         "pce:hard-constraints": {
           "pce:exclude_": {
             "node-id": [ "OpenROADM-2-1", "OpenROADM-2-2" ]
           }
         },
         "pce:pce-metric": "hop-count"
       }
     }</code>
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput getPCE_test3_request_54() {
        ServiceHandlerHeader serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-1-2")
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-3-2")
                .build();
        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceHandlerHeader(serviceHandlerHeader)
                .setServiceName("service 1")
                .setResourceReserve(true)
                .setPceMetric(PceMetric.HopCount)
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .setHardConstraints(new HardConstraintsBuilder()
                        .setCoRoutingOrGeneral(new GeneralBuilder()
                                .setExclude(new ExcludeBuilder()
                                        .setNodeId(Arrays.asList("OpenROADM-2-1", "OpenROADM-2-2"))
                                        .build())
                                .build())
                        .build())
                .build();
        return input;
    }

    /**
     * Generate Data for Test 3 result 5-4.
     * @return output PathComputationRequestOutput data
     */
    public static PathComputationRequestOutput getPCE_test3_result_54() {
        PathComputationRequestOutputBuilder outputBuilder = new PathComputationRequestOutputBuilder();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(createPathDescription(0L, 9L, 0L, 9L))
                .build();
        outputBuilder.setConfigurationResponseCommon(createCommonSuccessResponse())
                .setResponseParameters(responseParameters);
        return outputBuilder.build();
    }

    public static PathComputationRequestInput getPCE_simpletopology_test1_request() {
        ServiceHandlerHeader serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build();
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-1-2")
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-3-2")
                .build();
        PathComputationRequestInput input = new PathComputationRequestInputBuilder()
                .setServiceHandlerHeader(serviceHandlerHeader)
                .setServiceName("service 1")
                .setResourceReserve(true)
                .setPceMetric(PceMetric.HopCount)
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();
        return input;
    }

    public static PathComputationRequestOutput getPCE_simpletopology_test1_result(Long wl) {
        PathComputationRequestOutputBuilder outputBuilder = new PathComputationRequestOutputBuilder();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(createPathDescription(0L, wl, 0L, wl))
                .build();
        outputBuilder.setConfigurationResponseCommon(createCommonSuccessResponse())
                .setResponseParameters(responseParameters);
        return outputBuilder.build();
    }

    private static ConfigurationResponseCommon createCommonSuccessResponse() {
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                .setRequestId("request 1")
                .setResponseCode(ResponseCodes.RESPONSE_OK)
                .setResponseMessage("Path is calculated");
        return configurationResponseCommon.build();
    }

    private static PathDescription createPathDescription(long azRate, long azWaveLength, long zaRate,
        long zaWaveLength) {
        AToZDirection atozDirection = new AToZDirectionBuilder()
                .setRate(azRate)
                .setAToZWavelengthNumber(azWaveLength)
                .setAToZ(null)
                .build();
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
                .setRate(zaRate)
                .setZToAWavelengthNumber(zaWaveLength)
                .setZToA(null)
                .build();
        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(atozDirection)
                .setZToADirection(ztoaDirection)
                .build();
        return pathDescription;
    }

    /**
     * Generate Data for Test Diversity test 1 request 5-4.
     *<code>{
       "pce:input": {
         "pce:service-name": "service 1",
         "pce:resource-reserve": "true",
         "pce:service-handler-header": {
           "pce:request-id": "request 1"
         },
         "pce:service-a-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-1-1"
         },
         "pce:service-z-end": {
           "pce:service-rate": "0",
           "pce:node-id": "XPONDER-3-1"
         },
         "pce:hard-constraints": {
           "pce:diversity": {
            "existing-service": ["Path test-1-54"],
            "existing-service-applicability": {
                "node": "true"
            }
           }
         },
         "pce:pce-metric": "hop-count"
       }
     }</code>
     * @param base Path Computation Request Input base
     *
     * @return input PathComputationRequestInput data
     */
    public static PathComputationRequestInput build_diversity_from_request(PathComputationRequestInput base) {

        ExistingServiceApplicability nodeTrue = new ExistingServiceApplicabilityBuilder()
                .setNode(true).build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-1-1")
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setServiceRate(0L)
                .setNodeId("XPONDER-3-1")
                .build();

        PathComputationRequestInput input = new PathComputationRequestInputBuilder(base)
                .setServiceName("service 2")
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .setHardConstraints(new HardConstraintsBuilder()
                        .setCoRoutingOrGeneral(new GeneralBuilder()
                                .setDiversity(new DiversityBuilder()
                                        .setExistingService(Arrays.asList(base.getServiceName()))
                                        .setExistingServiceApplicability(nodeTrue)
                                        .build())
                                .build())
                        .build())
                .build();
        return input;
    }

    public static ServiceCreateInput buildServiceCreateInput() {

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev191129.service.create.input.ServiceAEnd serviceAEnd =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.service
                .rev191129.service.create.input.ServiceAEndBuilder()
                .setClli("clli")
                .setServiceRate((long)0)
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types
                    .rev191129.service.endpoint.TxDirectionBuilder()
                    .setPort(new PortBuilder().build())
                    .build())
                .setRxDirection(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types
                    .rev191129.service.endpoint.RxDirectionBuilder()
                    .setPort(new PortBuilder().build())
                    .build())
                .build();

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev191129.service.create.input.ServiceZEnd serviceZEnd =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.service
            .rev191129.service.create.input.ServiceZEndBuilder()
                .setClli("clli")
                .setServiceRate((long)0)
                .setNodeId(new NodeIdType("XPONDER-3-2"))
                .setTxDirection(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types
                    .rev191129.service.endpoint.TxDirectionBuilder()
                    .setPort(new PortBuilder().build())
                    .build())
                .setRxDirection(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types
                    .rev191129.service.endpoint.RxDirectionBuilder()
                    .setPort(new PortBuilder().build())
                    .build())
                .build();

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder()
                .setRequestId("request 1")
                .build());

        return builtInput.build();
    }
}
