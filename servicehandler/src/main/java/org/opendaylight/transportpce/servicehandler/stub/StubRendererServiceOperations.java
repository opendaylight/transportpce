/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.stub;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.service.rpc.result.sp.PathTopology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.service.rpc.result.sp.PathTopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubRendererServiceOperations implements RendererServiceOperations {
    private static final Logger LOG = LoggerFactory.getLogger(StubRendererServiceOperations.class);
    private final ListeningExecutorService executor;
    private final NotificationPublishService notificationPublishService;
    private final NetworkModelWavelengthService networkModelWavelengthService;
    private final DataBroker dataBroker;
    private Boolean rendererFailed;
    private Boolean isnetworkModelWlService;

    public StubRendererServiceOperations(NetworkModelWavelengthService networkModelWavelengthService,
            DataBroker dataBroker, NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
        this.networkModelWavelengthService = networkModelWavelengthService;
        this.dataBroker = dataBroker;
        this.rendererFailed = false;
        this.isnetworkModelWlService = true;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    }

    private void sendNotifications(Notification notif) {
        try {
            LOG.info("putting notification : {}", notif);
            notificationPublishService.putNotification(notif);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : ", e.getMessage());
        }
    }

    @Override
    public ListenableFuture<ServiceImplementationRequestOutput>
            serviceImplementation(ServiceImplementationRequestInput input) {
        return executor.submit(new Callable<ServiceImplementationRequestOutput>() {
            @Override
            public ServiceImplementationRequestOutput call() {
                LOG.info("serviceImplementation request ...");
                String serviceName = input.getServiceName();
                RpcStatusEx rpcStatusEx = RpcStatusEx.Pending;
                ServiceRpcResultSp notification = new ServiceRpcResultSpBuilder()
                        .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                        .setServiceName(serviceName).setStatus(rpcStatusEx)
                        .setStatusMessage("Service compliant, submitting serviceImplementation Request ...").build();
                sendNotifications(notification);
                String message = "";
                String responseCode = null;
                ConfigurationResponseCommonBuilder configurationResponseCommon = null;
                ServiceImplementationRequestOutput output = null;
                try {
                    LOG.info("Wait for 5s til beginning the Renderer serviceImplementation request");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    message = "renderer failed !";
                    rpcStatusEx = RpcStatusEx.Failed;
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                    LOG.error(message);
                    notification = new ServiceRpcResultSpBuilder()
                            .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                            .setServiceName(serviceName).setStatus(rpcStatusEx).setStatusMessage(message).build();
                    sendNotifications(notification);
                    configurationResponseCommon =
                            new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                    .setResponseCode(responseCode).setResponseMessage(message);
                    output = new ServiceImplementationRequestOutputBuilder()
                            .setConfigurationResponseCommon(configurationResponseCommon.build()).build();
                    return output;
                }
                if (rendererFailed) {
                    LOG.info("forcing renderer to fail");
                    message = "renderer failed !";
                    rpcStatusEx = RpcStatusEx.Failed;
                    LOG.error(message);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                } else {
                    if (isnetworkModelWlService) {
                        networkModelWavelengthService.useWavelengths(input.getPathDescription());
                    } else {
                        LOG.warn("No need to execute networkModelWavelengthService...");
                    }
                    message = "service implemented !";
                    rpcStatusEx = RpcStatusEx.Successful;
                    LOG.info(message);
                    responseCode = ResponseCodes.RESPONSE_OK;
                }
                PathTopology pathTopology = new PathTopologyBuilder().build();
                notification = new ServiceRpcResultSpBuilder()
                        .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                        .setServiceName(serviceName).setStatus(rpcStatusEx)
                        .setStatusMessage(message).setPathTopology(pathTopology).build();
                sendNotifications(notification);
                responseCode = ResponseCodes.RESPONSE_OK;
                configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                        .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                        .setRequestId(input.getServiceHandlerHeader().getRequestId())
                        .setResponseCode(responseCode)
                        .setResponseMessage(message);
                output = new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon.build())
                        .build();
                return output;
            }
        });
    }

    @Override
    public ListenableFuture<ServiceDeleteOutput> serviceDelete(ServiceDeleteInput input) {
        return executor.submit(new Callable<ServiceDeleteOutput>() {
            @Override
            public ServiceDeleteOutput call() {
                LOG.info("ServiceDelete request ...");
                String serviceName = input.getServiceName();
                RpcStatusEx rpcStatusEx = RpcStatusEx.Pending;
                ServiceRpcResultSp notification =
                        new ServiceRpcResultSpBuilder().setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                                .setServiceName(serviceName).setStatus(rpcStatusEx)
                        .setStatusMessage("Service compliant, submitting serviceDelete Request ...").build();
                sendNotifications(notification);
                String message = "";
                String responseCode = null;
                ConfigurationResponseCommonBuilder configurationResponseCommon = null;
                ServiceDeleteOutput output = null;
                try {
                    LOG.info("Wait for 5s til beginning the Renderer serviceDelete request");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    message = "deleting service failed !";
                    LOG.error("deleting service failed !", e);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                    rpcStatusEx = RpcStatusEx.Failed;
                    notification = new ServiceRpcResultSpBuilder()
                            .setNotificationType(ServicePathNotificationTypes.ServiceDelete).setServiceName(serviceName)
                            .setStatus(rpcStatusEx).setStatusMessage(message).build();
                    sendNotifications(notification);
                    configurationResponseCommon =
                            new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                    .setResponseCode(responseCode).setResponseMessage(message);
                    output = new ServiceDeleteOutputBuilder()
                            .setConfigurationResponseCommon(configurationResponseCommon.build()).build();
                }
                if (rendererFailed) {
                    LOG.info("forcing renderer to fail");
                    message = "renderer failed !";
                    rpcStatusEx = RpcStatusEx.Failed;
                    LOG.error(message);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                } else {
                    if (isnetworkModelWlService) {
                        // Obtain path description
                        Optional<PathDescription> pathDescriptionOpt = getPathDescriptionFromDatastore(serviceName);
                        PathDescription pathDescription;
                        if (pathDescriptionOpt.isPresent()) {
                            pathDescription = pathDescriptionOpt.get();
                            networkModelWavelengthService.freeWavelengths(pathDescription);
                        } else {
                            LOG.warn("failed to get pathDescription for service : {}", serviceName);
                        }
                    } else {
                        LOG.warn("No need to execute networkModelWavelengthService...");
                    }
                    message = "service deleted !";
                    rpcStatusEx = RpcStatusEx.Successful;
                    LOG.info(message);
                    responseCode = ResponseCodes.RESPONSE_OK;
                }
                notification = new ServiceRpcResultSpBuilder()
                        .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                        .setServiceName(serviceName).setStatus(rpcStatusEx)
                        .setStatusMessage(message).build();
                sendNotifications(notification);
                responseCode = ResponseCodes.RESPONSE_OK;
                configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                        .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                        .setRequestId(input.getServiceHandlerHeader().getRequestId())
                        .setResponseCode(responseCode)
                        .setResponseMessage(message);
                output = new ServiceDeleteOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon.build())
                        .build();
                return output;
            }
        });
    }

    private Optional<PathDescription> getPathDescriptionFromDatastore(String serviceName) {
        InstanceIdentifier<PathDescription> pathDescriptionIID = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName)).child(PathDescription.class);
        ReadOnlyTransaction pathDescReadTx = this.dataBroker.newReadOnlyTransaction();
        try {
            LOG.debug("Getting path description for service {}", serviceName);
            return pathDescReadTx.read(LogicalDatastoreType.OPERATIONAL, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting path description from datastore {} for service {}!", pathDescriptionIID,
                    serviceName, e);
            return Optional.absent();
        }
    }

    public void setRendererFailed(Boolean rendererFailed) {
        this.rendererFailed = rendererFailed;
    }

    public void setIsnetworkModelWlService(Boolean isnetworkModelWlService) {
        this.isnetworkModelWlService = isnetworkModelWlService;
    }

    @Override
    public OperationResult reserveResource(PathDescription pathDescription) {
        return null;
    }

    @Override
    public OperationResult freeResource(PathDescription pathDescription) {
        return null;
    }
}
