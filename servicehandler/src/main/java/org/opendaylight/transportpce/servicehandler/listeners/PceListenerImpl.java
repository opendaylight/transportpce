/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceListenerImpl implements TransportpcePceListener {

    private static final Logger LOG = LoggerFactory.getLogger(PceListenerImpl.class);

    private ServicePathRpcResult servicePathRpcResult;
    private RendererServiceOperations rendererServiceOperations;
    private OtnDeviceRendererService otnDeviceRendererService;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private ServiceInput input;
    private Boolean serviceReconfigure;
    private Boolean tempService;
    private Boolean serviceFeasiblity;

    public PceListenerImpl(RendererServiceOperations rendererServiceOperations,
                           PathComputationService pathComputationService,
                           NotificationPublishService notificationPublishService,
                           ServiceDataStoreOperations serviceDataStoreOperations,
                           OtnDeviceRendererService otnDeviceRendererService) {
        this.rendererServiceOperations = rendererServiceOperations;
        this.otnDeviceRendererService = otnDeviceRendererService;
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        setServiceReconfigure(false);
        setInput(null);
        setTempService(false);
        setServiceFeasiblity(false);
    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (!compareServicePathRpcResult(notification)) {
            servicePathRpcResult = notification;
            PathDescription pathDescription = null;
            switch (servicePathRpcResult.getNotificationType().getIntValue()) {
                /* path-computation-request. */
                case 1:
                    LOG.info("PCE '{}' Notification received : {}",servicePathRpcResult.getNotificationType().getName(),
                            notification);
                    if (servicePathRpcResult.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("PCE calculation done OK !");
                        if (servicePathRpcResult.getPathDescription() != null) {
                            pathDescription = new PathDescriptionBuilder()
                                    .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
                                .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection()).build();
                            LOG.info("PathDescription gets : {}", pathDescription);
                            if (!serviceFeasiblity) {
                                if (input == null) {
                                    LOG.error("Input is null !");
                                    return;
                                }
                                OperationResult operationResult = null;
                                if (tempService) {
                                    operationResult = this.serviceDataStoreOperations
                                        .createTempService(input.getTempServiceCreateInput());
                                    if (!operationResult.isSuccess()) {
                                        LOG.error("Temp Service not created in datastore !");
                                    }
                                } else {
                                    operationResult = this.serviceDataStoreOperations
                                        .createService(input.getServiceCreateInput());
                                    if (!operationResult.isSuccess()) {
                                        LOG.error("Service not created in datastore !");
                                    }
                                }
                                ResponseParameters responseParameters = new ResponseParametersBuilder()
                                        .setPathDescription(new org.opendaylight.yang.gen.v1.http.org
                                                .transportpce.b.c._interface.service.types.rev200128
                                                .response.parameters.sp.response.parameters
                                                .PathDescriptionBuilder(pathDescription).build())
                                        .build();
                                PathComputationRequestOutput pceResponse = new PathComputationRequestOutputBuilder()
                                        .setResponseParameters(responseParameters).build();
                                OperationResult operationServicePathSaveResult =
                                        this.serviceDataStoreOperations.createServicePath(input, pceResponse);
                                if (!operationServicePathSaveResult.isSuccess()) {
                                    LOG.error("Service Path not created in datastore !");
                                }

                                switch (input.getConnectionType()) {
                                    case Service:
                                        // TODO: Use if state to check the service-rate and differenciate between
                                        // Use the service-rate, otu-service-rate and odu-service-rate
                                        if (input.getServiceAEnd().getServiceRate().equals(100)) {
                                            ServiceImplementationRequestInput serviceImplementationRequest =
                                                ModelMappingUtils
                                                    .createServiceImplementationRequest(input, pathDescription);
                                            LOG.info("Sending serviceImplementation request : {}",
                                                serviceImplementationRequest);
                                            this.rendererServiceOperations
                                                .serviceImplementation(serviceImplementationRequest);
                                            // TODO: If the node-type is switch, then we need otn-service-path?
                                            // If it is on OTN, then use otn-service-path rpc
                                        }
                                        // For 1G and 10G service
                                        if (input.getServiceAEnd().getServiceRate().equals(10)
                                                || input.getServiceAEnd().getServiceRate().equals(1)) {
                                            LOG.info("Creating a ODU service");
                                            // TODO: Check the available BW on the WDM channel
                                            // If available, then create use the same o.w create a new
                                            ServiceImplementationRequestInput serviceImplementationRequest =
                                                ModelMappingUtils
                                                    .createServiceImplementationRequest(input, pathDescription);
                                            LOG.info("Sending serviceImplementation request : {}",
                                                serviceImplementationRequest);
                                            this.rendererServiceOperations
                                                .serviceImplementation(serviceImplementationRequest);


                                            OtnServicePathInput otnServicePathInput = ModelMappingUtils
                                                .createOtnServicePathInput(input, pathDescription);

                                            LOG.info("Sending the OTN service implementation request: {}",
                                                otnServicePathInput);
                                            this.otnDeviceRendererService.setupOtnServicePath(otnServicePathInput);

                                        }
                                        break;
                                    case Infrastructure:
                                        LOG.info("This is for OTU4 service {}", input.getConnectionType());
                                        // TODO: We should also create an ODU4 interfaces
                                        ServiceImplementationRequestInput serviceImplementationRequest =
                                            ModelMappingUtils
                                                .createServiceImplementationRequest(input, pathDescription);
                                        LOG.info("Sending serviceImplementation request : {}",
                                            serviceImplementationRequest);
                                        this.rendererServiceOperations
                                            .serviceImplementation(serviceImplementationRequest);
                                        break;
                                    default:
                                        LOG.warn("unsupported connection type {}", input.getConnectionType());
                                        //TODO maybe more things to do here
                                }
                            } else {
                                LOG.warn("service-feasibility-check RPC ");
                            }
                        } else {
                            LOG.error("'PathDescription' parameter is null ");
                            return;
                        }
                    } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
                        LOG.error("PCE path computation failed !");
                        return;
                    }
                    break;
                /* cancel-resource-reserve. */
                case 2:
                    if (servicePathRpcResult.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("PCE cancel resource done OK !");
                        OperationResult deleteServicePathOperationResult =
                                this.serviceDataStoreOperations.deleteServicePath(input.getServiceName());
                        if (!deleteServicePathOperationResult.isSuccess()) {
                            LOG.warn("Service path was not removed from datastore!");
                        }
                        OperationResult deleteServiceOperationResult = null;
                        if (tempService) {
                            deleteServiceOperationResult =
                                    this.serviceDataStoreOperations.deleteTempService(input.getServiceName());
                            if (!deleteServiceOperationResult.isSuccess()) {
                                LOG.warn("Service was not removed from datastore!");
                            }
                        } else {
                            deleteServiceOperationResult =
                                    this.serviceDataStoreOperations.deleteService(input.getServiceName());
                            if (!deleteServiceOperationResult.isSuccess()) {
                                LOG.warn("Service was not removed from datastore!");
                            }
                        }
                        /**
                         * if it was an RPC serviceReconfigure, re-launch PCR.
                         */
                        if (this.serviceReconfigure) {
                            LOG.info("cancel resource reserve done, relaunching PCE path computation ...");
                            this.pceServiceWrapper.performPCE(input.getServiceCreateInput(), true);
                            this.serviceReconfigure = false;
                        }
                    } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
                        LOG.info("PCE cancel resource failed !");
                    }
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("ServicePathRpcResult already wired !");
        }
    }

    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        Boolean result = true;
        if (servicePathRpcResult == null) {
            result = false;
        } else {
            if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
                result = false;
            }
            if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
                result = false;
            }
            if (servicePathRpcResult.getStatus() != notification.getStatus()) {
                result = false;
            }
            if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
                result = false;
            }
        }
        return result;
    }

    public void setInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    public void setServiceReconfigure(Boolean serv) {
        this.serviceReconfigure = serv;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
    }

    public void setServiceFeasiblity(Boolean serviceFeasiblity) {
        this.serviceFeasiblity = serviceFeasiblity;
    }

}
