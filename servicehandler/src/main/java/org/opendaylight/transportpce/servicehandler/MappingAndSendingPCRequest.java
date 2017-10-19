/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for Mapping and Sending PCE requests : - path-computation-request -
 * cancel-resource-reserve.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on behalf of Orange
 *
 */
public class MappingAndSendingPCRequest {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MappingAndSendingPCRequest.class);
    /** Permit to call PCE RPCs. */
    private StubpceService service;
    /** define procedure success (or not ). */
    private Boolean success;
    /** permit to call bundle service (PCE, Renderer, Servicehandler. */
    private RpcProviderRegistry rpcRegistry;
    PathComputationRequestInput pathComputationRequestInput = null;
    CancelResourceReserveInput cancelResourceReserveInput = null;
    HardConstraints hard = null;
    SoftConstraints soft = null;
    private ServiceAEnd serviceAEndSp;
    private ServiceZEnd serviceZEndSp;
    /** store all error messages. */
    private String error;
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));

    /**
     * MappingAndSendingPCRequest Class constructor for RPC serviceReconfigure.
     *
     * @param rpcRegistry
     *            RpcProviderRegistry
     * @param serviceReconfigureInput
     *            serviceReconfigureInput
     * @param resvResource
     *            Boolean to define resource reserve
     */
    public MappingAndSendingPCRequest(RpcProviderRegistry rpcRegistry, ServiceReconfigureInput serviceReconfigureInput,
            Boolean resvResource) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        endpointToEndpointStubpce(serviceReconfigureInput.getServiceAEnd(), serviceReconfigureInput.getServiceZEnd());
        SdncRequestHeader head = new SdncRequestHeaderBuilder()
                .setRequestId("reconfigure_" + serviceReconfigureInput.getServiceName())
                .setRpcAction(RpcActions.ServiceReconfigure)
                .build();
        MappingConstraints map = new MappingConstraints(serviceReconfigureInput.getHardConstraints(),
                serviceReconfigureInput.getSoftConstraints());
        map.serviceToServicePathConstarints();
        /**
         * mappingPCRequest(serviceReconfigureInput.getNewServiceName(),
         * serviceReconfigureInput.getHardConstraints(),
         * serviceReconfigureInput.getSoftConstraints(),head ,resvResource);
         */
        mappingPCRequest(serviceReconfigureInput.getNewServiceName(),serviceAEndSp, serviceZEndSp,
                map.getServicePathHardConstraints(), map.getServicePathSoftConstraints(), head, resvResource);
        setSuccess(false);
        setError("");
    }

    /**
     * MappingAndSendingPCRequest Class constructor for RPC
     * serviceFeasibilityCheck.
     *
     * @param rpcRegistry
     *            RpcProviderRegistry
     * @param serviceFeasibilityCheckInput
     *            ServiceFeasibilityCheckInput
     * @param resvResource
     *            Boolean to reserve resource
     */
    public MappingAndSendingPCRequest(RpcProviderRegistry rpcRegistry,
            ServiceFeasibilityCheckInput serviceFeasibilityCheckInput, Boolean resvResource) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        endpointToEndpointStubpce(serviceFeasibilityCheckInput.getServiceAEnd(),
                serviceFeasibilityCheckInput.getServiceZEnd());
        MappingConstraints map = new MappingConstraints(serviceFeasibilityCheckInput.getHardConstraints(),
                serviceFeasibilityCheckInput.getSoftConstraints());
        map.serviceToServicePathConstarints();
        mappingPCRequest("no name",serviceAEndSp, serviceZEndSp, map.getServicePathHardConstraints(),
                map.getServicePathSoftConstraints(), serviceFeasibilityCheckInput.getSdncRequestHeader(),
                resvResource);
        setSuccess(false);
        setError("");
    }

    /**
     * MappingAndSendingPCRequest Class constructor for RPC serviceCreate.
     *
     * @param rpcRegistry
     *            RpcProviderRegistry
     * @param serviceCreateInput
     *            ServiceCreateInput
     * @param resvResource
     *            Boolean to reserve resource
     */
    public MappingAndSendingPCRequest(RpcProviderRegistry rpcRegistry, ServiceCreateInput serviceCreateInput,
            Boolean resvResource) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        endpointToEndpointStubpce(serviceCreateInput.getServiceAEnd(), serviceCreateInput.getServiceZEnd());
        MappingConstraints map = new MappingConstraints(serviceCreateInput.getHardConstraints(),
                serviceCreateInput.getSoftConstraints());
        map.serviceToServicePathConstarints();
        mappingPCRequest(serviceCreateInput.getServiceName(),serviceAEndSp, serviceZEndSp,
                map.getServicePathHardConstraints(), map.getServicePathSoftConstraints(),
                serviceCreateInput.getSdncRequestHeader(), resvResource);
        setSuccess(false);
        setError("");
    }

    /**
     * MappingAndSendingPCRequest Class constructor for modify Service in ODL
     * Datastore.
     *
     * @param rpcRegistry
     *            RpcProviderRegistry
     * @param input
     *            Services
     * @param resvResource
     *            Boolean to reserve resource
     */
    public MappingAndSendingPCRequest(RpcProviderRegistry rpcRegistry, Services input, Boolean resvResource) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        endpointToEndpointStubpce(input.getServiceAEnd(), input.getServiceZEnd());
        MappingConstraints map = new MappingConstraints(input.getHardConstraints(), input.getSoftConstraints());
        map.serviceToServicePathConstarints();
        mappingPCRequest(input.getServiceName(),serviceAEndSp, serviceZEndSp, map.getServicePathHardConstraints(),
                map.getServicePathSoftConstraints(), input.getSdncRequestHeader(), resvResource);
        setSuccess(false);
        setError("");
    }


    public void endpointToEndpointStubpce(ServiceEndpoint serviceAEnd, ServiceEndpoint serviceZEnd) {
        LOG.info("Mapping Service Endpoint to Service Endpoint Stubpce");
        TxDirection txDirection = null;
        RxDirection rxDirection = null;
        if (serviceAEnd != null && serviceZEnd != null) {
            txDirection = new TxDirectionBuilder()
                    .setPort(serviceAEnd.getTxDirection().getPort())
                    .build();
            rxDirection = new RxDirectionBuilder()
                    .setPort(serviceAEnd.getRxDirection().getPort())
                    .build();
            serviceAEndSp = new ServiceAEndBuilder()
                    .setClli(serviceAEnd.getClli())
                    .setNodeId(serviceAEnd.getNodeId())
                    .setServiceFormat(serviceAEnd.getServiceFormat())
                    .setServiceRate(serviceAEnd.getServiceRate())
                    .setTxDirection(txDirection)
                    .setRxDirection(rxDirection)
                    .build();

            txDirection = new TxDirectionBuilder()
                    .setPort(serviceZEnd.getTxDirection().getPort())
                    .build();
            rxDirection = new RxDirectionBuilder()
                    .setPort(serviceZEnd.getRxDirection().getPort())
                    .build();
            serviceZEndSp = new ServiceZEndBuilder()
                    .setClli(serviceZEnd.getClli())
                    .setNodeId(serviceZEnd.getNodeId())
                    .setServiceFormat(serviceZEnd.getServiceFormat())
                    .setServiceRate(serviceZEnd.getServiceRate())
                    .setTxDirection(txDirection)
                    .setRxDirection(rxDirection)
                    .build();
        }
    }

    /**
     * Build pathComputationRequestInput or cancelResourceReserveInput with
     * input parameters (serviceReconfigureInput or serviceFeasibilityCheckInput.
     *
     * @param String
     *            serviceName
     * @param zend Service ZEnd
     * @param aend Service AEnd
     * @param HardConstraints
     *            hardConstraints
     * @param SoftConstraints
     *            softConstraints
     * @param SdncRequestHeader
     *            sdncRequestHeader
     * @param Boolean
     *            resvResource
     */
    private void mappingPCRequest(String serviceName, ServiceAEnd aend, ServiceZEnd zend,
            HardConstraints hardConstraints, SoftConstraints softConstraints, org.opendaylight.yang.gen.v1.http.org
            .openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader sdncRequestHeader,
            Boolean resvResource) {

        LOG.info("Mapping ServiceCreateInput or ServiceFeasibilityCheckInput or serviceReconfigureInput "
                + "to PCE requests");

        HardConstraints serviceCreateHard = hardConstraints;
        SoftConstraints serviceCreateSoft = softConstraints;
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing
            .constraints.sp.HardConstraints pceHardConstraints = null;
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing
            .constraints.sp.SoftConstraints pceSoftConstraints = null;
        if (serviceCreateHard != null) {
            pceHardConstraints = serviceCreateHard;
        }
        if (serviceCreateSoft != null) {
            pceSoftConstraints = serviceCreateSoft;
        }

        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (sdncRequestHeader != null) {
            serviceHandlerHeader.setRequestId(sdncRequestHeader.getRequestId());
        }

        /** PathComputationRequestInput build */
        pathComputationRequestInput = new PathComputationRequestInputBuilder()
                .setServiceName(serviceName)
                .setResourceReserve(resvResource)
                .setServiceHandlerHeader(serviceHandlerHeader.build())
                .setServiceAEnd(aend)
                .setServiceZEnd(zend)
                .setHardConstraints(pceHardConstraints)
                .setSoftConstraints(pceSoftConstraints)
                .setPceMetric(PceMetric.TEMetric).build();

        /** CancelResourceReserveInput build */
        cancelResourceReserveInput = new CancelResourceReserveInputBuilder().setServiceName(serviceName)
                .setServiceHandlerHeader(serviceHandlerHeader.build()).build();
    }


    /**
     * Send cancelResourceReserve request to PCE.
     *
     * @return Boolean true if success, false else
     */
    public ListenableFuture<Boolean> cancelResourceReserve() {
        setSuccess(false);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = false;
                if (cancelResourceReserveInput != null) {
                    RpcResult<CancelResourceReserveOutput> pceOutputResult = null;
                    Future<RpcResult<CancelResourceReserveOutput>> pceOutputFuture = service
                            .cancelResourceReserve(cancelResourceReserveInput);
                    try {
                        pceOutputResult = pceOutputFuture.get();
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from pce to cancelResourceReserve RPC "
                                + e.toString());
                        LOG.error(error);
                        pceOutputFuture.cancel(true);
                    }

                    if (pceOutputResult != null && pceOutputResult.isSuccessful()) {
                        LOG.info("PCE replied to CancelResource request !");
                        output = true;
                        setSuccess(true);
                    }
                } else {
                    LOG.info("cancelResourceReserveInput parameter not valid !");
                }
                return output;
            }
        });

    }

    /**
     * Send pathComputationRequest request to PCE.
     *
     * @return Boolean true if success, false else
     */
    public ListenableFuture<Boolean> pathComputationRequest() {
        LOG.info("In pathComputationRequest ...");
        setSuccess(false);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                RpcResult<PathComputationRequestOutput> pceOutputResult = null;
                Boolean output = false;
                if (pathComputationRequestInput != null) {
                    LOG.info("pathComputationRequestInput : " + pathComputationRequestInput.toString());
                    Future<RpcResult<PathComputationRequestOutput>> pceOutputFuture = service
                            .pathComputationRequest(pathComputationRequestInput);
                    try {
                        pceOutputResult = pceOutputFuture.get();
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from pce to pathComputationRequest RPC "
                                + e.toString());
                        LOG.error(error);
                        pceOutputFuture.cancel(true);
                    }
                    if (pceOutputResult != null && pceOutputResult.isSuccessful()) {
                        setSuccess(true);
                        output = true;
                        LOG.info("PCE replied to pathComputation request !");
                    }
                } else {
                    LOG.info("pathComputationRequestInput parameter not valid !");
                }
                return output;
            }
        });
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public RpcProviderRegistry getRpcRegistry() {
        return rpcRegistry;
    }

    public void setRpcRegistry(RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
    }

    public ServiceAEnd getServiceAEndSp() {
        return serviceAEndSp;
    }

    public void setServiceAEndSp(ServiceAEnd serviceAEndSp) {
        this.serviceAEndSp = serviceAEndSp;
    }

    public ServiceZEnd getServiceZEndSp() {
        return serviceZEndSp;
    }

    public void setServiceZEndSp(ServiceZEnd serviceZEndSp) {
        this.serviceZEndSp = serviceZEndSp;
    }
}
