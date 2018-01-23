/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.stubpce.CheckCoherencyHardSoft;
import org.opendaylight.transportpce.stubpce.SendingPceRPCs;
import org.opendaylight.transportpce.stubpce.StubpceCompliancyCheck;
import org.opendaylight.transportpce.stubpce.StubpceTxRxCheck;
import org.opendaylight.transportpce.stubpce.topology.PathDescriptionsOrdered;
import org.opendaylight.transportpce.stubpce.topology.SuperNodePath;
import org.opendaylight.transportpce.stubpce.topology.Topology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathDescriptionList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathDescriptionListBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathListBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.description.list.PathDescriptions;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.description.list.PathDescriptionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.description.list.PathDescriptionsKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.list.ServicePathsKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.rpc.result.PathDescription;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to implement StubpceService StubpceListener.
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */

public class StubpceImpl implements StubpceService {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubpceImpl.class);
    /** Permit to access database. */
    private DataBroker db;
    /** check service sdnc-request-header compliancy. */
    private StubpceCompliancyCheck compliancyCheck;
    /** check missing info on Tx/Rx for A/Z end. */
    private StubpceTxRxCheck txrxCheck;
    /** check coherency between hard and soft constraints. */
    private CheckCoherencyHardSoft checkCoherencyHardSoft;
    private NotificationPublishService notificationPublishService;
    private ServicePathRpcResult notification;
    private PathDescriptionBuilder pathDescriptionBuilder;
    private SendingPceRPCs sendingPCE;
    private final ListeningExecutorService executor = MoreExecutors
            .listeningDecorator(Executors.newFixedThreadPool(10));

    public StubpceImpl(NotificationPublishService notificationPublishService, DataBroker databroker) {
        this.notificationPublishService = notificationPublishService;
        this.db = databroker;
        pathDescriptionBuilder = null;
        if (initializePathDescriptionList(databroker)) {
            fillPathDesciptionList();
        }
        initializeServicePathList(databroker);
        /*if (initializeServicePathList(databroker)) {
            fillServicePathList();
        }*/
    }

    @Override
    public Future<RpcResult<CancelResourceReserveOutput>> cancelResourceReserve(CancelResourceReserveInput input) {
        LOG.info("RPC cancelResourceReserve  request received");
        String message = "";
        String responseCode = "";
        ConfigurationResponseCommonBuilder configurationResponseCommon = null;
        String serviceName = input.getServiceName();
        LOG.info("serviceName : {}", serviceName);
        if (serviceName != null) {
            sendingPCE = new SendingPceRPCs(input,db,executor);
            FutureCallback<Boolean> pceCallback = new FutureCallback<Boolean>() {
                String message = "";
                ServicePathRpcResult notification = null;

                @Override
                public void onFailure(Throwable arg0) {
                    LOG.error("Cancel resource failed : {}", arg0);
                    notification = new ServicePathRpcResultBuilder()
                            .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                            .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                            .setStatusMessage("Cancel resource request failed  : " + arg0.getMessage()).build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : {}", e);
                    }
                }

                @Override
                public void onSuccess(Boolean response) {
                    LOG.info("response : {}", response);
                    if (response) {
                        message = "Resource cancelled !";
                        notification = new ServicePathRpcResultBuilder()
                                .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Successful)
                                .setStatusMessage(message)
                                .build();
                    } else {
                        message = sendingPCE.getError();
                        notification = new ServicePathRpcResultBuilder()
                                .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                                .setServiceName("")
                                .setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                .build();
                        message = "Cancel request failed !";
                    }
                    LOG.info(notification.toString());
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : {}", e);
                    }
                    LOG.info(message);
                }
            };
            ListenableFuture<Boolean> pce = sendingPCE.cancelResourceReserve();
            Futures.addCallback(pce, pceCallback, executor);
            LOG.info("Cancel Resource Request in progress ...");
            configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator("No")
                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                    .setResponseMessage("Service compliant, Cancel resource request in progress...")
                    .setResponseCode("200");

            CancelResourceReserveOutputBuilder output = new CancelResourceReserveOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon.build());
            return RpcResultBuilder.success(output.build()).buildFuture();
        } else {
            message = "serviceName / requestId is not correct !";
            responseCode = "500";
            notification = new ServicePathRpcResultBuilder()
                    .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                    .setStatusMessage(message).build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : {}", e);
            }
        }
        configurationResponseCommon = new ConfigurationResponseCommonBuilder();
        configurationResponseCommon.setAckFinalIndicator("Yes")
        .setRequestId(input.getServiceHandlerHeader().getRequestId())
        .setResponseMessage(message)
        .setResponseCode(responseCode).build();
        CancelResourceReserveOutputBuilder output = new CancelResourceReserveOutputBuilder();
        output.setConfigurationResponseCommon(configurationResponseCommon.build());
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<PathComputationRequestOutput>> pathComputationRequest(PathComputationRequestInput input) {
        LOG.info("RPC pathcomputation request received");
        String message = "";
        String responseCode = "";
        boolean coherencyHardSoft = false;
        boolean commonId = true;
        ConfigurationResponseCommonBuilder configurationResponseCommon = null;
        compliancyCheck = new StubpceCompliancyCheck(input.getServiceName(), input.getServiceHandlerHeader());
        if (compliancyCheck.check(false, true)) {
            LOG.info("Service compliant !");
            /**
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new StubpceTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new StubpceTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked !");
                    /**
                     * If OK, common-id is verified in order to see if there is
                     * no routing policy provided. If yes, the routing
                     * constraints of the policy are recovered and coherency
                     * with hard/soft constraints provided in the input of the
                     * RPC.
                     */
                    if (input.getHardConstraints() != null || input.getSoftConstraints() != null) {
                        LOG.info("Constraints specified !");
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
                        notification = new ServicePathRpcResultBuilder()
                                .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                .setStatusMessage("Service compliant, submitting pathComputation Request ...").build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : {}", e);
                        }
                        sendingPCE = new SendingPceRPCs(input,db,executor);
                        FutureCallback<Boolean> pceCallback = new FutureCallback<Boolean>() {
                            String message = "";
                            ServicePathRpcResult notification = null;

                            @Override
                            public void onFailure(Throwable arg0) {
                                LOG.error("Failure message : {}", arg0.toString());
                                LOG.error("Path calculation failed !");
                                notification = new ServicePathRpcResultBuilder()
                                        .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                                        .setStatusMessage("PCR Request failed  : " + arg0.getMessage()).build();
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : {}", e);
                                }
                            }

                            @Override
                            public void onSuccess(Boolean response) {
                                LOG.info("response : {}", response);
                                if (response) {
                                    message = "Path Computated !";
                                    ServicePathRpcResultBuilder tmp = new ServicePathRpcResultBuilder()
                                            .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                                            .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Successful)
                                            .setStatusMessage(message);
                                    pathDescriptionBuilder = sendingPCE.getPathDescription();
                                    if (pathDescriptionBuilder != null) {
                                        PathDescription pathDescription = new org.opendaylight.yang.gen.v1.http.org
                                                .transportpce.b.c._interface.servicepath.rev170426.service.path
                                                .rpc.result.PathDescriptionBuilder()
                                                .setAToZDirection(pathDescriptionBuilder.getAToZDirection())
                                                .setZToADirection(pathDescriptionBuilder.getZToADirection())
                                                .build();
                                        tmp.setPathDescription(new PathDescriptionBuilder()
                                                .setAToZDirection(pathDescription.getAToZDirection())
                                                .setZToADirection(pathDescription.getZToADirection())
                                                .build());
                                    }
                                    notification = tmp.build();
                                } else {
                                    message = sendingPCE.getError();
                                    notification = new ServicePathRpcResultBuilder()
                                            .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                                            .setServiceName("")
                                            .setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                            .build();
                                    message = "Path not calculated!";
                                }
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : {}", e);
                                }
                                LOG.info(message);
                            }
                        };
                        ListenableFuture<Boolean> pce = sendingPCE.pathComputation();
                        Futures.addCallback(pce, pceCallback, executor);
                        LOG.info("PathComputation Request in progress ...");
                        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                                .setAckFinalIndicator("No")
                                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                .setResponseMessage("Service compliant, Path calculating in progress...")
                                .setResponseCode("200");

                        PathComputationRequestOutputBuilder output = new PathComputationRequestOutputBuilder()
                                .setConfigurationResponseCommon(configurationResponseCommon.build());
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
            notification = new ServicePathRpcResultBuilder()
                    .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                    .setStatusMessage("Service not compliant : " + message).build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : {}", e);
            }
        }
        configurationResponseCommon = new ConfigurationResponseCommonBuilder();
        configurationResponseCommon.setAckFinalIndicator("Yes")
        .setRequestId(input.getServiceHandlerHeader().getRequestId())
        .setResponseMessage(message)
        .setResponseCode(responseCode).build();
        PathComputationRequestOutputBuilder output = new PathComputationRequestOutputBuilder();
        output.setConfigurationResponseCommon(configurationResponseCommon.build());
        return RpcResultBuilder.success(output.build()).buildFuture();

    }

    /**
     * Initialize PathDescriptionList Structure on DataStore.
     *
     * @param DataBroker
     *            Access DataStore
     */
    private boolean initializePathDescriptionList(DataBroker db) {
        Boolean result = true;
        LOG.info("Preparing to initialize the PathDescription List");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<PathDescriptionList> iid = InstanceIdentifier.create(PathDescriptionList.class);
        PathDescriptionList pathDescriptionList = new PathDescriptionListBuilder().build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, pathDescriptionList);
        Future<Void> future = transaction.submit();
        try {
            Futures.getChecked(future, ExecutionException.class);
        } catch (ExecutionException e) {
            LOG.error("Failed to create PathDescription List");
            result = false;
        }
        return result;
    }

    /**
     * Initialize ServicePathList Structure on DataStore.
     *
     * @param DataBroker
     *            Access DataStore
     * @return <code>true</code> if ok, <code>false</code> else
     */
    private boolean initializeServicePathList(DataBroker db) {
        Boolean result = true;
        LOG.info("Preparing to initialize the ServicePathList registry");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<ServicePathList> iid = InstanceIdentifier.create(ServicePathList.class);
        ServicePathList servicePathList = new ServicePathListBuilder().build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, servicePathList);
        Future<Void> future = transaction.submit();
        try {
            Futures.getChecked(future, ExecutionException.class);
        } catch (ExecutionException e) {
            LOG.error("Failed to create ServicePathList List : {}", e.toString());
            result = false;
        }
        return result;
    }

    /**
     * fill PathDescriptionList
     * with direct paths and
     * indirect paths.
     */
    private void fillPathDesciptionList() {
        LOG.info("filling PathDescription List...");
        Topology topo = new Topology();
        topo.start();
        LOG.info("Network : {}", topo.getNetwork());
        SuperNodePath superNodePath = new SuperNodePath(topo.getNetwork());
        String aend = "NodeA";
        String zend = "NodeZ";
        superNodePath.run(aend, zend);
        fill(superNodePath.getDirectPathDesc(aend, zend, superNodePath.getPaths()));
        fill(superNodePath.getIndirectPathDesc(aend, zend, superNodePath.getPaths()));
    }

    /**
     * fill datastore with
     * Pathdescriptions List.
     *
     * @param sortedSet PathDescriptionsOrdered List
     */
    private void fill(SortedSet<PathDescriptionsOrdered> sortedSet) {
        InstanceIdentifier<PathDescriptions> iid;
        WriteTransaction writeTx;
        Future<Void> future;
        if (!sortedSet.isEmpty()) {
            Iterator<PathDescriptionsOrdered> it = sortedSet.iterator();
            while (it.hasNext()) {
                PathDescriptions pathDesc = it.next().getPathDescriptions();
                if (pathDesc != null) {
                    iid = InstanceIdentifier.create(PathDescriptionList.class)
                            .child(PathDescriptions.class, new PathDescriptionsKey(pathDesc.getPathName()));
                    writeTx = db.newWriteOnlyTransaction();
                    writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, pathDesc);
                    future = writeTx.submit();
                    try {
                        Futures.getChecked(future, ExecutionException.class);
                    } catch (ExecutionException e) {
                        LOG.error("Failed to write PathDescriptions to PathDescriptionsList : {}", e.toString());
                    }
                } else {
                    LOG.error("PathDescriptions gets is null !");
                }
            }
        } else {
            LOG.info("PathDescriptions List is empty !");
        }
    }

    /**
     * read Service from ServiceList DataStore.
     *
     * @param serviceName
     *            Name of Service
     *
     * @return <code>Services</code>
     */
    @SuppressWarnings("unused")
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
            LOG.debug("Service '{}' present !", serviceName);
            result = new ServicesBuilder(optional.get()).build();
        }
        return result;
    }

    /**
     * read ServicePath Service from ServicePathList DataStore.
     *
     * @param serviceName
     *            Name of ServicePath
     *
     * @return <code>Services</code>
     */
    @SuppressWarnings("unused")
    private ServicePaths readServicePathList(String serviceName) {
        ServicePaths result = null;
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        InstanceIdentifier<ServicePaths> iid = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName));
        Future<Optional<ServicePaths>> future = readTx.read(LogicalDatastoreType.OPERATIONAL,iid);
        Optional<ServicePaths> optional = Optional.absent();
        try {
            optional = Futures.getChecked(future, ExecutionException.class);
        } catch (ExecutionException e) {
            LOG.error("Reading service failed:", e);
        }
        if (optional.isPresent()) {
            LOG.debug("Service '{}' present !", serviceName);
            result = new ServicePathsBuilder(optional.get()).build();
        }
        return result;
    }

    /**
     * read PathDescription information from PathDescriptionList.
     *
     * @param pathName
     *            Name of PathDescription
     *
     * @return <code>PathDescriptions</code>
     */
    @SuppressWarnings("unused")
    private PathDescriptions readPathDescriptionList(String pathName) {
        PathDescriptions result = null;
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        InstanceIdentifier<PathDescriptions> iid = InstanceIdentifier.create(PathDescriptionList.class)
                .child(PathDescriptions.class, new PathDescriptionsKey(pathName));
        Future<Optional<PathDescriptions>> future = readTx.read(LogicalDatastoreType.OPERATIONAL,iid);
        Optional<PathDescriptions> optional = Optional.absent();
        try {
            optional = Futures.getChecked(future, ExecutionException.class);
        } catch (ExecutionException e) {
            LOG.error("Reading service failed:", e);
        }
        if (optional.isPresent()) {
            LOG.debug("PathDescritions '{}' present !", pathName);
            result = new PathDescriptionsBuilder(optional.get()).build();
        }
        return result;
    }

    /**
     * register ServicePath Service to ServicePathList with PathDescription
     * information.
     *
     * @param input
     *            PathComputationRequestInput
     * @return String operations result, null if ok or not otherwise
     */
    @SuppressWarnings("unused")
    private String writeServicePathList(PathComputationRequestInput input) {
        String serviceName = input.getServiceName();
        LOG.debug("Write ServicePath '{}' Service", serviceName);
        String result = null;
        LOG.debug("Writing '{}' ServicePath", serviceName);
        InstanceIdentifier<ServicePaths> iid = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName));

        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service
            .path.PathDescriptionBuilder path = new org.opendaylight.yang.gen.v1.http.org.transportpce
            .b.c._interface.service.types.rev170426.service.path.PathDescriptionBuilder();
        path.setAToZDirection(pathDescriptionBuilder.getAToZDirection());
        path.setZToADirection(pathDescriptionBuilder.getZToADirection());
        ServicePaths service = new ServicePathsBuilder().setServicePathName(input.getServiceName())
                .setSoftConstraints(input.getSoftConstraints()).setHardConstraints(input.getHardConstraints())
                .setPathDescription(path.build()).build();
        WriteTransaction writeTx = db.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
        Future<Void> future = writeTx.submit();
        try {
            Futures.getChecked(future, ExecutionException.class);
            result = null;
        } catch (ExecutionException e) {
            LOG.error("Failed to write service to Service List");
            result = "Failed to write service to Service List";
        }
        return result;
    }

    public PathDescriptionBuilder getPathDescriptionBuilder() {
        return pathDescriptionBuilder;
    }

    public void setPathDescriptionBuilder(PathDescriptionBuilder pathDescriptionBuilder) {
        this.pathDescriptionBuilder = pathDescriptionBuilder;
    }
}
