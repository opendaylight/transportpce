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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Class for Mapping and Sending
 * Service Implemention requests :
 * - service implementation
 * - service delete.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class MappingAndSendingSIRequest {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MappingAndSendingSIRequest.class);
    /** Permit to call Renderer RPCs. */
    private StubrendererService service;
    /** define procedure success (or not ). */
    private Boolean success = false;
    /** permit to call bundle service (PCE, Renderer, Servicehandler. */
    private RpcProviderRegistry rpcRegistry;
    /** store all error messages. */
    private String error;
    ServiceImplementationRequestInput serviceImplementationRequestInput = null;
    ServiceDeleteInput serviceDeleteInput = null;

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));


    /**
     * MappingAndSendingSIRequest class constructor
     * for RPC serviceCreate.
     *
     * @param rpcRegistry rpcRegistry
     * @param serviceCreateInput serviceCreateInput
     * @param pathComputationOutput pathComputationOutput
     */
    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,
            ServiceCreateInput serviceCreateInput,PathComputationRequestOutput pathComputationOutput) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /** Building ServiceImplementationRequestInput  / ServiceDeleteInput serviceDeleteInput. */
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (serviceCreateInput.getSdncRequestHeader() != null) {
            serviceHandlerHeader.setRequestId(serviceCreateInput.getSdncRequestHeader().getRequestId());
        }
        mappingSIRequest(pathComputationOutput, serviceHandlerHeader, serviceCreateInput.getServiceAEnd(),
                serviceCreateInput.getServiceZEnd(), serviceCreateInput.getServiceName());
        /*

        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.create.input.ServiceAEnd tempA = serviceCreateInput.getServiceAEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortRx =
            serviceCreateInput.getServiceAEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortTx =
            serviceCreateInput.getServiceAEnd().getTxDirection().getPort();

        RxDirection rxDirectionAEnd = getRxDirection(tempAPortRx);

        TxDirection txDirectionAEnd = getTxDirection(tempAPortTx);

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setClli(tempA.getClli())
            .setNodeId(tempA.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempA.getServiceFormat().getName()))
            .setServiceRate(tempA.getServiceRate())
            .setRxDirection(rxDirectionAEnd)
            .setTxDirection(txDirectionAEnd)
            .build();

        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.create.input.ServiceZEnd tempZ = serviceCreateInput.getServiceZEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortRx =
            serviceCreateInput.getServiceZEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortTx =
            serviceCreateInput.getServiceZEnd().getTxDirection().getPort();

        RxDirection rxDirectionZEnd = getRxDirection(tempZPortRx);

        TxDirection txDirectionZEnd = getTxDirection(tempZPortTx);

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
            .setClli(tempZ.getClli())
            .setNodeId(tempZ.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempZ.getServiceFormat().getName()))
            .setServiceRate(tempZ.getServiceRate())
            .setRxDirection(rxDirectionZEnd)
            .setTxDirection(txDirectionZEnd)
            .build();

        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
            .rev170426.response.parameters.sp.response.parameters.PathDescription tmp = null;
        try {
            tmp = pathComputationOutput.getResponseParameters().getPathDescription();
        } catch (NullPointerException e) {
            LOG.error("PathDescription is null : {}", e.toString());
        }
        PathDescriptionBuilder pathDescription = new PathDescriptionBuilder();
        if (tmp != null) {
            pathDescription = new PathDescriptionBuilder(tmp);
        }
        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
        .setPathDescription(pathDescription.build())
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(serviceCreateInput.getServiceName())
        .setServiceAEnd(serviceAEnd)
        .setServiceZEnd(serviceZEnd)
        .build();*/

        mappingSIRequest(pathComputationOutput, serviceHandlerHeader, serviceCreateInput.getServiceAEnd(),
                serviceCreateInput.getServiceZEnd(), serviceCreateInput.getServiceName());
    }

    /**
     * MappingAndSendingSIRequest class constructor
     * for RPC serviceReconfigure.
     *
     * @param rpcRegistry rpcRegistry
     * @param serviceReconfigureInput serviceReconfigureInput
     * @param pathComputationOutput pathComputationOutput
     */

    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,
            ServiceReconfigureInput serviceReconfigureInput,PathComputationRequestOutput pathComputationOutput) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /** Building ServiceImplementationRequestInput  / ServiceDeleteInput serviceDeleteInput .*/
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder()
                .setRequestId("reconfigure_" + serviceReconfigureInput.getNewServiceName());
        mappingSIRequest(pathComputationOutput, serviceHandlerHeader, serviceReconfigureInput.getServiceAEnd(),
                serviceReconfigureInput.getServiceZEnd(), serviceReconfigureInput.getNewServiceName());

        /*
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.reconfigure.input.ServiceAEnd tempA = serviceReconfigureInput.getServiceAEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortRx =
            serviceReconfigureInput.getServiceAEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortTx =
            serviceReconfigureInput.getServiceAEnd().getTxDirection().getPort();

        RxDirection rxDirectionAEnd = getRxDirection(tempAPortRx);

        TxDirection txDirectionAEnd = getTxDirection(tempAPortTx);

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setClli(tempA.getClli())
            .setNodeId(tempA.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempA.getServiceFormat().getName()))
            .setServiceRate(tempA.getServiceRate())
            .setRxDirection(rxDirectionAEnd)
            .setTxDirection(txDirectionAEnd)
            .build();

        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.reconfigure.input.ServiceZEnd tempZ = serviceReconfigureInput.getServiceZEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortRx =
            serviceReconfigureInput.getServiceZEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortTx =
            serviceReconfigureInput.getServiceZEnd().getTxDirection().getPort();


        RxDirection rxDirectionZEnd = getRxDirection(tempZPortRx);
        TxDirection txDirectionZEnd = getTxDirection(tempZPortTx);

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
            .setClli(tempZ.getClli())
            .setNodeId(tempZ.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempZ.getServiceFormat().getName()))
            .setServiceRate(tempZ.getServiceRate())
            .setRxDirection(rxDirectionZEnd)
            .setTxDirection(txDirectionZEnd)
            .build();

        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
            .rev170426.response.parameters.sp.response.parameters.PathDescription tmp = null;
        try {
            tmp = pathComputationOutput.getResponseParameters().getPathDescription();
        } catch (NullPointerException e) {
            LOG.error("PathDescription is null : {}", e.toString());
        }
        PathDescriptionBuilder pathDescription = new PathDescriptionBuilder();
        if (tmp != null) {
            pathDescription = new PathDescriptionBuilder(tmp);
        }
        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
        .setPathDescription(pathDescription.build())
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(serviceReconfigureInput.getNewServiceName())
        .setServiceAEnd(serviceAEnd)
        .setServiceZEnd(serviceZEnd)
        .build();
        LOG.info("ServiceImplementationRequestInput : {}", serviceImplementationRequestInput.toString());*/
    }


    /**
     * MappingAndSendingSIRequest class constructor
     * for RPC serviceDelete.
     *
     * @param rpcRegistry RpcProviderRegistry
     * @param requestId Request ID
     * @param serviceName Service name
     */
    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,String requestId, String serviceName) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /** ServiceDeleteInput Build .*/
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (requestId != null) {
            serviceHandlerHeader.setRequestId(requestId);
        }

        serviceDeleteInput = new ServiceDeleteInputBuilder()
                .setServiceHandlerHeader(serviceHandlerHeader.build())
                .setServiceName(serviceName)
                .build();
    }

    /**
     * MappingAndSendingSIRequest Class constructor
     * for modify Service in ODL Datastore.
     *
     * @param rpcRegistry RpcProviderRegistry
     * @param services Services
     * @param pathComputationOutput PathComputationRequestOutput
     */
    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,Services services,
            PathComputationRequestOutput pathComputationOutput) {
        this.setRpcRegistry(rpcRegistry);
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /** Building ServiceImplementationRequestInput  / ServiceDeleteInput serviceDeleteInput .*/
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        SdncRequestHeader sdnc = services.getSdncRequestHeader();
        if (sdnc != null) {
            String requestId = services.getSdncRequestHeader().getRequestId();
            if (requestId != null) {
                serviceHandlerHeader.setRequestId(requestId);
            }
        }
        mappingSIRequest(pathComputationOutput, serviceHandlerHeader, services.getServiceAEnd(),
                services.getServiceZEnd(), services.getServiceName());
        /*org.opendaylight.yang.gen.v1.http.org.openroadm.common.service
            .types.rev161014.service.ServiceAEnd tempA = services.getServiceAEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortRx = tempA.getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortTx = tempA.getTxDirection().getPort();


        RxDirection rxDirectionAEnd = getRxDirection(tempAPortRx);

        TxDirection txDirectionAEnd = getTxDirection(tempAPortTx);

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setClli(tempA.getClli())
            .setNodeId(tempA.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempA.getServiceFormat().getName()))
            .setServiceRate(tempA.getServiceRate())
            .setRxDirection(rxDirectionAEnd)
            .setTxDirection(txDirectionAEnd)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service
            .types.rev161014.service.ServiceZEnd tempZ = services.getServiceZEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortRx = tempZ.getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortTx = tempZ.getTxDirection().getPort();

        RxDirection rxDirectionZEnd = getRxDirection(tempZPortRx);
        TxDirection txDirectionZEnd = getTxDirection(tempZPortTx);

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
            .setClli(tempZ.getClli())
            .setNodeId(tempZ.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempZ.getServiceFormat().getName()))
            .setServiceRate(tempZ.getServiceRate())
            .setRxDirection(rxDirectionZEnd)
            .setTxDirection(txDirectionZEnd)
            .build();
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
        .rev170426.response.parameters.sp.response.parameters.PathDescription tmp = null;
        try {
            tmp = pathComputationOutput.getResponseParameters().getPathDescription();
        } catch (NullPointerException e) {
            LOG.error("PathDescription is null : {}", e);
        }
        PathDescriptionBuilder pathDescription = new PathDescriptionBuilder();
        if (tmp != null) {
            pathDescription = new PathDescriptionBuilder(tmp);
        }

        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
        .setPathDescription(pathDescription.build())
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(services.getServiceName())
        .setServiceAEnd(serviceAEnd)
        .setServiceZEnd(serviceZEnd)
        .build();
        LOG.info("ServiceImplementationRequestInput : {}", serviceImplementationRequestInput.toString());*/
    }

    /**
     *Build serviceImplementationRequestInput with
     *input parameters from ServiceCreateInput or
     *Services or serviceReconfigureInput.
     *
     * @param pathComputationOutput PathComputationRequestOutput
     * @param serviceHandlerHeader ServiceHandlerHeaderBuilder
     * @param aend Beginning ServiceEndpoint
     * @param zend Ending ServiceEndpoint
     * @param serviceName Service Name
     */
    private void mappingSIRequest(PathComputationRequestOutput pathComputationOutput,
            ServiceHandlerHeaderBuilder serviceHandlerHeader, org.opendaylight.yang.gen .v1.http.org.openroadm.common
            .service.types.rev161014.ServiceEndpoint aend , org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.ServiceEndpoint zend, String serviceName) {
        LOG.info("Mapping ServiceCreateInput or Services or serviceReconfigureInput to SIR requests");
        /** ServiceAEnd Build. */
        RxDirection rxDirectionAEnd = new RxDirectionBuilder()
                .setPort(aend.getRxDirection().getPort())
                .build();
        TxDirection txDirectionAEnd = new TxDirectionBuilder()
                .setPort(aend.getTxDirection().getPort())
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .setClli(aend.getClli())
                .setNodeId(aend.getNodeId())
                .setServiceFormat(ServiceFormat.valueOf(aend.getServiceFormat().getName()))
                .setServiceRate(aend.getServiceRate())
                .setRxDirection(rxDirectionAEnd)
                .setTxDirection(txDirectionAEnd)
                .build();

        /** ServiceZEnd Build. */
        RxDirection rxDirectionZEnd = new RxDirectionBuilder()
                .setPort(zend.getRxDirection().getPort())
                .build();

        TxDirection txDirectionZEnd = new TxDirectionBuilder()
                .setPort(zend.getTxDirection().getPort())
                .build();

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .setClli(zend.getClli())
                .setNodeId(zend.getNodeId())
                .setServiceFormat(ServiceFormat.valueOf(zend.getServiceFormat().getName()))
                .setServiceRate(zend.getServiceRate())
                .setRxDirection(rxDirectionZEnd)
                .setTxDirection(txDirectionZEnd)
                .build();


        /** ServiceImplementationRequestInput  Build. */
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
            .rev170426.response.parameters.sp.response.parameters.PathDescription tmp = null;
        try {
            tmp = pathComputationOutput.getResponseParameters().getPathDescription();
        } catch (NullPointerException e) {
            LOG.error("PathDescription is null : {}", e.toString());
        }
        PathDescriptionBuilder pathDescription = new PathDescriptionBuilder();
        if (tmp != null) {
            pathDescription = new PathDescriptionBuilder(tmp);
        }
        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
                .setPathDescription(pathDescription.build())
                .setServiceHandlerHeader(serviceHandlerHeader.build())
                .setServiceName(serviceName)
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .build();

    }

    /**
     * Send serviceImplementation request to Render.
     *
     * @return ServiceImplementationRequestOutput data response from Renderer
     */
    public ListenableFuture<Boolean> serviceImplementation() {
        setSuccess(false);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = null;
                if (serviceImplementationRequestInput != null) {
                    RpcResult<ServiceImplementationRequestOutput> rendererOutputResult = null;
                    Future<RpcResult<ServiceImplementationRequestOutput>> rendererOutputFuture =
                            service.serviceImplementationRequest(serviceImplementationRequestInput);
                    try {
                        rendererOutputResult = rendererOutputFuture.get();//wait to get  the result
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from renderer to pathComputationRequest RPC "
                                + e.toString());
                        LOG.error(error);
                        rendererOutputFuture.cancel(true);
                    }

                    if (rendererOutputResult != null && rendererOutputResult.isSuccessful()) {
                        LOG.info("Renderer replied to serviceImplementation Request !");
                        setSuccess(true);
                        output = true;
                        setSuccess(true);
                    }
                } else {
                    LOG.info("serviceImplementationRequestInput is not valid");
                }

                return output;
            }
        });

    }

    /**
     * Send serviceDelete request to Render.
     *
     * @return ServiceDeleteOutput data response from Renderer
     */
    public ListenableFuture<Boolean> serviceDelete() {
        setSuccess(false);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = null;
                if (serviceDeleteInput != null) {
                    RpcResult<ServiceDeleteOutput> rendererOutputResult = null;
                    Future<RpcResult<ServiceDeleteOutput>> rendererOutputFuture =
                            service.serviceDelete(serviceDeleteInput);
                    try {
                        rendererOutputResult = rendererOutputFuture.get();//wait to get  the result
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from renderer to pathComputationRequest RPC "
                                + e.toString());
                        LOG.error(error);
                        rendererOutputFuture.cancel(true);
                    }

                    if (rendererOutputResult != null && rendererOutputResult.isSuccessful()) {
                        LOG.info("Renderer replied to serviceDelete Request!");
                        setSuccess(true);
                        output = true;
                    }
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

}
