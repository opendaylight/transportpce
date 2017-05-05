/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.base.Optional;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.servicehandler.CheckCoherencyHardSoft;
import org.opendaylight.transportpce.servicehandler.LoggingFuturesCallBack;
import org.opendaylight.transportpce.servicehandler.MappingAndSendingPCRequest;
import org.opendaylight.transportpce.servicehandler.MappingAndSendingSIRequest;
import org.opendaylight.transportpce.servicehandler.ServicehandlerCompliancyCheck;
import org.opendaylight.transportpce.servicehandler.ServicehandlerTxRxCheck;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.EquipmentNotificationInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.EquipmentNotificationOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.NetworkReOptimizationInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.NetworkReOptimizationOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceFeasibilityCheckOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReconfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRerouteConfirmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRerouteConfirmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRestorationOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReversionInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReversionOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRollInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRollOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServicehandlerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.feasibility.check.output.IntermediateSites;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.feasibility.check.output.IntermediateSitesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.response.parameters.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.response.parameters.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.Topology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToA;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServicePathNotificationTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Class to implement ServicehandlerService & ServicehandlerListener.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class ServicehandlerImpl implements ServicehandlerService, StubpceListener, StubrendererListener, AutoCloseable {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);
    /* Permit to access database. */
    private DataBroker db;
    /* check service sdnc-request-header compliancy. */
    private ServicehandlerCompliancyCheck compliancyCheck;
    /* check missing info on Tx/Rx for A/Z end. */
    private ServicehandlerTxRxCheck txrxCheck;
    /* check coherency between hard & sof constraints. */
    private CheckCoherencyHardSoft checkCoherencyHardSoft;
    /*
     * Map and Send PCE requests : -
     * path-computation-request/cancel-resource-reserve.
     */
    private MappingAndSendingPCRequest mappingAndSendingPCRequest;
    /*
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

    public ServicehandlerImpl(DataBroker databroker, RpcProviderRegistry rpcRegistry,
            NotificationPublishService notificationPublishService) {
        this.db = databroker;
        this.rpcRegistry = rpcRegistry;
        this.notificationPublishService = notificationPublishService;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
        initializeDataTree(db);
    }

    @Override
    public Future<RpcResult<ServiceCreateOutput>> serviceCreate(ServiceCreateInput input) {
        LOG.info("RPC service creation received");
        action = RpcActions.ServiceCreate;
        boolean commonId = true;
        boolean coherencyHardSoft = false;
        ServiceRpcResult notification = null;
        notificationUrl = null;// input.getSdncRequestHeader().getnotificationUrl();
        LOG.info("notificationUrl : " + notificationUrl);

        ResponseParametersBuilder responseParameters = new ResponseParametersBuilder();
        ConfigurationResponseCommon configurationResponseCommon;
        String message = "";
        String responseCode = "";

        LOG.info("checking Service Compliancy ...");
        /*
         * Upon receipt of service-create RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        compliancyCheck = new ServicehandlerCompliancyCheck(input.getSdncRequestHeader(), input.getServiceName(),
                input.getConnectionType(), RpcActions.ServiceCreate);
        if (compliancyCheck.check(true, true)) {
            LOG.info("Service compliant !");
            /*
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    /*
                     * If OK, common-id is verified in order to see if there is
                     * no routing policy provided. If yes, the routing
                     * constraints of the policy are recovered and coherency
                     * with hard/soft constraints provided in the input of the
                     * RPC.
                     */
                    if (input.getCommonId() != null) {
                        LOG.info("Common-id specified");
                        /*
                         * Check coherency with hard/soft constraints
                         */

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
                        /*
                         * Before sending the PCE request, input data need to be
                         * formatted according to the Service Handler PCE
                         * interface data model.
                         */
                        mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, input, true);
                        /*
                         * Once PCE request is being sent to the PCE on
                         * interface B, PCE reply is expected until a timer
                         * expires.
                         */
                        notification = new ServiceRpcResultBuilder()
                                .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                .setStatusMessage("Service compliant, submitting PathComputation Request ...").build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                        sendNotifToUrl(notification, notificationUrl);
                        FutureCallback<PathComputationRequestOutput> pceCallback =
                                new FutureCallback<PathComputationRequestOutput>() {
                            String message = "";
                            String responseCode = "";
                            ServiceRpcResult notification = null;

                            @Override
                            public void onSuccess(PathComputationRequestOutput response) {
                                if (mappingAndSendingPCRequest.getSuccess() && response != null) {
                                    /*
                                     * If PCE reply is received before timer
                                     * expiration with a positive result, a
                                     * service is created with admin and
                                     * operational status 'down'.
                                     */
                                    LOG.info("PCE replied to PCR Request !");

                                    message = response.getConfigurationResponseCommon().getResponseMessage();
                                    notification = new ServiceRpcResultBuilder()
                                            .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                            .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                            .setStatusMessage("PCE replied to PCR Request !").build();
                                    try {
                                        notificationPublishService.putNotification(notification);
                                    } catch (InterruptedException e) {
                                        LOG.info("notification offer rejected : " + e);
                                    }
                                    sendNotifToUrl(notification, notificationUrl);
                                    String result = null;
                                    if ((result = writeOrModifyOrDeleteServiceList(input.getServiceName(), input,
                                            response, 2)) != null) {
                                        StringBuilder build = new StringBuilder();
                                        build.append(message);
                                        build.append(" " + result);
                                        message = build.toString();
                                    } else {
                                        /*
                                         * Send Implementation order to renderer
                                         */
                                        mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, input,
                                                response);

                                        notification = new ServiceRpcResultBuilder()
                                                .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                                .setStatusMessage("Submitting ServiceImplementation Request ...")
                                                .build();
                                        try {
                                            notificationPublishService.putNotification(notification);
                                        } catch (InterruptedException e) {
                                            LOG.info("notification offer rejected : " + e);
                                        }
                                        sendNotifToUrl(notification, notificationUrl);

                                        /*
                                         * Once PCE request is being sent to the
                                         * PCE on interface B, PCE reply is
                                         * expected until a timer expires.
                                         */
                                        ServiceImplementationRequestOutput siOutput = null;
                                        try {
                                            siOutput = mappingAndSendingSIRequest.serviceImplementation().get();
                                        } catch (InterruptedException | ExecutionException e2) {
                                            LOG.error("mappingAndSendingSIRequest.serviceImplementation().get() : "
                                                    + e2.getMessage());
                                        }
                                        if (siOutput == null) {
                                            LOG.info("siOutput is null ");
                                            LOG.info("Success : " + mappingAndSendingPCRequest.getSuccess());
                                        }
                                        if (mappingAndSendingSIRequest.getSuccess() && siOutput != null) {
                                            ConfigurationResponseCommon siCommon = siOutput
                                                    .getConfigurationResponseCommon();
                                            // message =
                                            // siCommon.getResponseMessage();
                                            responseCode = siCommon.getResponseCode();
                                            message = "Service implemented !";
                                            LOG.info(message);
                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                                    .setServiceName(input.getServiceName())
                                                    .setStatus(RpcStatusEx.Successful).setStatusMessage(message)
                                                    .build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);
                                            /*
                                             * Service implemented setting
                                             * Service op status to up
                                             */
                                            if (writeOrModifyOrDeleteServiceList(input.getServiceName(), null, null,
                                                    0) == null) {
                                                /*
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
                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                                    .setServiceName(input.getServiceName())
                                                    .setStatus(RpcStatusEx.Successful).setStatusMessage(message)
                                                    .build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);
                                        } else {
                                            LOG.info("Service not implemented !");
                                            message = response.getConfigurationResponseCommon().getResponseMessage();
                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                                    .setServiceName(input.getServiceName())
                                                    .setStatus(RpcStatusEx.Failed)
                                                    .setStatusMessage(
                                                            "Service not implemented, cancelling ResourceResv ...")
                                                    .build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);
                                            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry,
                                                    input, false);
                                            /*
                                             * Send Cancel resource Request to
                                             * PCE.
                                             */
                                            CancelResourceReserveOutput cancelOuptut = null;
                                            try {
                                                cancelOuptut = mappingAndSendingPCRequest.cancelResourceReserve().get();
                                            } catch (InterruptedException | ExecutionException e1) {
                                                LOG.error(e1.getMessage());
                                            }
                                            if (mappingAndSendingPCRequest.getSuccess() && cancelOuptut != null) {
                                                LOG.info("Service ResourceResv cancelled !");
                                                message = response.getConfigurationResponseCommon()
                                                        .getResponseMessage();
                                                notification = new ServiceRpcResultBuilder()
                                                        .setNotificationType(
                                                                ServiceNotificationTypes.ServiceCreateResult)
                                                        .setServiceName(input.getServiceName())
                                                        .setStatus(RpcStatusEx.Failed)
                                                        .setStatusMessage("Service ResourceResv cancelled").build();
                                                try {
                                                    notificationPublishService.putNotification(notification);
                                                } catch (InterruptedException e) {
                                                    LOG.info("notification offer rejected : " + e);
                                                }
                                                sendNotifToUrl(notification, notificationUrl);

                                                message = cancelOuptut.getConfigurationResponseCommon()
                                                        .getResponseMessage();
                                                responseCode = cancelOuptut.getConfigurationResponseCommon()
                                                        .getResponseCode();

                                                StringBuilder build = new StringBuilder();
                                                build.append("Service not implemented - ");
                                                build.append(message);
                                                message = build.toString();

                                                LOG.info("PCE replied to CancelResourceResv Request !");
                                            } else {
                                                message = "Cancelling Resource reserved failed ";
                                                LOG.info(message);
                                                responseCode = "500";
                                                StringBuilder build = new StringBuilder();
                                                build.append("Service not implemented - ");
                                                build.append(message);
                                                message = build.toString();

                                                message = response.getConfigurationResponseCommon()
                                                        .getResponseMessage();
                                                notification = new ServiceRpcResultBuilder()
                                                        .setNotificationType(
                                                                ServiceNotificationTypes.ServiceCreateResult)
                                                        .setServiceName(input.getServiceName())
                                                        .setStatus(RpcStatusEx.Failed)
                                                        .setStatusMessage("Cancelling Resource reserved failed")
                                                        .build();
                                                try {
                                                    notificationPublishService.putNotification(notification);
                                                } catch (InterruptedException e) {
                                                    LOG.info("notification offer rejected : " + e);
                                                }
                                                sendNotifToUrl(notification, notificationUrl);
                                            }

                                        }
                                    }
                                } else {
                                    message = mappingAndSendingPCRequest.getError();// "Path
                                                                                    // not
                                                                                    // calculated";
                                    responseCode = "500";
                                    notification = new ServiceRpcResultBuilder()
                                            .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                            .setServiceName("").setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                            .build();
                                    try {
                                        notificationPublishService.putNotification(notification);
                                    } catch (InterruptedException e) {
                                        LOG.info("notification offer rejected : " + e);
                                    }
                                    sendNotifToUrl(notification, notificationUrl);
                                }

                            }

                            @Override
                            public void onFailure(Throwable arg0) {
                                LOG.error("Path not calculated..");
                                notification = new ServiceRpcResultBuilder()
                                        .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                        .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                                        .setStatusMessage("PCR Request failed  : " + arg0.getMessage()).build();
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : " + e);
                                }
                                sendNotifToUrl(notification, notificationUrl);
                            }
                        };
                        ListenableFuture<PathComputationRequestOutput> pce = mappingAndSendingPCRequest
                                .pathComputationRequest();
                        Futures.addCallback(pce, pceCallback);
                        LOG.info("PCR Request in progress ");
                        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                                .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                                .setResponseMessage("Service compliant, serviceCreate in progress...")
                                .setResponseCode("200").build();

                        ServiceCreateOutputBuilder output = new ServiceCreateOutputBuilder()
                                .setConfigurationResponseCommon(configurationResponseCommon)
                                .setResponseParameters(responseParameters.build());

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
        }

        configurationResponseCommon = new ConfigurationResponseCommonBuilder().setAckFinalIndicator("Yes")
                .setRequestId(input.getSdncRequestHeader().getRequestId()).setResponseMessage(message)
                .setResponseCode(responseCode).build();

        ServiceCreateOutputBuilder output = new ServiceCreateOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon)
                .setResponseParameters(responseParameters.build());

        return RpcResultBuilder.success(output.build()).buildFuture();

    }

    @Override
    public Future<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        LOG.info("RPC serviceDelete request received for Service '" + input.getServiceDeleteReqInfo().getServiceName()
                + "'");
        notificationUrl = null;// input.getSdncRequestHeader().getnotificationUrl();
        String message = "";
        String responseCode = "";
        ServiceRpcResult notification = null;
        ResponseParametersBuilder responseParameters = new ResponseParametersBuilder();
        LOG.info("checking Service Compliancy ...");
        /*
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
                /*
                 * If compliant, service-delete order is send to renderer.
                 */
                mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry,
                        input.getSdncRequestHeader().getRequestId(), input.getServiceDeleteReqInfo().getServiceName());

                notification = new ServiceRpcResultBuilder()
                        .setNotificationType(ServiceNotificationTypes.ServiceDeleteResult).setServiceName(serviceName)
                        .setStatus(RpcStatusEx.Pending)
                        .setStatusMessage("Service compliant, submitting serviceDelete Request ...").build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info("notification offer rejected : " + e);
                }
                sendNotifToUrl(notification, notificationUrl);
                FutureCallback<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                    .stubrenderer.rev170426.ServiceDeleteOutput> rendererCallback =
                    new FutureCallback<org.opendaylight.yang.gen.v1.http.org.opendaylight
                    .transportpce.stubrenderer.rev170426.ServiceDeleteOutput>() {
                    String message = "";
                    String responseCode = "";
                    ServiceRpcResult notification = null;

                    @Override
                    public void onFailure(Throwable arg0) {
                        LOG.error("ServiceDelete Request failed !");
                        notification = new ServiceRpcResultBuilder()
                                .setNotificationType(ServiceNotificationTypes.ServiceDeleteResult)
                                .setServiceName(serviceName).setStatus(RpcStatusEx.Failed)
                                .setStatusMessage("ServiceDelete Request failed !").build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                        sendNotifToUrl(notification, notificationUrl);
                    }

                    @Override
                    public void onSuccess(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                            .stubrenderer.rev170426.ServiceDeleteOutput arg0) {

                        if (mappingAndSendingPCRequest.getSuccess() && arg0 != null) {
                            message = "Service deleted !";
                            LOG.info(message);
                            notification = new ServiceRpcResultBuilder()
                                    .setNotificationType(ServiceNotificationTypes.ServiceDeleteResult)
                                    .setServiceName(input.getServiceDeleteReqInfo().getServiceName())
                                    .setStatus(RpcStatusEx.Successful).setStatusMessage("Service deleted !").build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                            sendNotifToUrl(notification, notificationUrl);

                            // message =
                            // result.getConfigurationResponseCommon().getResponseMessage();
                            responseCode = arg0.getConfigurationResponseCommon().getResponseCode();
                            /*
                             * Service delete confirmed deleting service from
                             * database
                             */
                            if (writeOrModifyOrDeleteServiceList(input.getServiceDeleteReqInfo().getServiceName(), null,
                                    null, 1) == null) {
                                /* Service delete. */
                                StringBuilder build = new StringBuilder();
                                build.append(message);
                                build.append(" : Service deleted from database");
                                message = build.toString();
                            } else {
                                StringBuilder build = new StringBuilder();
                                build.append(message);
                                build.append(" but Failed to delete service from database !");
                                message = build.toString();
                            }
                            LOG.info(message);
                            notification = new ServiceRpcResultBuilder()
                                    .setNotificationType(ServiceNotificationTypes.ServiceDeleteResult)
                                    .setServiceName(input.getServiceDeleteReqInfo().getServiceName())
                                    .setStatus(RpcStatusEx.Successful).setStatusMessage(message).build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                            sendNotifToUrl(notification, notificationUrl);
                        } else {
                            message = "deleting service failed";
                            responseCode = "500";
                            notification = new ServiceRpcResultBuilder()
                                    .setNotificationType(ServiceNotificationTypes.ServiceDeleteResult)
                                    .setServiceName(input.getServiceDeleteReqInfo().getServiceName())
                                    .setStatus(RpcStatusEx.Failed).setStatusMessage(message).build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                            sendNotifToUrl(notification, notificationUrl);
                        }
                    }
                };
                ListenableFuture<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                    .stubrenderer.rev170426.ServiceDeleteOutput> renderer =
                    mappingAndSendingSIRequest.serviceDelete();
                Futures.addCallback(renderer, rendererCallback);
                LOG.info("ServiceDelete Request in progress ... ");
                ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                        .setAckFinalIndicator("No").setRequestId(input.getSdncRequestHeader().getRequestId())
                        .setResponseMessage("ServiceDelete Request in progress ...").setResponseCode("200").build();
                ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
                return RpcResultBuilder.success(output).buildFuture();
            } else {
                message = "Service '" + serviceName + "' not exists in datastore";
                LOG.error(message);
            }

        } else {
            message = "Service not compliant !";
            responseCode = "500";
        }

        /*
         * Building output response.
         */

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
        notificationUrl = null;// input.getSdncRequestHeader().getnotificationUrl();
        action = RpcActions.ServiceFeasibilityCheck;
        LOG.info("RPC service feasibilityCheck received");
        boolean commonId = true;
        boolean coherencyHardSoft = false;
        ServiceRpcResult notification = null;
        String name = "no name";
        mappingAndSendingPCRequest = null;

        ConfigurationResponseCommon configurationResponseCommon = null;
        String message = "";
        String responseCode = "";
        LOG.info("checking Service Compliancy ...");
        /*
         * Upon receipt of service-create RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        compliancyCheck = new ServicehandlerCompliancyCheck(input.getSdncRequestHeader(), name,
                input.getConnectionType(), RpcActions.ServiceFeasibilityCheck);
        if (compliancyCheck.check(true, true)) {
            LOG.info("Service compliant !");
            /*
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    /*
                     * If OK, common-id is verified in order to see if there is
                     * no routing policy provided. If yes, the routing
                     * constraints of the policy are recovered and coherency
                     * with hard/soft constraints provided in the input of the
                     * RPC.
                     */
                    if (input.getCommonId() != null) {
                        LOG.info("Common-id specified");
                        /*
                         * Check coherency with hard/soft constraints
                         */

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
                        /*
                         * Before sending the PCE request, input data need to be
                         * formatted according to the Service Handler - PCE
                         * interface data model.
                         */
                        mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, input, false);
                        /*
                         * Once PCE request is being sent to the PCE on
                         * interface B, PCE reply is expected until a timer
                         * expires.
                         */
                        notification = new ServiceRpcResultBuilder()
                                .setNotificationType(ServiceNotificationTypes.ServiceCreateResult).setServiceName(name)
                                .setStatus(RpcStatusEx.Pending)
                                .setStatusMessage("Service compliant, Submitting PathComputation Request ...").build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                        sendNotifToUrl(notification, notificationUrl);

                        FutureCallback<PathComputationRequestOutput> pceCallback =
                                new FutureCallback<PathComputationRequestOutput>() {
                            String message = "";
                            String responseCode = "";
                            ServiceRpcResult notification = null;

                            @Override
                            public void onFailure(Throwable arg0) {
                                LOG.error("Path not calculated..");
                                notification = new ServiceRpcResultBuilder()
                                        .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                        .setServiceName(name).setStatus(RpcStatusEx.Failed)
                                        .setStatusMessage("PCR Request failed !").build();
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : " + e);
                                }
                                sendNotifToUrl(notification, notificationUrl);

                            }

                            @Override
                            public void onSuccess(PathComputationRequestOutput response) {

                                if (mappingAndSendingPCRequest.getSuccess() && response != null) {
                                    /*
                                     * If PCE reply is received before timer
                                     * expiration with a positive result, a
                                     * service is created with admin and
                                     * operational status 'down'.
                                     */
                                    LOG.info("PCE replied to PCR Request !");
                                    notification = new ServiceRpcResultBuilder()
                                            .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                            .setServiceName("").setStatus(RpcStatusEx.Successful)
                                            .setStatusMessage("Service Feasility Checked").build();
                                    try {
                                        notificationPublishService.putNotification(notification);
                                    } catch (InterruptedException e) {
                                        LOG.info("notification offer rejected : " + e);
                                    }
                                    message = response.getConfigurationResponseCommon().getResponseMessage();
                                    sendNotifToUrl(notification, notificationUrl);
                                } else {
                                    message = mappingAndSendingPCRequest.getError();// "Path
                                                                                    // not
                                                                                    // calculated";
                                    responseCode = "500";
                                    notification = new ServiceRpcResultBuilder()
                                            .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                            .setServiceName("").setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                            .build();
                                    try {
                                        notificationPublishService.putNotification(notification);
                                    } catch (InterruptedException e) {
                                        LOG.info("notification offer rejected : " + e);
                                    }
                                    sendNotifToUrl(notification, notificationUrl);
                                }
                            }

                        };
                        ListenableFuture<PathComputationRequestOutput> pce = mappingAndSendingPCRequest
                                .pathComputationRequest();
                        Futures.addCallback(pce, pceCallback);
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
        }

        configurationResponseCommon = new ConfigurationResponseCommonBuilder().setAckFinalIndicator("Yes")
                .setRequestId(input.getSdncRequestHeader().getRequestId()).setResponseMessage(message)
                .setResponseCode(responseCode).build();

        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setHardConstraints(input.getHardConstraints())
                // .setPceMetric(input.getPceMetric())
                .setSoftConstraints(input.getSoftConstraints())
                // .setLocallyProtectedLinks(input.isLocallyProtectedLinks())
                .build();

        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.feasibility
            .check.output.ServiceAEnd serviceAEnd = new org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.servicehandler.rev161014.service.feasibility
                .check.output.ServiceAEndBuilder(input.getServiceAEnd()).build();

        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.feasibility
            .check.output.ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.servicehandler.rev161014.service.feasibility
            .check.output.ServiceZEndBuilder(input.getServiceZEnd()).build();

        /* TxDirection. */
        Port txPort = new PortBuilder().setPortDeviceName("ROUTER_SNJSCAMCJW1_000000.00_00").setPortType("router")
                .setPortName("Gigabit Ethernet_Tx.ge-1/0/0.0").setPortRack("000000.00").setPortShelf("00").build();
        Lgx txLgx = new LgxBuilder().setLgxDeviceName("LGX Panel_SNJSCAMCJW1_000000.00_00")
                .setLgxPortName("LGX_Back.23").setLgxPortRack("000000.00").setLgxPortShelf("00").build();
        TxDirection txDirection = new TxDirectionBuilder().setPort(txPort).setLgx(txLgx).build();

        /* RxDirection. */
        Port rxPort = new PortBuilder().setPortDeviceName("ROUTER_SNJSCAMCJW1_000000.00_00").setPortType("router")
                .setPortName("Gigabit Ethernet_Rx.ge-1/0/0.0").setPortRack("000000.00").setPortShelf("00").build();
        Lgx rxLgx = new LgxBuilder().setLgxDeviceName("LGX Panel_SNJSCAMCJW1_000000.00_00").setLgxPortName("LGX_Back.6")
                .setLgxPortRack("000000.00").setLgxPortShelf("00").build();
        RxDirection rxDirection = new RxDirectionBuilder().setPort(rxPort).setLgx(rxLgx).build();

        IntermediateSites inter = new IntermediateSitesBuilder().setClli("SNJSCAMCJW1").setServiceRate((long) 100)
                .setServiceFormat(ServiceFormat.Ethernet).setOpticType(OpticTypes.Gray).setTxDirection(txDirection)
                .setRxDirection(rxDirection).build();

        List<IntermediateSites> intersites = new ArrayList<IntermediateSites>();
        intersites.add(inter);
        ServiceFeasibilityCheckOutput output = new ServiceFeasibilityCheckOutputBuilder()
                .setIntermediateSites(intersites).setResponseParameters(responseParameters)
                .setConfigurationResponseCommon(configurationResponseCommon).setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd).build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceReconfigureOutput>> serviceReconfigure(ServiceReconfigureInput input) {

        LOG.info("RPC service reconfigure received");
        notificationUrl = null;// input.getnotificationUrl();
        boolean commonId = true;
        boolean coherencyHardSoft = false;
        ServiceRpcResult notification = null;

        String message = "";
        LOG.info("checking Service Compliancy ...");
        /*
         * Upon receipt of service-create RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        compliancyCheck = new ServicehandlerCompliancyCheck(input.getServiceName(), input.getConnectionType(),
                RpcActions.ServiceReconfigure);
        if (compliancyCheck.check(true, false)) {
            LOG.info("Service compliant !");
            /*
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */
            LOG.info("checking Tx/Rx Info for AEnd ...");
            txrxCheck = new ServicehandlerTxRxCheck(input.getServiceAEnd(), 1);
            if (txrxCheck.check()) {
                LOG.info("Tx/Rx Info for AEnd checked !");
                LOG.info("checking Tx/Rx Info for ZEnd ...");
                txrxCheck = new ServicehandlerTxRxCheck(input.getServiceZEnd(), 2);
                if (txrxCheck.check()) {
                    LOG.info("Tx/Rx Info for ZEnd checked");
                    /*
                     * If OK, common-id is verified in order to see if there is
                     * no routing policy provided. If yes, the routing
                     * constraints of the policy are recovered and coherency
                     * with hard/soft constraints provided in the input of the
                     * RPC.
                     */
                    if (input.getCommonId() != null) {
                        LOG.info("Common-id specified");
                        /*
                         * Check coherency with hard/soft constraints
                         */

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
                        /*
                         * Retrieving initial service topology.
                         */
                        String serviceName = input.getServiceName();
                        Services service = readServiceList(serviceName);
                        if (service != null) {
                            LOG.debug("Service '" + serviceName + "' present in datastore !");
                            /*
                             * Sending cancel resource resv request to PCE
                             */

                            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, input, false);
                            notification = new ServiceRpcResultBuilder()
                                    .setNotificationType(ServiceNotificationTypes.ServiceReconfigureResult)
                                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                    .setStatusMessage("Cancelling ResourceResv ...").build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                            sendNotifToUrl(notification, notificationUrl);
                            FutureCallback<CancelResourceReserveOutput> pceCallback =
                                    new FutureCallback<CancelResourceReserveOutput>() {
                                String message = "";
                                String responseCode = "";
                                ServiceRpcResult notification = null;

                                @Override
                                public void onFailure(Throwable arg0) {
                                    LOG.error("Failed to cancel ResourceResv ! ");
                                    notification = new ServiceRpcResultBuilder()
                                            .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                            .setServiceName(serviceName).setStatus(RpcStatusEx.Failed)
                                            .setStatusMessage("PCR Request failed !").build();
                                    try {
                                        notificationPublishService.putNotification(notification);
                                    } catch (InterruptedException e) {
                                        LOG.info("notification offer rejected : " + e);
                                    }

                                }

                                @Override
                                public void onSuccess(CancelResourceReserveOutput arg0) {
                                    if (mappingAndSendingPCRequest.getSuccess() && arg0 != null) {
                                        LOG.info("Service ResourceResv cancelled !");
                                        notification = new ServiceRpcResultBuilder()
                                                .setNotificationType(ServiceNotificationTypes.ServiceReconfigureResult)
                                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                                .setStatusMessage(
                                                        "Service '" + serviceName + "' ResourceResv cancelled")
                                                .build();
                                        try {
                                            notificationPublishService.putNotification(notification);
                                        } catch (InterruptedException e) {
                                            LOG.info("notification offer rejected : " + e);
                                        }
                                        sendNotifToUrl(notification, notificationUrl);

                                        message = "Service '" + serviceName + "' ResourceResv cancelled";

                                        LOG.info("PCE replied to CancelResourceResv Request !");
                                        /*
                                         * Before sending the PCE request, input
                                         * data need to be formatted according
                                         * to the Service Handler - PCE
                                         * interface data model.
                                         */
                                        mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, input,
                                                true);
                                        /*
                                         * Once PCE request is being sent to the
                                         * PCE on interface B, PCE reply is
                                         * expected until a timer expires.
                                         */
                                        notification = new ServiceRpcResultBuilder()
                                                .setNotificationType(ServiceNotificationTypes.ServiceReconfigureResult)
                                                .setServiceName(serviceName).setStatus(RpcStatusEx.Pending)
                                                .setStatusMessage(message + ", submitting PathComputation Request ...")
                                                .build();
                                        try {
                                            notificationPublishService.putNotification(notification);
                                        } catch (InterruptedException e) {
                                            LOG.info("notification offer rejected : " + e);
                                        }
                                        sendNotifToUrl(notification, notificationUrl);
                                        PathComputationRequestOutput response = null;
                                        try {
                                            response = mappingAndSendingPCRequest.pathComputationRequest().get();
                                        } catch (InterruptedException | ExecutionException e2) {
                                            LOG.error(e2.getMessage());
                                        }

                                        if (mappingAndSendingPCRequest.getSuccess() && response != null) {
                                            /*
                                             * If PCE reply is received before
                                             * timer expiration with a positive
                                             * result, a service is created with
                                             * admin and operational status
                                             * 'down'.
                                             */
                                            LOG.info("PCE replied to PCR Request !");
                                            message = response.getConfigurationResponseCommon().getResponseMessage();
                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(
                                                            ServiceNotificationTypes.ServiceReconfigureResult)
                                                    .setServiceName(serviceName).setStatus(RpcStatusEx.Pending)
                                                    .setStatusMessage("PCE replied to PCR Request !").build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);
                                            /*
                                             * Send Implementation order to
                                             * renderer
                                             */
                                            mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry,
                                                    input, response);

                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(
                                                            ServiceNotificationTypes.ServiceReconfigureResult)
                                                    .setServiceName(input.getServiceName())
                                                    .setStatus(RpcStatusEx.Pending)
                                                    .setStatusMessage("Submitting ServiceImplementation Request ...")
                                                    .build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);

                                            ServiceImplementationRequestOutput siOutput = null;
                                            try {
                                                siOutput = mappingAndSendingSIRequest.serviceImplementation().get();
                                            } catch (InterruptedException | ExecutionException e2) {
                                                LOG.error(e2.getMessage());
                                            }
                                            ConfigurationResponseCommon siCommon = siOutput
                                                    .getConfigurationResponseCommon();
                                            message = siCommon.getResponseMessage();

                                            if (mappingAndSendingSIRequest.getSuccess() && siOutput != null) {
                                                message = "Service reconfigured ";
                                                LOG.info("Service reconfigured !");
                                                notification = new ServiceRpcResultBuilder()
                                                        .setNotificationType(
                                                                ServiceNotificationTypes.ServiceReconfigureResult)
                                                        .setServiceName(input.getServiceName())
                                                        .setStatus(RpcStatusEx.Pending)
                                                        .setStatusMessage("Service reconfigure !").build();
                                                try {
                                                    notificationPublishService.putNotification(notification);
                                                } catch (InterruptedException e) {
                                                    LOG.info("notification offer rejected : " + e);
                                                }
                                                sendNotifToUrl(notification, notificationUrl);
                                                /*
                                                 * Service implemented Update in
                                                 * DB.
                                                 */
                                                Boolean update = false;
                                                Boolean delete = false;
                                                Services modifService = mappingServices(null, input, response);
                                                InstanceIdentifier<Services> iid = InstanceIdentifier
                                                        .create(ServiceList.class)
                                                        .child(Services.class, new ServicesKey(serviceName));
                                                WriteTransaction writeTx = db.newWriteOnlyTransaction();
                                                writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                                                try {
                                                    LOG.info("Deleting service info ...");
                                                    writeTx.submit().checkedGet();
                                                    delete = true;
                                                } catch (TransactionCommitFailedException e) {
                                                    LOG.error("Failed to delete service from Service List");
                                                }
                                                if (delete) {
                                                    iid = InstanceIdentifier.create(ServiceList.class).child(
                                                            Services.class, new ServicesKey(input.getNewServiceName()));
                                                    writeTx = db.newWriteOnlyTransaction();
                                                    writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, modifService);
                                                    try {
                                                        LOG.info("Updating service info ...");
                                                        writeTx.submit().checkedGet();
                                                        update = true;
                                                    } catch (TransactionCommitFailedException e) {
                                                        LOG.error("Failed to modify service from Service List");
                                                    }
                                                }
                                                if (update) {
                                                    LOG.info("Service '" + serviceName + "' updated with new name '"
                                                            + input.getNewServiceName() + "' ! ");
                                                    StringBuilder build = new StringBuilder();
                                                    build.append(message);
                                                    build.append(" : Service updated on DataBase !");
                                                    message = build.toString();
                                                } else {
                                                    LOG.info("Service '" + serviceName + "' update failed  ! ");
                                                    StringBuilder build = new StringBuilder();
                                                    build.append(message);
                                                    build.append(" : Failed to modify service from Service List ");
                                                    message = build.toString();
                                                }
                                            } else {
                                                LOG.info("Service not implemented !");
                                                message = response.getConfigurationResponseCommon()
                                                        .getResponseMessage();
                                                notification = new ServiceRpcResultBuilder()
                                                        .setNotificationType(
                                                                ServiceNotificationTypes.ServiceReconfigureResult)
                                                        .setServiceName(input.getServiceName())
                                                        .setStatus(RpcStatusEx.Failed)
                                                        .setStatusMessage(
                                                                "Service not implemented, cancelling ResourceResv")
                                                        .build();
                                                try {
                                                    notificationPublishService.putNotification(notification);
                                                } catch (InterruptedException e) {
                                                    LOG.info("notification offer rejected : " + e);
                                                }
                                                sendNotifToUrl(notification, notificationUrl);
                                                mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry,
                                                        input, false);
                                                /*
                                                 * Send Cancel resource Request
                                                 * to PCE.
                                                 */
                                                CancelResourceReserveOutput cancelOuptut = null;
                                                try {
                                                    cancelOuptut = mappingAndSendingPCRequest.cancelResourceReserve()
                                                            .get();
                                                } catch (InterruptedException | ExecutionException e1) {
                                                    LOG.error(e1.getMessage());
                                                }
                                                if (mappingAndSendingPCRequest.getSuccess() && cancelOuptut != null) {
                                                    LOG.info("Service ResourceResv cancelled !");
                                                    message = response.getConfigurationResponseCommon()
                                                            .getResponseMessage();
                                                    notification = new ServiceRpcResultBuilder()
                                                            .setNotificationType(
                                                                    ServiceNotificationTypes.ServiceReconfigureResult)
                                                            .setServiceName(input.getServiceName())
                                                            .setStatus(RpcStatusEx.Failed)
                                                            .setStatusMessage("Service ResourceResv cancelled").build();
                                                    try {
                                                        notificationPublishService.putNotification(notification);
                                                    } catch (InterruptedException e) {
                                                        LOG.info("notification offer rejected : " + e);
                                                    }
                                                    sendNotifToUrl(notification, notificationUrl);

                                                    message = cancelOuptut.getConfigurationResponseCommon()
                                                            .getResponseMessage();

                                                    StringBuilder build = new StringBuilder();
                                                    build.append("Service not implemented - ");
                                                    build.append(message);
                                                    message = build.toString();

                                                    LOG.info("PCE replied to CancelResourceResv Request !");
                                                } else {
                                                    message = "Cancelling Resource reserved failed ";
                                                    LOG.info(message);
                                                    StringBuilder build = new StringBuilder();
                                                    build.append("Service not implemented - ");
                                                    build.append(message);
                                                    message = build.toString();

                                                    message = response.getConfigurationResponseCommon()
                                                            .getResponseMessage();
                                                    notification = new ServiceRpcResultBuilder()
                                                            .setNotificationType(
                                                                    ServiceNotificationTypes.ServiceReconfigureResult)
                                                            .setServiceName(input.getServiceName())
                                                            .setStatus(RpcStatusEx.Failed)
                                                            .setStatusMessage("Cancelling Resource reserved failed")
                                                            .build();
                                                    try {
                                                        notificationPublishService.putNotification(notification);
                                                    } catch (InterruptedException e) {
                                                        LOG.info("notification offer rejected : " + e);
                                                    }
                                                    sendNotifToUrl(notification, notificationUrl);
                                                }
                                            }

                                        } else {
                                            LOG.error("PCE pathcomputation request failed !");
                                            message = "PCE pathcomputation request failed : "
                                                    + mappingAndSendingPCRequest.getError();// "Path
                                                                                            // not
                                                                                            // calculated";
                                        }
                                    } else {
                                        message = "Cancelling Resource reserved failed ";
                                        LOG.info(message);
                                        StringBuilder build = new StringBuilder();
                                        build.append("Service not implemented - ");
                                        build.append(message);
                                        message = build.toString();

                                        notification = new ServiceRpcResultBuilder()
                                                .setNotificationType(ServiceNotificationTypes.ServiceReconfigureResult)
                                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                                                .setStatusMessage("Cancelling Resource reserved failed").build();
                                        try {
                                            notificationPublishService.putNotification(notification);
                                        } catch (InterruptedException e) {
                                            LOG.info("notification offer rejected : " + e);
                                        }
                                        sendNotifToUrl(notification, notificationUrl);
                                    }

                                }
                            };
                            ListenableFuture<CancelResourceReserveOutput> pce = mappingAndSendingPCRequest
                                    .cancelResourceReserve();
                            Futures.addCallback(pce, pceCallback);
                            LOG.info("CancelResRev Request in progress ");
                            ServiceReconfigureOutput output = new ServiceReconfigureOutputBuilder()
                                    .setStatus(RpcStatusEx.Pending).setStatusMessage(message).build();

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
        }

        ServiceReconfigureOutput output = new ServiceReconfigureOutputBuilder().setStatus(RpcStatusEx.Successful)
                .setStatusMessage(message).build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceRestorationOutput>> serviceRestoration(ServiceRestorationInput input) {
        LOG.info("RPC service restoration received");
        ServiceRpcResult notification = null;
        notificationUrl = null;// input.getnotificationUrl();
        String message = "";
        LOG.info("checking Service Compliancy ...");
        compliancyCheck = new ServicehandlerCompliancyCheck(input.getServiceName(), RpcActions.ServiceRestoration);
        if (compliancyCheck.check(false, false)) {
            LOG.info("Service compliant !");
            /*
             * If compliant, Getting path from service DB.
             */

            String serviceName = input.getServiceName();
            Services service = readServiceList(serviceName);
            if (service != null) {
                LOG.debug("Service '" + serviceName + "' present in datastore !");
                notification = new ServiceRpcResultBuilder()
                        .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                        .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                        .setStatusMessage("Service '" + serviceName + "' present in datastore, deleting service ...")
                        .build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info("notification offer rejected : " + e);
                }
                sendNotifToUrl(notification, notificationUrl);
                /*
                 * Sending delete order to renderer
                 */
                mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, null, input.getServiceName());

                ListenableFuture<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                    .stubrenderer.rev170426.ServiceDeleteOutput> renderer =
                    mappingAndSendingSIRequest.serviceDelete();
                FutureCallback<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                    .stubrenderer.rev170426.ServiceDeleteOutput> rendererCallback =
                    new FutureCallback<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                    .stubrenderer.rev170426.ServiceDeleteOutput>() {
                    String message = "";
                    String responseCode = "";
                    ServiceRpcResult notification = null;

                    @Override
                    public void onFailure(Throwable arg0) {
                        LOG.error("ServiceDelete Request failed !");
                        notification = new ServiceRpcResultBuilder()
                                .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                .setServiceName(serviceName).setStatus(RpcStatusEx.Failed)
                                .setStatusMessage("ServiceDelete Request failed !").build();
                        try {
                            notificationPublishService.putNotification(notification);
                        } catch (InterruptedException e) {
                            LOG.info("notification offer rejected : " + e);
                        }
                        sendNotifToUrl(notification, notificationUrl);
                    }

                    @Override
                    public void onSuccess(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                            .stubrenderer.rev170426.ServiceDeleteOutput arg0) {
                        if (arg0 != null) {
                            message = arg0.getConfigurationResponseCommon().getResponseMessage();
                            notification = new ServiceRpcResultBuilder()
                                    .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                    .setStatusMessage("Service deleted !").build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                            sendNotifToUrl(notification, notificationUrl);
                            mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry, service, true);
                            /*
                             * Once PCE request is being sent to the PCE on
                             * interface B, PCE reply is expected until a timer
                             * expires.
                             */
                            notification = new ServiceRpcResultBuilder()
                                    .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                    .setStatusMessage("Service deleted, submitting PathComputation Request ...")
                                    .build();
                            try {
                                notificationPublishService.putNotification(notification);
                            } catch (InterruptedException e) {
                                LOG.info("notification offer rejected : " + e);
                            }
                            sendNotifToUrl(notification, notificationUrl);
                            PathComputationRequestOutput response = null;
                            try {
                                response = mappingAndSendingPCRequest.pathComputationRequest().get();
                            } catch (InterruptedException | ExecutionException e2) {
                                LOG.error(e2.getMessage());
                            }

                            if (mappingAndSendingPCRequest.getSuccess() && response != null) {
                                /*
                                 * If PCE reply is received before timer
                                 * expiration with a positive result, a service
                                 * is created with admin and operational status
                                 * 'down'.
                                 */
                                LOG.info("Path calculated !");
                                message = response.getConfigurationResponseCommon().getResponseMessage();
                                notification = new ServiceRpcResultBuilder()
                                        .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                        .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                        .setStatusMessage("Path calculated, modifying Service Admin / Op ...").build();
                                try {
                                    notificationPublishService.putNotification(notification);
                                } catch (InterruptedException e) {
                                    LOG.info("notification offer rejected : " + e);
                                }
                                sendNotifToUrl(notification, notificationUrl);

                                /*
                                 * creating Service with Admin / Op to down.
                                 *
                                 */

                                ServicesBuilder serviceRestoration = new ServicesBuilder(service)
                                        .setAdministrativeState(State.OutOfService)
                                        .setOperationalState(State.OutOfService)
                                        .setLifecycleState(LifecycleState.Planned);

                                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                                    .service.types.rev170426.response
                                        .parameters.sp.ResponseParameters responseParameters =
                                        response.getResponseParameters();
                                if (responseParameters != null) {
                                    // serviceRestoration.setPceMetric(responseParameters.getPceMetric());
                                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                                        .service.types.rev170426.response.parameters.sp.response
                                            .parameters.PathDescription pathDescription =
                                            responseParameters.getPathDescription();
                                    if (pathDescription != null) {
                                        List<AToZ> atozList = new ArrayList<AToZ>();
                                        List<ZToA> ztoaList = new ArrayList<ZToA>();

                                        for (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                                                    .pathdescription.rev170426.path.description.atoz.direction.AToZ
                                                    tmp : pathDescription.getAToZDirection().getAToZ()) {

                                            AToZKey key = new AToZKey(tmp.getKey().getId());
                                            tmp.getResource().getResource();
                                            AToZ atoz = new AToZBuilder().setId(tmp.getId()).setKey(key)
                                                    // .setResource(tmp.getResource())
                                                    .build();
                                            atozList.add(atoz);
                                        }

                                        for (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                                                    .pathdescription.rev170426.path.description.ztoa.direction.ZToA
                                                    tmp : pathDescription.getZToADirection().getZToA()) {
                                            ZToAKey key = new ZToAKey(tmp.getKey().getId());
                                            ZToA ztoa = new ZToABuilder().setId(tmp.getId()).setKey(key)
                                                    // .setResource(tmp.getResource())
                                                    .build();
                                            ztoaList.add(ztoa);
                                        }
                                        Topology topology = new TopologyBuilder().setAToZ(atozList).setZToA(ztoaList)
                                                .build();
                                        serviceRestoration.setTopology(topology);
                                    }
                                }
                                Boolean update = false;
                                InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                                        .child(Services.class, new ServicesKey(serviceName));
                                WriteTransaction writeTx = db.newWriteOnlyTransaction();
                                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, service);

                                try {
                                    writeTx.submit().checkedGet();
                                    update = true;
                                } catch (TransactionCommitFailedException e) {
                                    LOG.error("Failed to modify service from Service List");
                                }
                                if (update) {
                                    LOG.info("Service modified !");
                                    /*
                                     * Send Implementation order to renderer
                                     */
                                    mappingAndSendingSIRequest = new MappingAndSendingSIRequest(rpcRegistry, service);

                                    notification = new ServiceRpcResultBuilder()
                                            .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                            .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                            .setStatusMessage(
                                                    "Service modified, submitting ServiceImplementation Request")
                                            .build();
                                    try {
                                        notificationPublishService.putNotification(notification);
                                    } catch (InterruptedException e) {
                                        LOG.info("notification offer rejected : " + e);
                                    }
                                    sendNotifToUrl(notification, notificationUrl);

                                    ServiceImplementationRequestOutput siOutput = null;
                                    try {
                                        siOutput = mappingAndSendingSIRequest.serviceImplementation().get();
                                    } catch (InterruptedException | ExecutionException e2) {
                                        LOG.error(e2.getMessage());
                                    }
                                    if (mappingAndSendingSIRequest.getSuccess() && siOutput != null) {
                                        ConfigurationResponseCommon siCommon = siOutput
                                                .getConfigurationResponseCommon();
                                        message = siCommon.getResponseMessage();
                                        LOG.info("Service restored !");
                                        notification = new ServiceRpcResultBuilder()
                                                .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Pending)
                                                .setStatusMessage("Service restored !").build();
                                        try {
                                            notificationPublishService.putNotification(notification);
                                        } catch (InterruptedException e) {
                                            LOG.info("notification offer rejected : " + e);
                                        }
                                        sendNotifToUrl(notification, notificationUrl);
                                        /*
                                         * Service implemented setting Service
                                         * op status to up
                                         */
                                        if (writeOrModifyOrDeleteServiceList(serviceName, null, null, 0) == null) {
                                            message = "Service restored : Service Op Status changed to Up !";
                                        } else {
                                            message = "Service restored : "
                                                    + "but Failed to modify service from Service List !";
                                        }
                                    } else {
                                        LOG.info("Service not restored !");
                                        message = response.getConfigurationResponseCommon().getResponseMessage();
                                        notification = new ServiceRpcResultBuilder()
                                                .setNotificationType(ServiceNotificationTypes.ServiceRestorationResult)
                                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                                                .setStatusMessage("Service not restored, cancelling ResourceResv ...")
                                                .build();
                                        try {
                                            notificationPublishService.putNotification(notification);
                                        } catch (InterruptedException e) {
                                            LOG.info("notification offer rejected : " + e);
                                        }
                                        sendNotifToUrl(notification, notificationUrl);
                                        mappingAndSendingPCRequest = new MappingAndSendingPCRequest(rpcRegistry,
                                                service, false);
                                        /*
                                         * Send Cancel resource Request to PCE.
                                         */
                                        CancelResourceReserveOutput cancelOuptut = null;
                                        try {
                                            cancelOuptut = mappingAndSendingPCRequest.cancelResourceReserve().get();
                                        } catch (InterruptedException | ExecutionException e1) {
                                            LOG.error(e1.getMessage());
                                        }
                                        if (mappingAndSendingPCRequest.getSuccess() && cancelOuptut != null) {
                                            LOG.info("Service ResourceResv cancelled !");
                                            message = response.getConfigurationResponseCommon().getResponseMessage();
                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(
                                                            ServiceNotificationTypes.ServiceRestorationResult)
                                                    .setServiceName(input.getServiceName())
                                                    .setStatus(RpcStatusEx.Pending)
                                                    .setStatusMessage("Service ResourceResv cancelled").build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);
                                            message = cancelOuptut.getConfigurationResponseCommon()
                                                    .getResponseMessage();
                                            StringBuilder build = new StringBuilder();
                                            build.append("Service not implemented - ");
                                            build.append(message);
                                            message = build.toString();
                                            LOG.info("PCE replied to CancelResourceResv Request !");
                                        } else {
                                            message = "Cancelling Resource reserved failed ";
                                            LOG.info(message);
                                            StringBuilder build = new StringBuilder();
                                            build.append("Service not implemented - ");
                                            build.append(message);
                                            message = build.toString();
                                            message = response.getConfigurationResponseCommon().getResponseMessage();
                                            notification = new ServiceRpcResultBuilder()
                                                    .setNotificationType(ServiceNotificationTypes.ServiceCreateResult)
                                                    .setServiceName(input.getServiceName())
                                                    .setStatus(RpcStatusEx.Failed)
                                                    .setStatusMessage("Cancelling Resource reserved failed").build();
                                            try {
                                                notificationPublishService.putNotification(notification);
                                            } catch (InterruptedException e) {
                                                LOG.info("notification offer rejected : " + e);
                                            }
                                            sendNotifToUrl(notification, notificationUrl);
                                        }
                                    }
                                } else {
                                    LOG.error("Failed to modify service from service list !");
                                }
                            } else {
                                message = mappingAndSendingPCRequest.getError();
                                /* Path not calculated. */
                                LOG.error("Path Computation request failed : " + message);
                            }
                        } else {
                            message = "deleting service failed";
                            LOG.error(message);
                        }
                    }
                };
                Futures.addCallback(renderer, rendererCallback);
                ServiceRestorationOutput output = new ServiceRestorationOutputBuilder().setStatus(RpcStatusEx.Pending)
                        .setStatusMessage(message).build();

                return RpcResultBuilder.success(output).buildFuture();

            } else {
                message = "Service '" + serviceName + "' not exists in datastore";
                LOG.error(message);
            }

        } else {
            message = compliancyCheck.getMessage();
            LOG.error(message);
        }

        ServiceRestorationOutput output = new ServiceRestorationOutputBuilder().setStatus(RpcStatusEx.Successful)
                .setStatusMessage(message).build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    /*
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
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<>("Failed to create Service List", LOG));
    }

    /*
     * Map Input (ServiceCreateInmput, ServiceReconfigureInput) & output
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
                    .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);

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

    /*
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
        CheckedFuture<Optional<Services>, ReadFailedException> future = readTx.read(LogicalDatastoreType.OPERATIONAL,
                iid);
        Optional<Services> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Reading service failed:", e);
        }
        if (optional.isPresent()) {
            LOG.debug("Service '" + serviceName + "' present !");
            result = new ServicesBuilder(optional.get()).build();
        }
        return result;
    }

    /*
     * Write or Modify or Delete Service from/to SreviceList.
     *
     * @param serviceName
     *            Name of service
     * @param input
     *            ServiceCreateInput
     * @param output
     *            PathComputationRequestOutput
     * @param choice
     *            0 - Modify 1 - Delete 2 - Write
     * @return String operations result, null if ok or not otherwise
     */
    private String writeOrModifyOrDeleteServiceList(String serviceName, ServiceCreateInput input,
            PathComputationRequestOutput output, int choice) {
        LOG.debug("WriteOrModifyOrDeleting '" + serviceName + "' Service");
        WriteTransaction writeTx = db.newWriteOnlyTransaction();
        String result = null;
        Services readService = readServiceList(serviceName);
        if (readService != null) {
            /*
             * Modify / Delete Service.
             */
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                    new ServicesKey(serviceName));
            ServicesBuilder service = new ServicesBuilder(readService);

            String action = null;
            switch (choice) {
                case 0: /* Modify. */
                    LOG.debug("Modifying '" + serviceName + "' Service");
                    service.setOperationalState(State.InService).setAdministrativeState(State.InService);
                    writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, service.build());
                    action = "modify";
                    break;

                case 1: /* Delete */
                    LOG.debug("Deleting '" + serviceName + "' Service");
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    action = "delete";
                    break;

                default:
                    LOG.debug("No choice found");
                    break;

            }
            try {
                writeTx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Failed to " + action + " service from Service List");
                result = "Failed to " + action + " service from Service List";
            }
        } else {
            if (choice == 2) { /* Write Service */
                LOG.debug("Writing '" + serviceName + "' Service");
                InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                        new ServicesKey(serviceName));

                Services service = mappingServices(input, null, output);
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
                try {
                    writeTx.submit().checkedGet();
                    result = null;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Failed to write service to Service List");
                    result = "Failed to write service to Service List";
                }
            } else {
                LOG.info("Service is not present ! ");
                result = "Service is not present ! ";
            }
        }
        return result;
    }

    private void sendNotifToUrl(ServiceRpcResult notification, String url) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                // .serializeNulls()
                .create();
        String data = gson.toJson(notification);
        URL obj;
        try {
            obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            LOG.info("Response Code : " + responseCode);
        } catch (IOException e) {
            LOG.error("IOException : " + e.toString());
        }

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
    public Future<RpcResult<TempServiceDeleteOutput>> tempServiceDelete(TempServiceDeleteInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<TempServiceCreateOutput>> tempServiceCreate(TempServiceCreateInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onServiceRpcResultSp(ServiceRpcResultSp notification) {
        if (!compareServiceRpcResultSp(notification)) {
            serviceRpcResultSp = notification;
            StringBuilder build = new StringBuilder();
            build.append(
                    "Received '" + notification.getNotificationType() + "' StubRenderer notification" + "from service '"
                            + notification.getServiceName() + "' " + "with status '" + notification.getStatus() + "'");
            build.append(" with StatusMessage '" + notification.getStatusMessage() + "'");
            if (notification.getStatus() == RpcStatusEx.Successful && notification.getNotificationType()
                    .getIntValue() == ServicePathNotificationTypes.ServiceImplementationRequest.getIntValue()) {
                build.append(" PathTopology : " + notification.getPathTopology().toString());
            }
            LOG.info(build.toString());
        } else {
            LOG.info("ServicePathRpcResult already wired !");
        }

    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (!compareServicePathRpcResult(notification)) {
            servicePathRpcResult = notification;
            StringBuilder build = new StringBuilder();
            build.append(
                    "Received '" + notification.getNotificationType() + "' StubPce notification " + "from service '"
                            + notification.getServiceName() + "' " + "with status '" + notification.getStatus() + "'");
            build.append(" with StatusMessage '" + notification.getStatusMessage() + "'");
            if (notification.getStatus() == RpcStatusEx.Successful && notification.getNotificationType()
                    .getIntValue() == ServicePathNotificationTypes.PathComputationRequest.getIntValue()) {
                build.append(" PathDescription : " + notification.getPathDescription().toString());
                /*
                 * switch (action.getIntValue()) { case 1: //service-create case
                 * 3: //service-delete case 8: //service-reconfigure case 9:
                 * //service-restoration case 10://service-reversion case
                 * 11://service-reroute break;
                 *
                 * default: break; }
                 */
            }

            LOG.info(build.toString());
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
}
