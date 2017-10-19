/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.base.Optional;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.servicehandler.CheckCoherencyHardSoft;
import org.opendaylight.transportpce.servicehandler.MappingAndSendingPCRequest;
import org.opendaylight.transportpce.servicehandler.MappingAndSendingSIRequest;
import org.opendaylight.transportpce.servicehandler.ServicehandlerCompliancyCheck;
import org.opendaylight.transportpce.servicehandler.ServicehandlerTxRxCheck;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev170930.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev170930.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.Topology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.EquipmentNotificationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.EquipmentNotificationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.NetworkReOptimizationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.NetworkReOptimizationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceFeasibilityCheckOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteConfirmInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteConfirmOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRestorationOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReversionInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReversionOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRollInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRollOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToA;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response.parameters.sp.response.parameters.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.rpc.result.sp.PathTopology;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.rpc.result.sp.PathTopologyBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to implement ServicehandlerService and ServicehandlerListener.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on behalf of Orange
 */
public class ServicehandlerImpl implements OrgOpenroadmServiceService,StubpceListener,
    StubrendererListener,AutoCloseable {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);
    /** Permit to access database. */
    private DataBroker db;
    /** check service sdnc-request-header compliancy. */
    private ServicehandlerCompliancyCheck compliancyCheck;
    /** check missing info on Tx/Rx for A/Z end. */
    private ServicehandlerTxRxCheck txrxCheck;
    /** check coherency between hard and soft constraints. */
    private CheckCoherencyHardSoft checkCoherencyHardSoft;
    /**
     * Map and Send PCE requests : -
     * path-computation-request/cancel-resource-reserve.
     */
    private MappingAndSendingPCRequest mappingAndSendingPCRequest;
    /**
     * Map and Send Service Implemention requests : - service
     * implementation/service delete.
     */
    private MappingAndSendingSIRequest mappingAndSendingSIRequest;

    private RpcProviderRegistry rpcRegistry;
    private NotificationPublishService notificationPublishService;
    private final ListeningExecutorService executor;

    private ServicePathRpcResult servicePathRpcResult = null;
    private ServiceRpcResultSp serviceRpcResultSp = null;

    private String notificationUrl = "";
    private RpcActions action;

    private PathDescription pathDescription;
    private PathTopology pathTopology;
    private ServiceCreateInput serviceCreateInput;
    private ServiceDeleteInput serviceDeleteInput;
    private ServiceReconfigureInput serviceReconfigureInput;
    private Services service;
    private ServiceFeasibilityCheckInput serviceFeasibilityCheckInput;

    public ServicehandlerImpl(DataBroker databroker, RpcProviderRegistry rpcRegistry,
            NotificationPublishService notificationPublishService) {
        this.db = databroker;
        this.rpcRegistry = rpcRegistry;
        this.notificationPublishService = notificationPublishService;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
        serviceCreateInput = null;
        setServiceDeleteInput(null);
        setServiceReconfigureInput(null);
        initializeDataTree(db);
    }

    /**
     * delete service from
     * datastore after receiving
     * Stubrenderer notification.
     *
     */
    private void deleteServiceFromDatastore() {
        String serviceName = null;
        if (serviceDeleteInput != null) {
            LOG.info("deleteServiceFromDatastore came from RPC serviceDelete");
            serviceName = serviceDeleteInput.getServiceDeleteReqInfo().getServiceName();
        } else if (service != null) {
            LOG.info("deleteServiceFromDatastore came from RPC serviceRestoration");
            serviceName = service.getServiceName();
        }
        if (serviceName != null) {
            LOG.info("deleting service '" + serviceName + "'from datastore ...");
            ServiceRpcResultSh notification = null;
            String message = "";
            /**
             * Service delete confirmed deleting service from
             * database
             */
            if (writeOrModifyOrDeleteServiceList(serviceName, null,
                    null,1) == null) {
                /** Service delete. */
                message = "Service deleted from database";
            } else {
                message = "deleting service from database failed !";
            }
            LOG.info(message);
            notification = new ServiceRpcResultShBuilder()
                    .setNotificationType(ServiceNotificationTypes.ServiceDeleteResult)
                    .setServiceName(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName())
                    .setStatus(RpcStatusEx.Successful).setStatusMessage(message).build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }
        } else {
            LOG.error("Parameter 'ServiceName' fro deleteServiceFromDatastore is null !");
        }
    }


    /**
     *Put Service status to up
     *and add topology information
     *after receiving Stubrenderer
     *service implementation
     *notification.
     *
     * @param input ServiceCreateInput or
     */
    private <T> void updateServiceStatus(T input) {
        LOG.info("Updating Service Status ...");
        ServiceRpcResultSh notification = null;
        String message = "";
        String serviceName = null;
        ServiceNotificationTypes notif = null;
        if (input instanceof ServiceCreateInput) {
            LOG.info("Updating Service Status came from RPC serviceCreateInput ...");
            serviceName = serviceCreateInput.getServiceName();
            notif = ServiceNotificationTypes.ServiceCreateResult;
        } else if (input instanceof ServiceReconfigureInput) {
            LOG.info("Updating Service Status came from RPC serviceReconfigure ...");
            serviceName = serviceReconfigureInput.getNewServiceName();
            notif = ServiceNotificationTypes.ServiceReconfigureResult;
        } else if (input instanceof Services) {
            LOG.info("Updating Service Status came from RPC serviceRestoration ...");
            serviceName = service.getServiceName();
            notif = ServiceNotificationTypes.ServiceRestorationResult;
        }
        if (serviceName != null && notif != null) {
            if (pathTopology != null) {
                LOG.info("PathTopology contains in Stubrenderer notification received !");
                Topology topo = new TopologyBuilder()
                        .setAToZ(pathTopology.getAToZ())
                        .setZToA(pathTopology.getZToA())
                        .build();

                /**
                 * Service implemented setting
                 * Service op status to up.
                 */
                if (writeOrModifyOrDeleteServiceList(serviceName, null,topo,0) == null) {
                    /**
                     * Service modified.
                     */
                    StringBuilder build = new StringBuilder();
                    build.append(message);
                    build.append(" : Service Op Status changed to Up !");
                    message = build.toString();
                } else {
                    StringBuilder build = new StringBuilder();
                    build.append(message);
                    build.append(" but Failed to modify service from Service List !");
                    message = build.toString();
                }
                notification = new ServiceRpcResultShBuilder()
                        .setNotificationType(notif)
                        .setServiceName(serviceCreateInput.getServiceName())
                        .setStatus(RpcStatusEx.Successful).setStatusMessage(message)
                        .build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info("notification offer rejected : " + e);
                }
            } else {
                message = "pathTopology not in stubrenderer notification, cancelling pce resource reserve ...";
                LOG.info(message);
                notification = new ServiceRpcResultShBuilder()
                        .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                        .setServiceName(serviceCreateInput.getServiceName())
                        .setStatus(RpcStatusEx.Failed)
                        .setStatusMessage("message")
                        .build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info("notification offer rejected : " + e);
                }
                pceCancelResResource();
            }
        } else {
            LOG.info("Parameters 'serviceName' or/ and 'notiftype' is null");
        }
    }

    /**
     * Send pathComputation
     * request to PCE.
     *
     * @param input ServiceCreate or ServiceReconfigure or Services
     */
    private <T> void  pcePathComputation(T input) {
        LOG.info("sending pathcomputation request to pce ...");
        ServiceRpcResultSh notification = null;
        String serviceName = null;
        ServiceNotificationTypes type = null;
        /**
         * Before sending the PCE request, input data need to be
         * formatted according to the Service Handler PCE
         * interface data model.
         */
        if (input instanceof ServiceReconfigureInput) {
            LOG.info("PCR came from RPC serviceReconfigure ...");
            ServiceReconfigureInput tmp = (ServiceReconfigureInput)input;
            serviceName = tmp.getNewServiceName();
            type = ServiceNotificationTypes.ServiceReconfigureResult;
            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, tmp, true);
        } else if (input instanceof ServiceCreateInput) {
            LOG.info("PCR came from RPC serviceCreate ...");
            ServiceCreateInput tmp = (ServiceCreateInput)input;
            serviceName = tmp.getServiceName();
            type = ServiceNotificationTypes.ServiceCreateResult;
            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, tmp, true);
        } else if (input instanceof Services) {
            LOG.info("PCR came from RPC serviceRestoration ...");
            Services tmp = (Services)input;
            serviceName = tmp.getServiceName();
            type = ServiceNotificationTypes.ServiceRestorationResult;
            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, tmp, true);
        } else if (input instanceof ServiceFeasibilityCheckInput) {
            LOG.info("PCR came from RPC ServiceFeasibilityCheck ...");
            ServiceFeasibilityCheckInput tmp = (ServiceFeasibilityCheckInput)input;
            serviceName = "no name";
            type = ServiceNotificationTypes.ServiceCreateResult;
            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, tmp, false);
        }
        final String name = serviceName;
        final ServiceNotificationTypes notifType = type;

        /**
         * Once PCE request is being sent to the PCE on
         * interface B, PCE reply is expected until a timer
         * expires.
         */
        notification = new ServiceRpcResultShBuilder()
                .setNotificationType(notifType)
                .setServiceName(serviceName).setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting PathComputation Request ...").build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : " + e);
        }
        FutureCallback<Boolean> pceCallback =  new FutureCallback<Boolean>() {
            String message = "";
            ServiceRpcResultSh notification = null;

            @Override
            public void onSuccess(Boolean response) {
                if (response) {
                    /**
                     * If PCE reply is received before timer
                     * expiration with a positive result, a
                     * service is created with admin and
                     * operational status 'down'.
                     */
                    message = "PCE replied to PCR Request !";
                    LOG.info(message);
                    notification = new ServiceRpcResultShBuilder()
                            .setNotificationType(notifType)
                            .setServiceName(name).setStatus(RpcStatusEx.Pending)
                            .setStatusMessage(message).build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }
                } else {
                    message = mappingAndSendingPCRequest.getError();
                    notification = new ServiceRpcResultShBuilder()
                            .setNotificationType(notifType)
                            .setServiceName("").setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                            .build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }
                }
            }

            @Override
            public void onFailure(Throwable arg0) {
                LOG.error("Path not calculated..");
                notification = new ServiceRpcResultShBuilder()
                        .setNotificationType(notifType)
                        .setServiceName(name).setStatus(RpcStatusEx.Failed)
                        .setStatusMessage("PCR Request failed  : " + arg0.getMessage()).build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info("notification offer rejected : " + e);
                }

            }
        };
        ListenableFuture<Boolean> pce = mappingAndSendingPCRequest.pathComputationRequest();
        Futures.addCallback(pce, pceCallback, executor);
    }

    /**
     * Send RPC cancel reserve
     * resource to PCE.
     */
    private void pceCancelResResource() {
        LOG.info("sending RPC cancel reserve resource to PCE ...");
        Services pceService = null;
        ServiceNotificationTypes notif = null;
        if (serviceDeleteInput != null) {
            LOG.info("pceCancelResResource came from RPC serviceDelete");
            notif = ServiceNotificationTypes.ServiceDeleteResult;
            String serviceName = serviceDeleteInput.getServiceDeleteReqInfo().getServiceName();
            if (serviceName != null) {
                pceService = readServiceList(serviceName);
            } else {
                LOG.info("Parameter 'serviceName' for pceCancelResResource is null");
            }
        } else if (service != null) {
            notif = ServiceNotificationTypes.ServiceRestorationResult;
            LOG.info("pceCancelResResource came from RPC serviceRestoration");
            pceService = service;
        } else if (serviceReconfigureInput != null) {
            notif = ServiceNotificationTypes.ServiceReconfigureResult;
            LOG.info("pceCancelResResource came from RPC serviceReconfigure");
            String serviceName = serviceReconfigureInput.getServiceName();
            if (serviceName != null) {
                pceService = readServiceList(serviceName);
            } else {
                LOG.info("Parameter 'serviceName' for pceCancelResResource is null");
            }
        } else if (serviceCreateInput != null) {
            notif = ServiceNotificationTypes.ServiceCreateResult;
            LOG.info("pceCancelResResource came from RPC serviceCreate");
            String serviceName = serviceCreateInput.getServiceName();
            if (serviceName != null) {
                pceService = readServiceList(serviceName);
            } else {
                LOG.info("Parameter 'serviceName' for pceCancelResResource is null");
            }
        }
        if (pceService != null && notif != null) {
            final Services cancelService = pceService;
            final ServiceNotificationTypes type = notif;
            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, pceService, false);
            FutureCallback<Boolean> pceCallback =  new FutureCallback<Boolean>() {
                String message = "";
                ServiceRpcResultSh notification = null;
                @Override
                public void onSuccess(Boolean response) {
                    if (response) {
                        /**
                         * If PCE reply is received before timer
                         * expiration with a positive result, a
                         * service is created with admin and
                         * operational status 'down'.
                         */
                        message = "PCE replied to cancel resource Request !";
                        LOG.info(message);
                        notification = new ServiceRpcResultShBuilder()
                                .setNotificationType(type)
                                .setServiceName(cancelService.getServiceName()).setStatus(RpcStatusEx.Pending)
                                .setStatusMessage(message).build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                    } else {
                        message = mappingAndSendingPCRequest.getError();
                        notification = new ServiceRpcResultShBuilder()
                                .setNotificationType(type)
                                .setServiceName("").setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                .build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable arg0) {
                    message = "Cancel resource request failed !";
                    LOG.error(message);
                    notification = new ServiceRpcResultShBuilder()
                            .setNotificationType(type)
                            .setServiceName(cancelService.getServiceName()).setStatus(RpcStatusEx.Failed)
                            .setStatusMessage(message + " : " + arg0.getMessage()).build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }

                }
            };
            ListenableFuture<Boolean> pce = mappingAndSendingPCRequest.cancelResourceReserve();
            Futures.addCallback(pce, pceCallback, executor);
        }
    }

    private void stubrendererDelete() {
        LOG.info("sending RPC service delete to stubrenderer ...");
        String tmp = null;
        String id = null;
        if (service != null) {
            LOG.info("RPC service delete came from RPC serviceRestoration !");
            tmp = service.getServiceName();
            id = service.getCommonId();
        } else if (serviceDeleteInput != null) {
            LOG.info("RPC service delete came from ServiceDelete !");
            tmp = serviceDeleteInput.getServiceDeleteReqInfo().getServiceName();
            id = serviceDeleteInput.getSdncRequestHeader().getRequestId();
        }

        if (tmp != null && id != null) {
            final String serviceName = tmp;
            LOG.info("stubrendererDelete service '" + serviceName + "'");
            mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, id, serviceName);
            ListenableFuture<Boolean> renderer = mappingAndSendingSIRequest.serviceDelete();
            FutureCallback<Boolean> rendererCallback = new FutureCallback<Boolean>() {
                String message = "";
                ServiceRpcResultSh notification = null;

                @Override
                public void onFailure(Throwable arg0) {
                    message = "ServiceDelete Request failed : " + arg0;
                    LOG.error("ServiceDelete Request failed !");
                    notification = new ServiceRpcResultShBuilder()
                            .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                            .setServiceName(serviceName).setStatus(RpcStatusEx.Failed)
                            .setStatusMessage(message).build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }

                }

                @Override
                public void onSuccess(Boolean response) {
                    if (response) {
                        message = "Service deleted !";
                        notification = new ServiceRpcResultShBuilder()
                                .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                .setServiceName(serviceName).setStatus(RpcStatusEx.Pending)
                                .setStatusMessage(message).build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                    } else {
                        message = "deleting service failed !";
                        notification = new ServiceRpcResultShBuilder()
                                .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                .setServiceName(serviceName).setStatus(RpcStatusEx.Failed)
                                .setStatusMessage(message)
                                .build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                    }
                }
            };
            Futures.addCallback(renderer, rendererCallback, executor);
        } else {
            LOG.info("Parameter 'serviceName' and / or 'id' is null");
        }
    }

    /**
     * send a RPC serviceImplementation
     * to stubrenderer after
     * receiving a stubpce notification.
     *
     * @param input ServiceCreate or ServiceReconfigure
     */
    private <T> void stubrendererImplementation(T input) {
        ServiceRpcResultSh notification = null;
        String serviceName = null;
        String message = "";
        String newServiceName = null;
        ServiceNotificationTypes type = null;
        Boolean create = false;
        Boolean delete = true;
        if (pathDescription != null) {
            LOG.info("Pathdescription conatins in Stubpce notification received !");
            String result = null;
            PathComputationRequestOutput pathComputationResponse =
                    new PathComputationRequestOutputBuilder()
                    .setResponseParameters(new org.opendaylight.yang.gen.v1.http.org
                            .transportpce.b.c._interface.service.types.rev170426.response
                            .parameters.sp.ResponseParametersBuilder()
                            .setPathDescription(pathDescription)
                            .build())
                    .build();
            if (input instanceof ServiceReconfigureInput) {
                /** delete and write . */
                LOG.info("RPC serviceImplementation came from RPC serviceReconfigure ...");
                ServiceReconfigureInput tmp = (ServiceReconfigureInput)input;
                serviceName = tmp.getServiceName();
                newServiceName = tmp.getNewServiceName();
                type = ServiceNotificationTypes.ServiceReconfigureResult;
                delete = false;
                mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, tmp,pathComputationResponse);
            } else if (input instanceof ServiceCreateInput) {
                /** create. */
                LOG.info("RPC serviceImplementation came from RPC serviceCreate ...");
                ServiceCreateInput tmp = (ServiceCreateInput)input;
                serviceName = tmp.getServiceName();
                type = ServiceNotificationTypes.ServiceCreateResult;
                mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, tmp,pathComputationResponse);
                delete = true;
                create = true;
            } else if (input instanceof Services) {
                /** update. */
                LOG.info("RPC serviceImplementation came from RPC ServiceRestoration ...");
                Services tmp = new ServicesBuilder((Services)input)
                        .setAdministrativeState(State.OutOfService)
                        .setOperationalState(State.OutOfService)
                        .setLifecycleState(LifecycleState.Planned)
                        .build();
                serviceName = tmp.getServiceName();
                type = ServiceNotificationTypes.ServiceRestorationResult;
                mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, tmp, pathComputationResponse);
                delete = true;
                create = true;
            }
            final String name = serviceName;
            final ServiceNotificationTypes notifType = type;
            if (!create) { /** ServiceReconfigure. */
                if ((result = writeOrModifyOrDeleteServiceList(serviceName, pathComputationResponse,null, 1)) == null) {
                    LOG.info("Service '" + serviceName + "' deleted from datastore");
                    serviceName = newServiceName;
                    delete = true;
                } else {
                    LOG.info("deleting Service '" + serviceName + "' failed !");
                }
            }
            if (delete) {
                if ((result = writeOrModifyOrDeleteServiceList(serviceName,pathComputationResponse,null, 2)) != null) {
                    LOG.info("writting Service failed !");
                    StringBuilder build = new StringBuilder();
                    build.append(message);
                    build.append(" " + result);
                    message = build.toString();
                } else {
                    /**
                     * Send Implementation order to renderer
                     */
                    notification = new ServiceRpcResultShBuilder()
                            .setNotificationType(notifType)
                            .setServiceName(name)
                            .setStatus(RpcStatusEx.Pending)
                            .setStatusMessage("Submitting ServiceImplementation Request ...")
                            .build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }

                    FutureCallback<Boolean> rendererCallback = new FutureCallback<Boolean>() {
                        String message = "";
                        ServiceRpcResultSh notification = null;

                        @Override
                        public void onSuccess(Boolean response) {
                            if (response) {
                                /**
                                 * If stubrenderer reply is received before timer
                                 * expiration with a positive result, a
                                 * service is created with admin and
                                 * operational status 'down'.
                                 */
                                message = "StubRenderer replied to Request !";
                                LOG.info(message);
                                notification = new ServiceRpcResultShBuilder()
                                        .setNotificationType(notifType)
                                        .setServiceName(name).setStatus(RpcStatusEx.Pending)
                                        .setStatusMessage(message).build();
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : " + e);
                                }
                            } else {
                                message = mappingAndSendingSIRequest.getError();
                                notification = new ServiceRpcResultShBuilder()
                                        .setNotificationType(notifType)
                                        .setServiceName(name).setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                        .build();
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : " + e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable arg0) {
                            LOG.error("Service not implemented ...");
                            notification = new ServiceRpcResultShBuilder()
                                    .setNotificationType(notifType)
                                    .setServiceName(name).setStatus(RpcStatusEx.Failed)
                                    .setStatusMessage("Service implementation failed  : " + arg0.getMessage()).build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                        }
                    };
                    ListenableFuture<Boolean> renderer = mappingAndSendingSIRequest.serviceImplementation();
                    Futures.addCallback(renderer, rendererCallback, executor);
                }
            } else {
                LOG.info("deleting Service failed");
            }
        } else {
            message = "PathDescription contains in Stubpce notification "
                    + "not recieved !";
            LOG.info(message);
        }
    }

    /**
     * Checking Service Compliancy.
     *
     * @return String if not compliant, null else
     */
    private String serviceCompliancy(SdncRequestHeader sdncRequestHeader, String serviceName,
            ConnectionType connectionType, RpcActions rpcActions, ServiceEndpoint aend, ServiceEndpoint zend,
            String commonIdValue, HardConstraints hard, SoftConstraints soft) {
        String message = null;
        Boolean contype = false;
        Boolean sdncRequest = false;
        Boolean commonId = true;
        Boolean coherencyHardSoft = false;

        if (sdncRequestHeader != null) {
            sdncRequest = true;
        }
        if (connectionType != null) {
            contype = true;
        }
        compliancyCheck = new ServicehandlerCompliancyCheck(sdncRequestHeader, serviceName,
                connectionType, rpcActions);
        if (compliancyCheck.check(contype, sdncRequest)) {
            LOG.info("Service compliant !");
            /**
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(aend, 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(zend, 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    /**
                     * If OK, common-id is verified in order to see if there is
                     * no routing policy provided. If yes, the routing
                     * constraints of the policy are recovered and coherency
                     * with hard/soft constraints provided in the input of the
                     * RPC.
                     */
                    if (commonIdValue != null) {
                        LOG.info("Common-id specified");
                        /**
                         * Check coherency with hard/soft constraints.
                         */
                        checkCoherencyHardSoft = new CheckCoherencyHardSoft(hard,soft);
                        if (checkCoherencyHardSoft.check()) {
                            LOG.info("hard/soft constraints coherent !");
                            coherencyHardSoft = true;
                        } else {
                            LOG.info("hard/soft constraints are not coherent !");
                            message = "hard/soft constraints are not coherent !";
                        }
                    } else {
                        commonId = false;
                    }
                    if (!commonId || (commonId && coherencyHardSoft)) {
                        message = null;
                    }
                } else {
                    message = txrxCheck.getMessage();
                }
            } else {
                message = txrxCheck.getMessage();
            }
        } else {
            message = compliancyCheck.getMessage();
        }
        return message;
    }


    @Override
    public Future<RpcResult<ServiceCreateOutput>> serviceCreate(ServiceCreateInput input) {
        LOG.info("RPC service creation received");
        pathDescription = null;
        pathTopology = null;
        action = RpcActions.ServiceCreate;
        notificationUrl = null;
        LOG.info("notificationUrl : " + notificationUrl);
        setPathDescription(null);
        serviceCreateInput = input;
        setServiceDeleteInput(null);
        setServiceReconfigureInput(null);
        service = null;
        String message = "";
        String responseCode = "";
        ConfigurationResponseCommon configurationResponseCommon;
        LOG.info("checking Service Compliancy ...");
        /**
         * Upon receipt of service-create RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        String serviceCompliancy = null;
        if ((serviceCompliancy = serviceCompliancy(input.getSdncRequestHeader(), input.getServiceName(),
                input.getConnectionType(), RpcActions.ServiceCreate, input.getServiceAEnd(), input.getServiceZEnd(),
                input.getCommonId(), input.getHardConstraints(), input.getSoftConstraints())) != null) {
            message = "Service not compliant : " + serviceCompliancy;
            LOG.info(message);
        } else {
            LOG.info("Service compliant !");
            pcePathComputation(input);
            LOG.info("PCR Request in progress ");
            configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                    .setResponseMessage("Service compliant, serviceCreate in progress...")
                    .setResponseCode("200").build();

            ServiceCreateOutputBuilder output = new ServiceCreateOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon);

            return RpcResultBuilder.success(output.build()).buildFuture();

        }
        /*compliancyCheck = new ServicehandlerCompliancyCheck(input.getSdncRequestHeader(), input.getServiceName(),
                input.getConnectionType(), RpcActions.ServiceCreate);
        if (compliancyCheck.check(true, true)) {
            LOG.info("Service compliant !");
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    if (input.getCommonId() != null) {
                        LOG.info("Common-id specified");
                        checkCoherencyHardSoft = new CheckCoherencyHardSoft(input.getHardConstraints(),
                                input.getSoftConstraints());
                        if (checkCoherencyHardSoft.check()) {
                            LOG.info("hard/soft constraints coherent !");
                            coherencyHardSoft = true;
                        } else {
                            LOG.info("hard/soft constraints are not coherent !");
                            message = "hard/soft constraints are not coherent !";
                            responseCode = "500";
                        }
                    } else {
                        commonId = false;
                    }
                    if (!commonId || (commonId && coherencyHardSoft)) {
                        pcePathComputation(input);
                        LOG.info("PCR Request in progress ");
                        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                                .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                                .setResponseMessage("Service compliant, serviceCreate in progress...")
                                .setResponseCode("200").build();

                        ServiceCreateOutputBuilder output = new ServiceCreateOutputBuilder()
                                .setConfigurationResponseCommon(configurationResponseCommon);

                        return RpcResultBuilder.success(output.build()).buildFuture();
                    }
                } else {
                    message = txrxCheck.getMessage();
                    responseCode = "500";
                }
            } else {
                message = txrxCheck.getMessage();
                responseCode = "500";
            }
        } else {
            message = compliancyCheck.getMessage();
            responseCode = "500";
        }*/

        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("Yes")
                .setRequestId(input.getSdncRequestHeader().getRequestId()).setResponseMessage(message)
                .setResponseCode(responseCode).build();

        ServiceCreateOutputBuilder output = new ServiceCreateOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon);

        return RpcResultBuilder.success(output.build()).buildFuture();

    }

    @Override
    public Future<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        LOG.info("RPC serviceDelete request received for Service '" + input.getServiceDeleteReqInfo().getServiceName()
                + "'");
        setServiceDeleteInput(input);
        setServiceReconfigureInput(null);
        serviceCreateInput = null;
        service = null;
        String message = "";
        String responseCode = "";
        LOG.info("checking Service Compliancy ...");
        /**
         * Upon receipt of service-delete RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        compliancyCheck = new ServicehandlerCompliancyCheck(input.getSdncRequestHeader(),
                input.getServiceDeleteReqInfo().getServiceName(), RpcActions.ServiceDelete);
        if (compliancyCheck.check(false, true)) {
            LOG.info("Service compliant !");
            String serviceName = input.getServiceDeleteReqInfo().getServiceName();
            Services service = readServiceList(serviceName);
            if (service != null) {
                LOG.debug("Service '" + serviceName + "' present in datastore !");
                stubrendererDelete();
                LOG.info("ServiceDelete Request in progress ... ");
                ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                        .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                        .setResponseMessage("ServiceDelete Request in progress ...").setResponseCode("200").build();
                ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
                return RpcResultBuilder.success(output).buildFuture();
            } else {
                message = "Service '" + serviceName + "' not exists in datastore";
                LOG.info(message);
            }

        } else {
            message = "Service not compliant !";
            responseCode = "500";
            LOG.info(message);
        }
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("Yes").setRequestId(input.getSdncRequestHeader().getRequestId())
                .setResponseMessage(message).setResponseCode(responseCode).build();
        ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceFeasibilityCheckOutput>> serviceFeasibilityCheck(
            ServiceFeasibilityCheckInput input) {
        action = RpcActions.ServiceFeasibilityCheck;
        LOG.info("RPC service feasibilityCheck received");
        mappingAndSendingPCRequest = null;
        serviceFeasibilityCheckInput = input;
        serviceCreateInput = null;
        serviceDeleteInput = null;
        service = null;
        serviceReconfigureInput = null;

        ConfigurationResponseCommon configurationResponseCommon = null;
        String message = "";
        String responseCode = "";
        LOG.info("checking Service Compliancy ...");
        /**
         * Upon receipt of service-create RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        String name = "no name";
        String serviceCompliancy = null;
        if ((serviceCompliancy = serviceCompliancy(input.getSdncRequestHeader(), name, input.getConnectionType(),
                RpcActions.ServiceFeasibilityCheck, input.getServiceAEnd(), input.getServiceZEnd(), input.getCommonId(),
                input.getHardConstraints(), input.getSoftConstraints())) != null) {
            message = "Service not compliant : " + serviceCompliancy;
            LOG.info(message);
        } else {
            LOG.info("Service compliant !");
            pcePathComputation(input);
            LOG.info("PCR Request in progress ");
            configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                    .setResponseMessage("Service compliant, ServiceFeasibilityCheck in progress...")
                    .setResponseCode("200").build();

            ServiceFeasibilityCheckOutput output = new ServiceFeasibilityCheckOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon).build();
            return RpcResultBuilder.success(output).buildFuture();
        }
        /*compliancyCheck = new ServicehandlerCompliancyCheck(input.getSdncRequestHeader(), name,
                input.getConnectionType(), RpcActions.ServiceFeasibilityCheck);
        if (compliancyCheck.check(true, true)) {
            LOG.info("Service compliant !");
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    if (input.getCommonId() != null) {
                        LOG.info("Common-id specified");
                        checkCoherencyHardSoft = new CheckCoherencyHardSoft(input.getHardConstraints(),
                                input.getSoftConstraints());
                        if (checkCoherencyHardSoft.check()) {
                            LOG.info("hard/soft constraints coherent !");
                            coherencyHardSoft = true;
                        } else {
                            LOG.info("hard/soft constraints are not coherent !");
                            message = "hard/soft constraints are not coherent !";
                            responseCode = "500";
                        }
                    } else {
                        commonId = false;
                    }

                    if (!commonId || (commonId && coherencyHardSoft)) {
                        pcePathComputation(input);
                        LOG.info("PCR Request in progress ");
                        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                                .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                                .setResponseMessage("Service compliant, ServiceFeasibilityCheck in progress...")
                                .setResponseCode("200").build();

                        ServiceFeasibilityCheckOutput output = new ServiceFeasibilityCheckOutputBuilder()
                                .setConfigurationResponseCommon(configurationResponseCommon).build();
                        return RpcResultBuilder.success(output).buildFuture();
                    }
                } else {
                    message = txrxCheck.getMessage();
                    responseCode = "500";
                }
            } else {
                message = txrxCheck.getMessage();
                responseCode = "500";
            }
        } else {
            message = compliancyCheck.getMessage();
            responseCode = "500";
        }*/
        configurationResponseCommon = new ConfigurationResponseCommonBuilder().setAckFinalIndicator("Yes")
                .setRequestId(input.getSdncRequestHeader().getRequestId()).setResponseMessage(message)
                .setResponseCode(responseCode).build();
        ServiceFeasibilityCheckOutput output = new ServiceFeasibilityCheckOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceReconfigureOutput>> serviceReconfigure(ServiceReconfigureInput input) {
        LOG.info("RPC service reconfigure received");
        setServiceReconfigureInput(input);
        setServiceDeleteInput(null);
        serviceCreateInput = null;
        service = null;
        String message = "";
        LOG.info("checking Service Compliancy ...");
        /**
         * Upon receipt of service-create RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        String serviceCompliancy = null;
        if ((serviceCompliancy = serviceCompliancy(null, input.getServiceName(), input.getConnectionType(),
                RpcActions.ServiceReconfigure, input.getServiceAEnd(), input.getServiceZEnd(), input.getCommonId(),
                input.getHardConstraints(), input.getSoftConstraints())) != null) {
            message = "Service not compliant : " + serviceCompliancy;
            LOG.info(message);
        } else {
            LOG.info("Service compliant !");
            /**
             * Retrieving initial service topology.
             */
            String serviceName = input.getServiceName();
            Services service = readServiceList(serviceName);
            if (service != null) {
                LOG.debug("Service '" + serviceName + "' present in datastore !");
                /**
                 * Sending cancel resource reserve request to PCE.
                 */
                pceCancelResResource();
                ServiceReconfigureOutput output = new ServiceReconfigureOutputBuilder()
                        .setStatus(RpcStatus.Successful)
                        .setStatusMessage("ServiceReconfigure in progress ...").build();
                return RpcResultBuilder.success(output).buildFuture();
            } else {
                message = "Service '" + serviceName + "' not exists in datastore";
                LOG.info(message);
            }
        }
        /*compliancyCheck = new ServicehandlerCompliancyCheck(input.getServiceName(), input.getConnectionType(),
                RpcActions.ServiceReconfigure);
        if (compliancyCheck.check(true, false)) {
            LOG.info("Service compliant !");
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    if (input.getCommonId() != null) {
                        LOG.info("Common-id specified");
                        checkCoherencyHardSoft = new CheckCoherencyHardSoft(input.getHardConstraints(),
                                input.getSoftConstraints());
                        if (checkCoherencyHardSoft.check()) {
                            LOG.info("hard/soft constraints coherent !");
                            coherencyHardSoft = true;
                        } else {
                            LOG.info("hard/soft constraints are not coherent !");
                            message = "hard/soft constraints are not coherent !";
                        }
                    } else {
                        commonId = false;
                    }

                    if (!commonId || (commonId && coherencyHardSoft)) {
                        String serviceName = input.getServiceName();
                        Services service = readServiceList(serviceName);
                        if (service != null) {
                            LOG.debug("Service '" + serviceName + "' present in datastore !");
                            pceCancelResResource();
                            ServiceReconfigureOutput output = new ServiceReconfigureOutputBuilder()
                                    .setStatus(RpcStatusEx.Pending)
                                    .setStatusMessage("ServiceReconfigure in progress ...").build();
                            return RpcResultBuilder.success(output).buildFuture();
                        } else {
                            message = "Service '" + serviceName + "' not exists in datastore";
                            LOG.error(message);
                        }
                    }
                } else {
                    message = txrxCheck.getMessage();
                }
            } else {
                message = txrxCheck.getMessage();
            }
        } else {
            message = compliancyCheck.getMessage();
        }*/

        ServiceReconfigureOutput output = new ServiceReconfigureOutputBuilder().setStatus(RpcStatus.Successful)
                .setStatusMessage(message).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceRestorationOutput>> serviceRestoration(ServiceRestorationInput input) {
        LOG.info("RPC service restoration received");
        ServiceRpcResultSh notification = null;
        setServiceDeleteInput(null);
        setServiceReconfigureInput(null);
        notificationUrl = null;
        String message = "";
        LOG.info("checking Service Compliancy ...");
        compliancyCheck = new ServicehandlerCompliancyCheck(input.getServiceName(), RpcActions.ServiceRestoration);
        if (compliancyCheck.check(false, false)) {
            LOG.info("Service compliant !");
            /**
             * If compliant, Getting path from service DB.
             */
            String serviceName = input.getServiceName();
            Services service = readServiceList(serviceName);
            if (service != null) {
                this.service = service;
                LOG.debug("Service '" + serviceName + "' present in datastore !");
                notification = new ServiceRpcResultShBuilder()
                        .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                        .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                        .setStatusMessage("Service '" + serviceName + "' present in datastore, deleting service ...")
                        .build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info("notification offer rejected : " + e);
                }
                stubrendererDelete();
                LOG.info("PCR Request in progress ");
                ServiceRestorationOutput output = new ServiceRestorationOutputBuilder()
                        .setStatus(RpcStatus.Successful)
                        .setStatusMessage("ServiceRestoration in progress...").build();
                return RpcResultBuilder.success(output).buildFuture();
            } else {
                message = "Service '" + serviceName + "' not exists in datastore";
                LOG.error(message);
            }
        } else {
            message = compliancyCheck.getMessage();
            LOG.error(message);
        }

        ServiceRestorationOutput output = new ServiceRestorationOutputBuilder().setStatus(RpcStatus.Successful)
                .setStatusMessage(message).build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<TempServiceDeleteOutput>> tempServiceDelete(TempServiceDeleteInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<TempServiceCreateOutput>> tempServiceCreate(TempServiceCreateInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Initialize ServiceList Structure on Datastore.
     *
     * @param DataBroker
     *            Access Datastore
     */
    private void initializeDataTree(DataBroker db) {
        LOG.info("Preparing to initialize the greeting registry");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<ServiceList> iid = InstanceIdentifier.create(ServiceList.class);
        ServiceList greetingRegistry = new ServiceListBuilder().build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, greetingRegistry);
        Future<Void> future = transaction.submit();
        try {
            Futures.getChecked(future, ExecutionException.class);
        } catch (ExecutionException e) {
            LOG.error("Failed to create Service List");
        }
    }

    /**
     * Map Input (ServiceCreateInmput, ServiceReconfigureInput) and output
     * (PathComputationRequestOutput) to Service.
     *
     * @param serviceCreateInput
     *            ServiceCreateInput parameter
     * @param serviceReconfigureInput
     *            serviceReconfigureInput parameter
     * @param output
     *            PathComputationRequestOutput parameter
     *
     * @return Services Service data
     */
    private Services mappingServices(ServiceCreateInput serviceCreateInput,
            ServiceReconfigureInput serviceReconfigureInput, PathComputationRequestOutput output) {
        LOG.info("Mapping informations to Services");
        ServiceAEnd aend = null;
        ServiceZEnd zend = null;
        ServicesBuilder service = new ServicesBuilder();
        if (serviceCreateInput != null) {
            aend = new ServiceAEndBuilder(serviceCreateInput.getServiceAEnd()).build();
            zend = new ServiceZEndBuilder(serviceCreateInput.getServiceZEnd()).build();
            service.setServiceName(serviceCreateInput.getServiceName()).setAdministrativeState(State.OutOfService)
            .setOperationalState(State.OutOfService).setCommonId(serviceCreateInput.getCommonId())
            .setConnectionType(serviceCreateInput.getConnectionType())
            .setCustomer(serviceCreateInput.getCustomer())
            .setCustomerContact(serviceCreateInput.getCustomerContact())
            .setHardConstraints(serviceCreateInput.getHardConstraints())
            .setSoftConstraints(serviceCreateInput.getSoftConstraints())
            .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend)
            .setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader());

        } else if (serviceReconfigureInput != null) {
            aend = new ServiceAEndBuilder(serviceReconfigureInput.getServiceAEnd()).build();
            zend = new ServiceZEndBuilder(serviceReconfigureInput.getServiceZEnd()).build();
            service.setServiceName(serviceReconfigureInput.getNewServiceName())
            .setAdministrativeState(State.OutOfService).setOperationalState(State.OutOfService)
            .setCommonId(serviceReconfigureInput.getCommonId())
            .setConnectionType(serviceReconfigureInput.getConnectionType())
            .setCustomer(serviceReconfigureInput.getCustomer())
            .setCustomerContact(serviceReconfigureInput.getCustomerContact())
            .setHardConstraints(serviceReconfigureInput.getHardConstraints())
            .setSoftConstraints(serviceReconfigureInput.getSoftConstraints())
            .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);
        }

        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response
            .parameters.sp.ResponseParameters responseParameters = output.getResponseParameters();
        if (responseParameters != null) {
            // service.setPceMetric(responseParameters.getPceMetric());
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426
                .response.parameters.sp.response.parameters.PathDescription pathDescription =
                responseParameters.getPathDescription();
            if (pathDescription != null) {
                List<AToZ> atozList = new ArrayList<AToZ>();
                List<ZToA> ztoaList = new ArrayList<ZToA>();

                for (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426
                        .path.description.atoz.direction.AToZ
                        tmp : pathDescription.getAToZDirection().getAToZ()) {

                    AToZKey key = new AToZKey(tmp.getKey().getId());
                    AToZ atoz = new AToZBuilder().setId(tmp.getId()).setKey(key)
                            // .setResource(tmp.getResource())
                            .build();
                    atozList.add(atoz);
                }

                for (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426
                        .path.description.ztoa.direction.ZToA
                        tmp : pathDescription.getZToADirection().getZToA()) {
                    ZToAKey key = new ZToAKey(tmp.getKey().getId());
                    ZToA ztoa = new ZToABuilder().setId(tmp.getId()).setKey(key)
                            // .setResource(tmp.getResource())
                            .build();
                    ztoaList.add(ztoa);
                }

                Topology topology = new TopologyBuilder().setAToZ(atozList).setZToA(ztoaList).build();
                service.setTopology(topology);
            }
        }
        return service.build();
    }

    /**
     * read Service from ServiceList DataStore.
     *
     * @param serviceName
     *            Name of Service
     *
     * @return <code>Services</code>
     */
    private Services readServiceList(String serviceName) {
        Services result = null;
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                new ServicesKey(serviceName));
        Future<Optional<Services>> future = readTx.read(LogicalDatastoreType.OPERATIONAL,iid);
        Optional<Services> optional = Optional.absent();
        try {
            optional = Futures.getChecked(future, ExecutionException.class);
        } catch (ExecutionException e) {
            LOG.error("Reading service failed:", e);
        }
        if (optional.isPresent()) {
            LOG.debug("Service '" + serviceName + "' present !");
            result = new ServicesBuilder(optional.get()).build();
        }
        return result;
    }

    /**
     * Write or Modify or Delete Service from/to ServiceList.
     *
     * @param serviceName
     *            Name of service
     * @param output
     *            PathComputationRequestOutput
     * @param topo
     *            Topology
     * @param choice
     *            0 - Modify 1 - Delete 2 - Write
     * @return String operations result, null if ok or not otherwise
     */
    private String writeOrModifyOrDeleteServiceList(String serviceName, PathComputationRequestOutput output,
            Topology topo, int choice) {
        LOG.info("WriteOrModifyOrDeleting '" + serviceName + "' Service");
        WriteTransaction writeTx = db.newWriteOnlyTransaction();
        String result = null;
        Services readService = readServiceList(serviceName);
        Future<Void> future = null;
        if (readService != null) {
            /**
             * Modify / Delete Service.
             */
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                    new ServicesKey(serviceName));
            ServicesBuilder service = new ServicesBuilder(readService);

            String action = null;
            switch (choice) {
                case 0: /** Modify. */
                    LOG.info("Modifying '" + serviceName + "' Service");
                    service.setOperationalState(State.InService).setAdministrativeState(State.InService);
                    service.setTopology(topo);
                    writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, service.build());
                    action = "modify";
                    break;

                case 1: /** Delete. */
                    LOG.info("Deleting '" + serviceName + "' Service");
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    action = "delete";
                    break;

                default:
                    LOG.info("No choice found");
                    break;
            }
            future = writeTx.submit();
            try {
                Futures.getChecked(future, ExecutionException.class);
            } catch (ExecutionException e) {
                LOG.info("Failed to " + action + " service from Service List");
                result = "Failed to " + action + " service from Service List";
            }
        } else if (choice == 2) { /** Write Service. */
            LOG.info("Writing '" + serviceName + "' Service");
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                    new ServicesKey(serviceName));
            Services writeService = null;
            if (this.service != null) {
                writeService = service;
            } else {
                writeService = mappingServices(serviceCreateInput, serviceReconfigureInput, output);
            }
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, writeService);
            future = writeTx.submit();
            try {
                Futures.getChecked(future, ExecutionException.class);
                result = null;
            } catch (ExecutionException e) {
                LOG.error("Failed to write service to Service List");
                result = "Failed to write service to Service List";
            }
        } else {
            LOG.info("Service is not present  in datastore ! ");
            result = "Service is not present  in datastore ! ";
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
    }

    @Override
    public Future<RpcResult<EquipmentNotificationOutput>> equipmentNotification(EquipmentNotificationInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<ServiceRerouteConfirmOutput>> serviceRerouteConfirm(ServiceRerouteConfirmInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<ServiceRerouteOutput>> serviceReroute(ServiceRerouteInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<ServiceReversionOutput>> serviceReversion(ServiceReversionInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<ServiceRollOutput>> serviceRoll(ServiceRollInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<NetworkReOptimizationOutput>> networkReOptimization(NetworkReOptimizationInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onServiceRpcResultSp(ServiceRpcResultSp notification) {
        if (notification != null && !compareServiceRpcResultSp(notification)) {
            serviceRpcResultSp = notification;
            StringBuilder build = new StringBuilder();
            build.append(
                    "Received '" + notification.getNotificationType() + "' StubRenderer notification" + "from service '"
                            + notification.getServiceName() + "' " + "with status '" + notification.getStatus() + "'");
            build.append(" with StatusMessage '" + notification.getStatusMessage() + "'");
            LOG.info(build.toString());
            switch (serviceRpcResultSp.getNotificationType().getIntValue()) {
                case 3 : /** service-implementation-request. */
                    if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
                        if (serviceRpcResultSp.getPathTopology() != null) {
                            pathTopology = new PathTopologyBuilder()
                                    .setAToZ(serviceRpcResultSp.getPathTopology().getAToZ())
                                    .setZToA(serviceRpcResultSp.getPathTopology().getZToA())
                                    .build();
                            LOG.info("PathTopology gets !");
                        } else {
                            LOG.info("'serviceRpcResultSp.getPathTopology()' parameter is null ");
                        }
                        if (serviceCreateInput != null) {
                            updateServiceStatus(serviceCreateInput);
                        } else if (serviceReconfigureInput != null) {
                            updateServiceStatus(serviceReconfigureInput);
                        } else if (service != null) {
                            updateServiceStatus(service);
                        }
                    } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
                        LOG.info("Stubrenderer computation failed !");
                        pceCancelResResource();
                    }
                    break;

                case 4 : /** service-delete. */
                    if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
                        if (service != null) { //serviceRestoration
                            LOG.info("RPC service delete came from serviceRestoration");
                            pcePathComputation(service);
                        } else {
                            pceCancelResResource();
                        }
                    } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
                        LOG.info("Stubrenderer computation failed !");
                    }
                    break;

                default:
                    break;
            }
        } else {
            LOG.info("ServiceRpcResultSp already wired !");
        }
    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (notification != null && !compareServicePathRpcResult(notification)) {
            servicePathRpcResult = notification;
            StringBuilder build = new StringBuilder();
            build.append(
                    "Received '" + notification.getNotificationType() + "' Stubpce notification" + "from service '"
                            + notification.getServiceName() + "' " + "with status '" + notification.getStatus() + "'");
            build.append(" with StatusMessage '" + notification.getStatusMessage() + "'");
            LOG.info(build.toString());
            switch (servicePathRpcResult.getNotificationType().getIntValue()) {
                case 1 : /** path-computation-request. */
                    if (servicePathRpcResult.getStatus() == RpcStatusEx.Successful) {
                        if (servicePathRpcResult.getPathDescription() != null) {
                            pathDescription = new PathDescriptionBuilder()
                                    .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
                                    .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection())
                                    .build();
                            LOG.info("PathDescription gets !");
                            if (serviceReconfigureInput != null) {
                                stubrendererImplementation(serviceReconfigureInput);
                            } else if (serviceCreateInput != null) {
                                stubrendererImplementation(serviceCreateInput);
                            } else if (service != null) {
                                stubrendererImplementation(service);
                            }
                        } else {
                            LOG.info("'servicePathRpcResult.getPathDescription()'parameter is null ");
                        }
                    } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
                        LOG.info("Stupce computation failed !");
                    }
                    break;

                case 2 : /** cancel-resource-reserve. */
                    if (servicePathRpcResult.getStatus() == RpcStatusEx.Successful) {
                        /**if it was an RPC serviceReconfigure, relaunch
                         * PCR else delete the service.
                         */
                        if (serviceReconfigureInput != null) {
                            LOG.info("cancel reserve resource request came from RPC serviceReconfigure !");
                            pcePathComputation(serviceReconfigureInput);
                        } else {
                            deleteServiceFromDatastore();
                        }
                    } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
                        LOG.info("Stupce computation failed !");
                    }
                    break;

                default:
                    break;
            }
        } else {
            LOG.info("ServicePathRpcResult already wired !");
        }
    }

    public RpcActions getAction() {
        return action;
    }

    public void setAction(RpcActions action) {
        this.action = action;
    }

    /**
     * Compare ServicePathRpcResult.
     *
     * @param notification ServicePathRpcResult
     * @return <code>Boolean</code> true if idem, false else
     */
    public Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
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

    /**
     * Compare ServiceRpcResultSp.
     *
     * @param notification ServiceRpcResultSp
     * @return <code>Boolean</code> true if idem, false else
     */
    public Boolean compareServiceRpcResultSp(ServiceRpcResultSp notification) {
        Boolean result = true;
        if (serviceRpcResultSp == null) {
            result = false;
        } else {
            if (serviceRpcResultSp.getNotificationType() != notification.getNotificationType()) {
                result = false;
            }
            if (serviceRpcResultSp.getServiceName() != notification.getServiceName()) {
                result = false;
            }
            if (serviceRpcResultSp.getStatus() != notification.getStatus()) {
                result = false;
            }
            if (serviceRpcResultSp.getStatusMessage() != notification.getStatusMessage()) {
                result = false;
            }
        }
        return result;
    }

    public PathDescription getPathDescription() {
        return pathDescription;
    }

    public void setPathDescription(PathDescription pathDescription) {
        this.pathDescription = pathDescription;
    }

    public ServiceDeleteInput getServiceDeleteInput() {
        return serviceDeleteInput;
    }

    public void setServiceDeleteInput(ServiceDeleteInput serviceDeleteInput) {
        this.serviceDeleteInput = serviceDeleteInput;
    }

    public ServiceReconfigureInput getServiceReconfigureInput() {
        return serviceReconfigureInput;
    }

    public void setServiceReconfigureInput(ServiceReconfigureInput serviceReconfigureInput) {
        this.serviceReconfigureInput = serviceReconfigureInput;
    }

    public ServiceFeasibilityCheckInput getServiceFeasibilityCheckInput() {
        return serviceFeasibilityCheckInput;
    }

    public void setServiceFeasibilityCheckInput(ServiceFeasibilityCheckInput serviceFeasibilityCheckInput) {
        this.serviceFeasibilityCheckInput = serviceFeasibilityCheckInput;
    }
}
