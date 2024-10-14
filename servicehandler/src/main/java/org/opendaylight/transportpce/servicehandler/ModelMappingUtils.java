/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd1Builder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd1Builder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SpectrumAllocation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.response.parameters.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.hierarchy.TransportAssignmentBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.hierarchy.transport.assignment.McTtpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.hierarchy.transport.assignment.NmcCtp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.hierarchy.transport.assignment.NmcCtpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.hierarchy.transport.assignment.NmcCtpKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexCbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexFlexe;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexGfp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexImp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTUflex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OtuRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list.services.SupportingServiceHierarchy;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list.services.SupportingServiceHierarchyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list.services.SupportingServiceHierarchyKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelMappingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ModelMappingUtils.class);

    private static final ImmutableMap<String, OduRateIdentity> ODU_RATE_MAP =
        ImmutableMap.<String, OduRateIdentity>builder()
            .put("ODU0", ODU0.VALUE)
            .put("ODU1", ODU1.VALUE)
            .put("ODU2", ODU2.VALUE)
            .put("ODU2e", ODU2e.VALUE)
            .put("ODU3", ODU3.VALUE)
            .put("ODU4", ODU4.VALUE)
            .put("ODUCn", ODUCn.VALUE)
            .put("ODUflexCbr", ODUflexCbr.VALUE)
            .put("ODUflexFlexe", ODUflexFlexe.VALUE)
            .put("ODUflexGfp", ODUflexGfp.VALUE)
            .put("ODUflexImp", ODUflexImp.VALUE)
            .build();

    private static final ImmutableMap<String, OtuRateIdentity> OTU_RATE_MAP =
        ImmutableMap.<String, OtuRateIdentity>builder()
            .put("OTU0", OTU0.VALUE)
            .put("OTU1", OTU1.VALUE)
            .put("OTU2", OTU2.VALUE)
            .put("OTU2e", OTU2e.VALUE)
            .put("OTU3", OTU3.VALUE)
            .put("OTU4", OTU4.VALUE)
            .put("OTUCn", OTUCn.VALUE)
            .put("OTUflex", OTUflex.VALUE)
            .build();

    private ModelMappingUtils() {
    }

    public static ServiceImplementationRequestInput createServiceImplementationRequest(
            ServiceInput input,
            PathDescription pathDescription) {
        return new ServiceImplementationRequestInputBuilder()
            .setConnectionType(input.getConnectionType())
            .setServiceName(
                input.isServiceReconfigure()
                    ? input.getNewServiceName()
                    : input.getServiceName())
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                        .service.implementation.request.input.ServiceAEndBuilder()
                    .setServiceFormat(input.getServiceAEnd().getServiceFormat())
                    .setServiceRate(input.getServiceAEnd().getServiceRate())
                    .setOtuServiceRate(getOtuServiceRate(input.getServiceAEnd().getOtuServiceRate()))
                    .setOduServiceRate(getOduServiceRate(input.getServiceAEnd().getOduServiceRate()))
                    .setClli(input.getServiceAEnd().getClli())
                    .setNodeId(new NodeIdType(input.getServiceAEnd().getNodeId().getValue()).getValue())
                    .setTxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.TxDirectionBuilder()
                            .setPort(
                                input.getServiceAEnd().getTxDirection().values().stream().findFirst().orElseThrow()
                                    .getPort())
                            .build())
                    .setRxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.RxDirectionBuilder()
                            .setPort(
                                input.getServiceAEnd().getRxDirection().values().stream().findFirst().orElseThrow()
                                    .getPort())
                            .build())
                    .build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                        .service.implementation.request.input.ServiceZEndBuilder()
                    .setServiceFormat(input.getServiceZEnd().getServiceFormat())
                    .setServiceRate(input.getServiceZEnd().getServiceRate())
                    .setOtuServiceRate(getOtuServiceRate(input.getServiceZEnd().getOtuServiceRate()))
                    .setOduServiceRate(getOduServiceRate(input.getServiceZEnd().getOduServiceRate()))
                    .setClli(input.getServiceZEnd().getClli())
                    .setNodeId(new NodeIdType(input.getServiceZEnd().getNodeId().getValue()).getValue())
                    .setTxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.TxDirectionBuilder()
                            .setPort(
                                input.getServiceZEnd().getTxDirection().values().stream().findFirst().orElseThrow()
                                    .getPort())
                            .build())
                    .setRxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.RxDirectionBuilder()
                            .setPort(
                                input.getServiceZEnd().getRxDirection().values().stream().findFirst().orElseThrow()
                                    .getPort())
                            .build())
                    .build())
            .setConnectionType(input.getConnectionType())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(input.getSdncRequestHeader().getRequestId()).build())
            .setPathDescription(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                        .service.implementation.request.input.PathDescriptionBuilder()
                    .setAToZDirection(pathDescription.getAToZDirection())
                    .setZToADirection(pathDescription.getZToADirection())
                    .build())
            .build();
    }

    private static OduRateIdentity getOduServiceRate(OduRateIdentity oduServiceRate) {
        if (oduServiceRate == null || !ODU_RATE_MAP.containsKey(oduServiceRate.toString())) {
            LOG.error("ODU rate {} not recognized", oduServiceRate);
            return null;
        }
        return ODU_RATE_MAP.get(oduServiceRate.toString());
    }

    private static OtuRateIdentity getOtuServiceRate(OtuRateIdentity otuServiceRate) {
        if (otuServiceRate == null || !OTU_RATE_MAP.containsKey(otuServiceRate.toString())) {
            LOG.error("OTU rate {} not recognized", otuServiceRate);
            return null;
        }
        return OTU_RATE_MAP.get(otuServiceRate.toString());
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(ServiceInput serviceInput) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceInput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId(serviceInput.getSdncRequestHeader().getRequestId())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(
                ServiceRerouteInput serviceRerouteinput,
                Services services) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceRerouteinput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId(services.getSdncRequestHeader().getRequestId())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(
                ServiceRestorationInput serviceRestorationInput,
                Services services) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceRestorationInput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId(services.getSdncRequestHeader().getRequestId())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(ServiceReconfigureInput serviceReconfigureInput) {
        String serviceName = serviceReconfigureInput.getServiceName();
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceName)
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(serviceName + "-reconfigure").build())
            .build();
    }

    public static ServiceAEnd createServiceAEnd(
            ServiceEndpoint serviceAEnd,
            SpectrumAllocation spectrumAEndAllocation) {

        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder()
                .setClli(serviceAEnd.getClli())
                .setNodeId(serviceAEnd.getNodeId().getValue())
                .setRxDirection(
                        createRxDirection(serviceAEnd.getRxDirection().values().stream().findFirst().orElseThrow()))
                .setServiceFormat(serviceAEnd.getServiceFormat())
                .setServiceRate(serviceAEnd.getServiceRate())
                .setTxDirection(
                        createTxDirection(serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow()));


        if (spectrumAEndAllocation != null) {

            ServiceAEnd1Builder serviceAEnd1Builder = new ServiceAEnd1Builder();
            if (spectrumAEndAllocation.getFrequencySlot() != null) {
                serviceAEnd1Builder.setFrequencySlot(spectrumAEndAllocation.getFrequencySlot());
            }
            if (spectrumAEndAllocation.getFrequencyRange() != null) {
                serviceAEnd1Builder.setFrequencyRange(spectrumAEndAllocation.getFrequencyRange());
            }

            serviceAEndBuilder.addAugmentation(serviceAEnd1Builder.build());
        }

        return serviceAEndBuilder.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205
            .path.computation.reroute.request.input.ServiceAEnd createServiceAEndReroute(ServiceEndpoint serviceAEnd) {
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205
                    .path.computation.reroute.request.input.ServiceAEndBuilder()
                .setClli(serviceAEnd.getClli())
                .setNodeId(serviceAEnd.getNodeId().getValue())
                .setRxDirection(createRxDirection(serviceAEnd.getRxDirection().values().stream().findFirst()
                        .orElseThrow()))
                .setServiceFormat(serviceAEnd.getServiceFormat())
                .setServiceRate(serviceAEnd.getServiceRate())
                .setTxDirection(createTxDirection(serviceAEnd.getTxDirection().values().stream().findFirst()
                        .orElseThrow()))
                .build();
    }

    public static ServiceZEnd createServiceZEnd(
            ServiceEndpoint serviceZEnd,
            SpectrumAllocation spectrumZEndAllocation
    ) {
        ServiceZEndBuilder serviceZEndBuilder = new ServiceZEndBuilder()
                .setClli(serviceZEnd.getClli())
                .setNodeId(serviceZEnd.getNodeId().getValue())
                .setRxDirection(
                        createRxDirection(serviceZEnd.getRxDirection().values().stream().findFirst().orElseThrow()))
                .setServiceFormat(serviceZEnd.getServiceFormat())
                .setServiceRate(serviceZEnd.getServiceRate())
                .setTxDirection(
                        createTxDirection(serviceZEnd.getTxDirection().values().stream().findFirst().orElseThrow()));

        if (spectrumZEndAllocation != null) {

            ServiceZEnd1Builder serviceZEnd1Builder = new ServiceZEnd1Builder();
            if (spectrumZEndAllocation.getFrequencySlot() != null) {
                serviceZEnd1Builder.setFrequencySlot(spectrumZEndAllocation.getFrequencySlot());
            }
            if (spectrumZEndAllocation.getFrequencyRange() != null) {
                serviceZEnd1Builder.setFrequencyRange(spectrumZEndAllocation.getFrequencyRange());
            }

            serviceZEndBuilder.addAugmentation(serviceZEnd1Builder.build());
        }

        return serviceZEndBuilder.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205
            .path.computation.reroute.request.input.ServiceZEnd createServiceZEndReroute(ServiceEndpoint serviceZEnd) {
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205
                    .path.computation.reroute.request.input.ServiceZEndBuilder()
                .setClli(serviceZEnd.getClli())
                .setNodeId(serviceZEnd.getNodeId().getValue())
                .setRxDirection(createRxDirection(serviceZEnd.getRxDirection().values().stream().findFirst()
                        .orElseThrow()))
                .setServiceFormat(serviceZEnd.getServiceFormat())
                .setServiceRate(serviceZEnd.getServiceRate())
                .setTxDirection(createTxDirection(serviceZEnd.getTxDirection().values().stream().findFirst()
                        .orElseThrow()))
                .build();
    }

    public static RxDirection createRxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                .service.endpoint.RxDirection rxDirection) {
        return new RxDirectionBuilder().setPort(rxDirection.getPort()).build();
    }

    public static TxDirection createTxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                .service.endpoint.TxDirection txDirection) {
        return new TxDirectionBuilder().setPort(txDirection.getPort()).build();
    }

    public static ListenableFuture<RpcResult<ServiceDeleteOutput>> createDeleteServiceReply(
            ServiceDeleteInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceDeleteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<TempServiceDeleteOutput>> createDeleteServiceReply(
            TempServiceDeleteInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new TempServiceDeleteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(null)
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceCreateOutput>> createCreateServiceReply(
            ServiceCreateInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceCreateOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .setResponseParameters(new ResponseParametersBuilder().build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<TempServiceCreateOutput>> createCreateServiceReply(
            TempServiceCreateInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new TempServiceCreateOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                             .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .setResponseParameters(new ResponseParametersBuilder().build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> createCreateServiceReply(
            ServiceFeasibilityCheckInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceFeasibilityCheckOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .setResponseParameters(new ResponseParametersBuilder().build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceReconfigureOutput>> createCreateServiceReply(
            ServiceReconfigureInput input, String message) {
        return RpcResultBuilder
            .success(
                new ServiceReconfigureOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setResponseMessage(message)
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRerouteOutput>> createRerouteServiceReply(
            ServiceRerouteInput input, String finalAckYes, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceRerouteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                             .setAckFinalIndicator(finalAckYes)
                             .setResponseCode(responseCode)
                             .setResponseMessage(message)
                             .build())
                    .setHardConstraints(null)
                    .setSoftConstraints(null)
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRestorationOutput>> createRestoreServiceReply(String message) {
        return RpcResultBuilder
            .success(
                new ServiceRestorationOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setResponseMessage(message)
                            .build())
                    .build())
            .buildFuture();
    }

    public static Services mappingServices(
            ServiceCreateInput serviceCreateInput,
            ServiceReconfigureInput serviceReconfigureInput) {
        if (serviceCreateInput != null) {
            return new ServicesBuilder()
                .setServiceName(serviceCreateInput.getServiceName())
                .setAdministrativeState(AdminStates.OutOfService)
                .setOperationalState(State.OutOfService)
                .setCommonId(serviceCreateInput.getCommonId())
                .setConnectionType(serviceCreateInput.getConnectionType())
                .setCustomer(serviceCreateInput.getCustomer())
                .setCustomerContact(serviceCreateInput.getCustomerContact())
                .setServiceResiliency(serviceCreateInput.getServiceResiliency())
                .setHardConstraints(serviceCreateInput.getHardConstraints())
                .setSoftConstraints(serviceCreateInput.getSoftConstraints())
                .setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader())
                .setLifecycleState(LifecycleState.Planned)
                .setServiceAEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                            .service.ServiceAEndBuilder(serviceCreateInput.getServiceAEnd())
                        .build())
                .setServiceZEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                            .service.ServiceZEndBuilder(serviceCreateInput.getServiceZEnd())
                        .build())
                .build();
        }
        if (serviceReconfigureInput != null) {
            return new ServicesBuilder()
                .setServiceName(serviceReconfigureInput.getServiceName())
                .setAdministrativeState(AdminStates.OutOfService)
                .setOperationalState(State.OutOfService)
                .setCommonId(serviceReconfigureInput.getCommonId())
                .setConnectionType(serviceReconfigureInput.getConnectionType())
                .setCustomer(serviceReconfigureInput.getCustomer())
                .setCustomerContact(serviceReconfigureInput.getCustomerContact())
                .setHardConstraints(serviceReconfigureInput.getHardConstraints())
                .setSoftConstraints(serviceReconfigureInput.getSoftConstraints())
                .setLifecycleState(LifecycleState.Planned)
                .setServiceAEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                            .service.ServiceAEndBuilder(serviceReconfigureInput.getServiceAEnd())
                        .build())
                .setServiceZEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                            .service.ServiceZEndBuilder(serviceReconfigureInput.getServiceZEnd())
                        .build())
                .build();
        }
        //FIXME: Because of Silicon, we cannot have empty key.
        //it's this case possible ? There is a Junit test covering null
        //temporary workaround as now there is a null key check done by yangtools.
        //Functional review is needed
        LOG.warn("ServiceCreateInput and ServiceReconfigureInput are null");
        return new ServicesBuilder().withKey(new ServicesKey("unknown")).build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
            .temp.service.list.Services mappingServices(TempServiceCreateInput tempServiceCreateInput,
                                                        PathDescription pathDescription) {
        Map<SupportingServiceHierarchyKey, SupportingServiceHierarchy> supportingServiceHierarchyMap = new HashMap<>();
        Map<NmcCtpKey, NmcCtp> nmcCtpMap = new HashMap<>();
        SupportingServiceHierarchyKey supportingServiceHierarchyKey = new SupportingServiceHierarchyKey(
                tempServiceCreateInput.getCommonId());
        // TODO: here we assume the A-Z and Z-A has parameters
        LOG.info("Min and Max frequencies are {} {}", pathDescription.getAToZDirection().getAToZMinFrequency(),
                pathDescription.getAToZDirection().getAToZMinFrequency());
        nmcCtpMap.put(
            new NmcCtpKey("1"),
            new NmcCtpBuilder()
                .setId("1")
                .setFrequency(pathDescription.getAToZDirection().getCentralFrequency())
                .setWidth(pathDescription.getAToZDirection().getWidth())
                .build());
        supportingServiceHierarchyMap.put(
            supportingServiceHierarchyKey,
            new SupportingServiceHierarchyBuilder().setServiceIdentifier(tempServiceCreateInput.getCommonId())
                .setTransportAssignment(
                    new TransportAssignmentBuilder()
                        .setMcTtp(
                            new McTtpBuilder()
                                .setMaxFreq(pathDescription.getAToZDirection().getAToZMaxFrequency())
                                .setMinFreq(pathDescription.getAToZDirection().getAToZMinFrequency())
                                .build())
                        .setNmcCtp(nmcCtpMap)
                        .build())
                .build());
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .temp.service.list.ServicesBuilder()
            .setServiceName(tempServiceCreateInput.getCommonId())
            .setAdministrativeState(AdminStates.OutOfService)
            .setOperationalState(State.OutOfService)
            .setCommonId(tempServiceCreateInput.getCommonId())
            .setConnectionType(tempServiceCreateInput.getConnectionType())
            .setCustomer(tempServiceCreateInput.getCustomer())
            .setCustomerContact(tempServiceCreateInput.getCustomerContact())
            .setHardConstraints(tempServiceCreateInput.getHardConstraints())
            .setSoftConstraints(tempServiceCreateInput.getSoftConstraints())
            .setSdncRequestHeader(tempServiceCreateInput.getSdncRequestHeader())
            .setLifecycleState(LifecycleState.Planned)
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                    .service.ServiceAEndBuilder(tempServiceCreateInput.getServiceAEnd()).build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                    .service.ServiceZEndBuilder(tempServiceCreateInput.getServiceZEnd()).build())
            .setSupportingServiceHierarchy(supportingServiceHierarchyMap)
            .build();
    }

    public static ServicePaths mappingServicePaths(ServiceInput serviceInput, PathComputationRequestOutput output) {
        if (serviceInput == null) {
            //FIXME: Because of Silicon, we cannot have empty key.
            //it's this case possible ? There is a Junit test covering null
            //temporary workaround as now there is a null key check done by yangtools.
            //Functional review is needed
            LOG.warn("ServiceInput is null");
            return new ServicePathsBuilder().withKey(new ServicePathsKey("unknown")).build();
        }

        ServicePathsBuilder servicePathBuilder =
            new ServicePathsBuilder()
                .setServiceAEnd(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.path.ServiceAEndBuilder()
                        .setServiceFormat(serviceInput.getServiceAEnd().getServiceFormat())
                        .setServiceRate(serviceInput.getServiceAEnd().getServiceRate())
                        .setClli(serviceInput.getServiceAEnd().getClli())
                        .setNodeId(new NodeIdType(serviceInput.getServiceAEnd().getNodeId().getValue()).getValue())
                        .setTxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.TxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceAEnd().getTxDirection()
                                        .values().stream().findFirst().orElseThrow().getPort())
                                .build())
                        .setRxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.RxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceAEnd().getRxDirection()
                                        .values().stream().findFirst().orElseThrow().getPort())
                                .build())
                        .build())
                .setServiceZEnd(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.path.ServiceZEndBuilder()
                        .setServiceFormat(serviceInput.getServiceZEnd().getServiceFormat())
                        .setServiceRate(serviceInput.getServiceZEnd().getServiceRate())
                        .setClli(serviceInput.getServiceZEnd().getClli())
                        .setNodeId(new NodeIdType(serviceInput.getServiceZEnd().getNodeId().getValue()).getValue())
                        .setTxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.TxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceZEnd().getTxDirection()
                                        .values().stream().findFirst().orElseThrow().getPort())
                                .build())
                        .setRxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.RxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceZEnd().getRxDirection()
                                        .values().stream().findFirst().orElseThrow().getPort())
                                .build())
                        .build())
                .setServicePathName(serviceInput.getServiceName())
                .setServiceHandlerHeader(
                    new ServiceHandlerHeaderBuilder()
                        .setRequestId(serviceInput.getSdncRequestHeader()
                        .getRequestId())
                        .build());
        if (serviceInput.getHardConstraints() != null) {
            servicePathBuilder.setHardConstraints(serviceInput.getHardConstraints());
        }
        if (serviceInput.getSoftConstraints() != null) {
            servicePathBuilder.setSoftConstraints(serviceInput.getSoftConstraints());
        }

        if (output.getResponseParameters().getPathDescription() != null) {
            servicePathBuilder.setPathDescription(
                new PathDescriptionBuilder(output.getResponseParameters().getPathDescription()).build());
        }
        return servicePathBuilder.build();
    }


    @SuppressFBWarnings(
        value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
        justification = "not relevant to return and zero length array as we need real pos")
    public static int[] findTheLongestSubstring(String s1, String s2) {
        if ((s1 == null) || (s2 == null)) {
            return null;
        }
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        int maxLen = 0;
        int endPos = 0;
        for (int i = 1; i < dp.length; i++) {
            for (int j = 1; j < dp[0].length; j++) {
                char ch1 = s1.charAt(i - 1);
                char ch2 = s2.charAt(j - 1);
                if (ch1 == ch2) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] >= maxLen) {
                        maxLen = dp[i][j];
                        endPos = i;
                    }
                }
            }
        }
        return new int[] { endPos - maxLen, endPos };
    }

    public static ListenableFuture<RpcResult<AddOpenroadmOperationalModesToCatalogOutput>>
        addOpenroadmServiceReply(AddOpenroadmOperationalModesToCatalogInput input, String finalAck,
                                 String message,String responseCode) {
        return RpcResultBuilder
            .success(
                new AddOpenroadmOperationalModesToCatalogOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                                .setAckFinalIndicator(finalAck)
                                .setResponseMessage(message)
                            .setResponseCode(responseCode)
                                .setRequestId(
                                        input.getSdncRequestHeader() == null
                                                ? null
                                                : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<AddSpecificOperationalModesToCatalogOutput>>
        addSpecificOpenroadmServiceReply(AddSpecificOperationalModesToCatalogInput input, String finalAck,
                                         String message,String responseCode) {
        return RpcResultBuilder
                .success(
                        new AddSpecificOperationalModesToCatalogOutputBuilder()
                                .setConfigurationResponseCommon(
                                        new ConfigurationResponseCommonBuilder()
                                                .setAckFinalIndicator(finalAck)
                                                .setResponseMessage(message)
                                                .setResponseCode(responseCode)
                                                .setRequestId(
                                                        input.getSdncRequestHeader() == null
                                                                ? null
                                                                : input.getSdncRequestHeader().getRequestId())
                                                .build())
                                .build())
                .buildFuture();
    }

}
