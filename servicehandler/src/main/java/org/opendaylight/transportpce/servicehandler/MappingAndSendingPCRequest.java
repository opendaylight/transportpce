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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * class for Mapping and Sending PCE requests : - path-computation-request -
 * cancel-resource-reserve.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class MappingAndSendingPCRequest {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MappingAndSendingPCRequest.class);
    /* Permit to call PCE RPCs. */
    private StubpceService service;
    /* define procedure success (or not ). */
    private Boolean success;
    /* permit to call bundle service (PCE, Renderer, Servicehandler. */
    private RpcProviderRegistry rpcRegistry;
    PathComputationRequestInput pathComputationRequestInput = null;
    CancelResourceReserveInput cancelResourceReserveInput = null;
    HardConstraints hard = null;
    SoftConstraints soft = null;
    /* store all error messages. */
    private String error;
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));

    /*
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
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        SdncRequestHeader head = new SdncRequestHeaderBuilder().build();
        MappingConstraints map = new MappingConstraints(serviceReconfigureInput.getHardConstraints(),
                serviceReconfigureInput.getSoftConstraints());
        map.serviceToServicePathConstarints();
        /*
         * mappingPCRequest(serviceReconfigureInput.getNewServiceName(),
         * serviceReconfigureInput.getHardConstraints(),
         * serviceReconfigureInput.getSoftConstraints(),head ,resvResource);
         */
        mappingPCRequest(serviceReconfigureInput.getNewServiceName(), map.getServicePathHardConstraints(),
                map.getServicePathSoftConstraints(), head, resvResource);
        setSuccess(false);
        setError("");
    }

    /*
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
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        MappingConstraints map = new MappingConstraints(serviceFeasibilityCheckInput.getHardConstraints(),
                serviceFeasibilityCheckInput.getSoftConstraints());
        map.serviceToServicePathConstarints();
        mappingPCRequest("no name", map.getServicePathHardConstraints(), map.getServicePathSoftConstraints(),
                serviceFeasibilityCheckInput.getSdncRequestHeader(), resvResource);
        setSuccess(false);
        setError("");
    }

    /*
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
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        MappingConstraints map = new MappingConstraints(serviceCreateInput.getHardConstraints(),
                serviceCreateInput.getSoftConstraints());
        map.serviceToServicePathConstarints();
        mappingPCRequest(serviceCreateInput.getServiceName(), map.getServicePathHardConstraints(),
                map.getServicePathSoftConstraints(), serviceCreateInput.getSdncRequestHeader(), resvResource);
        setSuccess(false);
        setError("");
    }

    /*
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
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubpceService.class);
        }
        MappingConstraints map = new MappingConstraints(input.getHardConstraints(), input.getSoftConstraints());
        map.serviceToServicePathConstarints();
        mappingPCRequest(input.getServiceName(), map.getServicePathHardConstraints(),
                map.getServicePathSoftConstraints(), input.getSdncRequestHeader(), resvResource);
        setSuccess(false);
        setError("");
    }

    /*
     * Build pathComputationRequestInput or cancelResourceReserveInput with
     * input parameters (serviceReconfigureInput or serviceFeasibilityCheckInput.
     *
     * @param String
     *            serviceName
     * @param HardConstraints
     *            hardConstraints
     * @param SoftConstraints
     *            softConstraints
     * @param SdncRequestHeader
     *            sdncRequestHeader
     * @param Boolean
     *            resvResource
     */
    private void mappingPCRequest(String serviceName, HardConstraints hardConstraints, SoftConstraints softConstraints,
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header
            .SdncRequestHeader sdncRequestHeader, Boolean resvResource) {

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

        /* PathComputationRequestInput build */
        pathComputationRequestInput = new PathComputationRequestInputBuilder().setServiceName(serviceName)
                .setResourceReserve(resvResource).setServiceHandlerHeader(serviceHandlerHeader.build())
                .setHardConstraints(pceHardConstraints).setSoftConstraints(pceSoftConstraints)
                .setPceMetric(PceMetric.TEMetric).build();

        /* CancelResourceReserveInput build */
        cancelResourceReserveInput = new CancelResourceReserveInputBuilder().setServiceName(serviceName)
                .setServiceHandlerHeader(serviceHandlerHeader.build()).build();
    }

    /* Send cancelResourceReserve request to PCE.
     *
     * @return CancelResourceReserveOutput data response from PCE
     */
    public ListenableFuture<CancelResourceReserveOutput> cancelResourceReserve() {
        setSuccess(false);
        return executor.submit(new Callable<CancelResourceReserveOutput>() {

            @Override
            public CancelResourceReserveOutput call() throws Exception {
                CancelResourceReserveOutput output = null;
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
                        CancelResourceReserveOutput pceOutput = pceOutputResult.getResult();
                        output = new CancelResourceReserveOutputBuilder()
                                .setConfigurationResponseCommon(pceOutput.getConfigurationResponseCommon()).build();
                        setSuccess(true);
                    }
                } else {
                    LOG.info("cancelResourceReserveInput parameter not valid !");
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

    /*Send pathComputationRequest request to PCE.
     *
     * @return PathComputationRequestOutput data response from PCE
     */
    public ListenableFuture<PathComputationRequestOutput> pathComputationRequest() {
        LOG.info("In pathComputationRequest ...");
        setSuccess(false);
        return executor.submit(new Callable<PathComputationRequestOutput>() {
            @Override
            public PathComputationRequestOutput call() throws Exception {
                RpcResult<PathComputationRequestOutput> pceOutputResult = null;
                PathComputationRequestOutput output = null;
                if (pathComputationRequestInput != null) {
                    Future<RpcResult<PathComputationRequestOutput>> pceOutputFuture = service
                            .pathComputationRequest(pathComputationRequestInput);
                    try {
                        pceOutputResult = pceOutputFuture.get();// wait to get
                                                                // the result
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from pce to pathComputationRequest RPC "
                                + e.toString());
                        LOG.error(error);
                        pceOutputFuture.cancel(true);
                    }

                    if (pceOutputResult != null && pceOutputResult.isSuccessful()) {
                        setSuccess(true);
                        LOG.info("PCE replied to pathComputation request !");
                        PathComputationRequestOutput pceOutput = pceOutputResult.getResult();
                        output = new PathComputationRequestOutputBuilder()
                                .setConfigurationResponseCommon(pceOutput.getConfigurationResponseCommon())
                                .setResponseParameters(pceOutput.getResponseParameters()).build();
                    }
                } else {
                    LOG.info("pathComputationRequestInput parameter not valid !");
                }
                return output;
            }
        });

    }
}
