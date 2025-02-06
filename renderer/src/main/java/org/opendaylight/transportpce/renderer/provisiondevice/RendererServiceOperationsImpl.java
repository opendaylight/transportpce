/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.ServicePathInputData;
import org.opendaylight.transportpce.renderer.provisiondevice.notification.Notification;
import org.opendaylight.transportpce.renderer.provisiondevice.result.Message;
import org.opendaylight.transportpce.renderer.provisiondevice.result.WeightedResultMessage;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.DeviceRenderingRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.DeviceRenderingTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.NetworkDeviceRenderingRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerSetupRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerSetupTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerTurnDownTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OtnDeviceRenderingTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.RollbackProcessor;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.RollbackResultMessage;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.history.History;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.history.TransactionHistory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPm;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndown;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.link._for.notif.ATerminationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.link._for.notif.ZTerminationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.Nodes;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate = true)
public class RendererServiceOperationsImpl implements RendererServiceOperations {

    private static final Logger LOG = LoggerFactory.getLogger(RendererServiceOperationsImpl.class);
    private static final String DEVICE_RENDERING_ROLL_BACK_MSG =
            "Device rendering was not successful! Rendering will be rolled back.";
    private static final String OLM_ROLL_BACK_MSG =
            "OLM power setup was not successful! Rendering and OLM will be rolled back.";
    private static final String RENDERING_DEVICES_A_Z_MSG = "Rendering devices A-Z";
    private static final String RENDERING_DEVICES_Z_A_MSG = "Rendering device Z-A";
    private static final String ATOZPATH = "A-to-Z";
    private static final String ZTOAPATH = "Z-to-A";
    private static final String OPERATION_FAILED = "Operation Failed";
    private static final String OPERATION_SUCCESSFUL = "Operation Successful";
    private static final int NUMBER_OF_THREADS = 4;

    private final DeviceRendererService deviceRenderer;
    private final OtnDeviceRendererService otnDeviceRenderer;
    private final DataBroker dataBroker;
    private final Notification notification;
    private final PortMapping portMapping;
    private final RpcService rpcService;
    private ListeningExecutorService executor;

    @Activate
    public RendererServiceOperationsImpl(@Reference DeviceRendererService deviceRenderer,
            @Reference OtnDeviceRendererService otnDeviceRenderer,
            @Reference DataBroker dataBroker,
            @Reference Notification notification,
            @Reference PortMapping portMapping,
            @Reference RpcService rpcService) {
        this.deviceRenderer = deviceRenderer;
        this.otnDeviceRenderer = otnDeviceRenderer;
        this.dataBroker = dataBroker;
        this.notification = notification;
        this.portMapping = portMapping;
        this.rpcService = rpcService;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
        LOG.debug("RendererServiceOperationsImpl instantiated");
    }

    @Override
    public ListenableFuture<ServiceImplementationRequestOutput>
            serviceImplementation(ServiceImplementationRequestInput input, boolean isTempService) {
        LOG.info("Calling service impl request {}", input.getServiceName());
        LOG.debug("Check if it is temp-service {}", isTempService);
        return executor.submit(new Callable<ServiceImplementationRequestOutput>() {

            @Override
            public ServiceImplementationRequestOutput call() throws Exception {
                sendNotifications(
                    ServicePathNotificationTypes.ServiceImplementationRequest,
                    input.getServiceName(),
                    RpcStatusEx.Pending,
                    "Service compliant, submitting service implementation Request ...");
                Uint32 serviceRate = getServiceRate(input);
                LOG.info("Using {}G rate", serviceRate);
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115
                        .network.Nodes mappingNode =
                    portMapping.isNodeExist(input.getServiceAEnd().getNodeId())
                        ? portMapping.getNode(input.getServiceAEnd().getNodeId())
                        : null;
                String serviceType = ServiceTypes.getServiceType(
                    input.getServiceAEnd().getServiceFormat().getName(),
                    serviceRate,
                    mappingNode != null
                        && NodeTypes.Xpdr.equals(mappingNode.getNodeInfo().getNodeType())
                            && input.getServiceAEnd().getTxDirection() != null
                            && input.getServiceAEnd().getTxDirection().getPort() != null
                            && input.getServiceAEnd().getTxDirection().getPort().getPortName() != null
                        ? portMapping.getMapping(input.getServiceAEnd().getNodeId(),
                                input.getServiceAEnd().getTxDirection().getPort().getPortName())
                        : null);
                //TODO a Map might be more indicated here
                switch (serviceType) {
                    case StringConstants.SERVICE_TYPE_100GE_T:
                    case StringConstants.SERVICE_TYPE_400GE:
                    case StringConstants.SERVICE_TYPE_OTU4:
                    case StringConstants.SERVICE_TYPE_OTUC2:
                    case StringConstants.SERVICE_TYPE_OTUC3:
                    case StringConstants.SERVICE_TYPE_OTUC4:
                    case StringConstants.SERVICE_TYPE_OTHER:
                        LOG.debug("Check temp service {}", isTempService);
                        if (!manageServicePathCreation(input, serviceType, isTempService)) {
                            return ModelMappingUtils
                                .createServiceImplResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                        }
                        break;
                    case StringConstants.SERVICE_TYPE_1GE:
                    case StringConstants.SERVICE_TYPE_10GE:
                    case StringConstants.SERVICE_TYPE_100GE_M:
                    case StringConstants.SERVICE_TYPE_100GE_S:
                    case StringConstants.SERVICE_TYPE_ODU4:
                    case StringConstants.SERVICE_TYPE_ODUC2:
                    case StringConstants.SERVICE_TYPE_ODUC3:
                    case StringConstants.SERVICE_TYPE_ODUC4:
                        if (!manageOtnServicePathCreation(input, serviceType, serviceRate)) {
                            return ModelMappingUtils
                                .createServiceImplResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                        }
                        break;
                    default:
                        LOG.error("unsupported service-type");
                        return ModelMappingUtils
                            .createServiceImplResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                }
                return ModelMappingUtils
                    .createServiceImplResponse(ResponseCodes.RESPONSE_OK, OPERATION_SUCCESSFUL);
            }
        });
    }

    @Override
    public ListenableFuture<ServiceDeleteOutput> serviceDelete(ServiceDeleteInput input, Services service) {
        String serviceName = input.getServiceName();
        LOG.info("Calling service delete request {}", serviceName);
        return executor.submit(new Callable<ServiceDeleteOutput>() {

            @Override
            public ServiceDeleteOutput call() throws Exception {
                sendNotifications(
                    ServicePathNotificationTypes.ServiceDelete,
                    serviceName,
                    RpcStatusEx.Pending,
                    "Service compliant, submitting service delete Request ...");
                // Obtain path description
                Optional<
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                        .service.path.PathDescription> pathDescriptionOpt =
                    getPathDescriptionFromDatastore(serviceName);
                if (pathDescriptionOpt.isEmpty()) {
                    LOG.error("Unable to get path description for service {}!", serviceName);
                    sendNotifications(
                        ServicePathNotificationTypes.ServiceDelete,
                        serviceName,
                        RpcStatusEx.Failed,
                        "Unable to get path description for service");
                    return ModelMappingUtils
                        .createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                }
                PathDescription pathDescription = pathDescriptionOpt.orElseThrow();
                String serviceType =
                    ServiceTypes.getServiceType(
                        service.getServiceAEnd().getServiceFormat().getName(),
                        service.getServiceAEnd().getServiceRate(),
                        service.getServiceAEnd().getTxDirection() == null
                                || service.getServiceAEnd().getTxDirection().values().stream().findFirst().orElseThrow()
                                    .getPort() == null
                                || service.getServiceAEnd().getTxDirection().values().stream().findFirst().orElseThrow()
                                    .getPort().getPortName() == null
                            ? null
                            : portMapping.getMapping(
                                    service.getServiceAEnd().getNodeId().getValue(),
                                    service.getServiceAEnd().getTxDirection().values().stream().findFirst()
                                        .orElseThrow().getPort().getPortName()));
                switch (serviceType) {
                    case StringConstants.SERVICE_TYPE_100GE_T:
                    case StringConstants.SERVICE_TYPE_400GE:
                    case StringConstants.SERVICE_TYPE_OTU4:
                    case StringConstants.SERVICE_TYPE_OTUC2:
                    case StringConstants.SERVICE_TYPE_OTUC3:
                    case StringConstants.SERVICE_TYPE_OTUC4:
                    case StringConstants.SERVICE_TYPE_OTHER:
                        if (!manageServicePathDeletion(serviceName, pathDescription, serviceType)) {
                            return ModelMappingUtils
                                .createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                        }
                        break;
                    case StringConstants.SERVICE_TYPE_1GE:
                    case StringConstants.SERVICE_TYPE_10GE:
                    case StringConstants.SERVICE_TYPE_100GE_M:
                    case StringConstants.SERVICE_TYPE_100GE_S:
                    case StringConstants.SERVICE_TYPE_ODU4:
                    case StringConstants.SERVICE_TYPE_ODUC2:
                    case StringConstants.SERVICE_TYPE_ODUC3:
                    case StringConstants.SERVICE_TYPE_ODUC4:
                        if (!manageOtnServicePathDeletion(serviceName, pathDescription, service, serviceType)) {
                            return ModelMappingUtils
                                .createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                        }
                        break;
                    default:
                        LOG.error("unsupported service-type");
                        return ModelMappingUtils
                            .createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED, OPERATION_FAILED);
                }
                return ModelMappingUtils
                    .createServiceDeleteResponse(ResponseCodes.RESPONSE_OK, OPERATION_SUCCESSFUL);
            }
        });
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private Uint32 getServiceRate(ServiceImplementationRequestInput input) {
        if (input.getServiceAEnd() == null) {
            LOG.warn("Unable to get service-rate for service {}", input.getServiceName());
            return Uint32.ZERO;
        }
        if (input.getServiceAEnd().getServiceRate() != null) {
            return input.getServiceAEnd().getServiceRate();
        }
        LOG.warn("Input should have rate if you are using 200 or 300G");
        // TODO: missing 200, and 300G rates here, OTUCn cannot always be 400G
        Map<ServiceFormat, Map<String, Uint32>> formatRateMap  = Map.of(
                ServiceFormat.OTU, Map.of(
                    "OTUCn", Uint32.valueOf(400),
                    "OTU4", Uint32.valueOf(100),
                    "OTU2", Uint32.valueOf(10),
                    "OTU2e", Uint32.valueOf(10)),
                ServiceFormat.ODU, Map.of(
                    "ODUCn",Uint32.valueOf(400),
                    "ODU4", Uint32.valueOf(100),
                    "ODU2", Uint32.valueOf(10),
                    "ODU2e", Uint32.valueOf(10),
                    "ODU0", Uint32.valueOf(1)));
        if (!formatRateMap.containsKey(input.getServiceAEnd().getServiceFormat())) {
            LOG.warn("Unable to get service-rate for service {} - unsupported service format {}",
                input.getServiceName(), input.getServiceAEnd().getServiceFormat());
            return Uint32.ZERO;
        }
        String serviceName =
            ServiceFormat.OTU.getName().equals(input.getServiceAEnd().getServiceFormat().getName())
                ? input.getServiceAEnd().getOtuServiceRate().toString().split("\\{")[0]
                : input.getServiceAEnd().getOduServiceRate().toString().split("\\{")[0];
        if (!formatRateMap.get(input.getServiceAEnd().getServiceFormat()).containsKey(serviceName)) {
            LOG.warn("Unable to get service-rate for service {} - unsupported service name {}",
                input.getServiceName(), serviceName);
            return Uint32.ZERO;
        }
        return formatRateMap
            .get(input.getServiceAEnd().getServiceFormat())
            .get(serviceName);
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private Optional<org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
            .service.path.PathDescription> getPathDescriptionFromDatastore(String serviceName) {
        DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                    .service.path.PathDescription> pathDescriptionIID = DataObjectIdentifier
                .builder(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName))
                .child(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                    .service.path.PathDescription.class)
                .build();
        try {
            LOG.debug("Getting path description for service {}", serviceName);
            return this.dataBroker.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.OPERATIONAL, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting path description from datastore {} for service {}!",
                    pathDescriptionIID, serviceName, e);
            return Optional.empty();
        }
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private List<DeviceRenderingResult> deviceRendering(
            RollbackProcessor rollbackProcessor,
            ServicePathInputData servicePathDataAtoZ,
            ServicePathInputData servicePathDataZtoA) {

        //TODO atozrenderingFuture & ztoarenderingFuture & renderingCombinedFuture used only once
        //     Do notifications & LOG.info deserve this ?
        LOG.info(RENDERING_DEVICES_A_Z_MSG);
        sendNotifications(
            ServicePathNotificationTypes.ServiceImplementationRequest,
            servicePathDataAtoZ.getServicePathInput().getServiceName(),
            RpcStatusEx.Pending,
            RENDERING_DEVICES_A_Z_MSG);

        History transactionHistory = new TransactionHistory();
        ListenableFuture<DeviceRenderingResult> atozrenderingFuture =
            this.executor.submit(
                new DeviceRenderingTask(this.deviceRenderer, servicePathDataAtoZ, ServicePathDirection.A_TO_Z,
                        transactionHistory));

        LOG.info(RENDERING_DEVICES_Z_A_MSG);
        sendNotifications(
            ServicePathNotificationTypes.ServiceImplementationRequest,
            servicePathDataZtoA.getServicePathInput().getServiceName(),
            RpcStatusEx.Pending,
            RENDERING_DEVICES_Z_A_MSG);
        ListenableFuture<DeviceRenderingResult> ztoarenderingFuture =
            this.executor.submit(
                new DeviceRenderingTask(this.deviceRenderer, servicePathDataZtoA, ServicePathDirection.Z_TO_A,
                        transactionHistory));

        ListenableFuture<List<DeviceRenderingResult>> renderingCombinedFuture =
            Futures.allAsList(atozrenderingFuture, ztoarenderingFuture);

        List<DeviceRenderingResult> renderingResults = new ArrayList<>(2);
        try {
            LOG.info("Waiting for A-Z and Z-A device renderers ...");
            renderingResults = renderingCombinedFuture.get(Timeouts.RENDERING_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(DEVICE_RENDERING_ROLL_BACK_MSG, e);
            sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                servicePathDataAtoZ.getServicePathInput().getServiceName(),
                RpcStatusEx.Pending,
                DEVICE_RENDERING_ROLL_BACK_MSG);
            //FIXME we can't do rollback here, because we don't have rendering results.
            return renderingResults;
        }

        rollbackProcessor.addTask(
            new NetworkDeviceRenderingRollbackTask(
                "RollbackTransactionHistoryTask",
                transactionHistory,
                ! (renderingResults.get(0).isSuccess() && renderingResults.get(1).isSuccess()),
                deviceRenderer,
                new RollbackResultMessage()
            )
        );

        return renderingResults;
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private List<OtnDeviceRenderingResult> otnDeviceRendering(
            RollbackProcessor rollbackProcessor,
            OtnServicePathInput otnServicePathAtoZ,
            OtnServicePathInput otnServicePathZtoA,
            String serviceType) {

        //TODO atozrenderingFuture & ztoarenderingFuture & renderingCombinedFuture used only once
        //     Do notifications & LOG.info deserve this ?
        LOG.info(RENDERING_DEVICES_A_Z_MSG);
        sendNotifications(
            ServicePathNotificationTypes.ServiceImplementationRequest,
            otnServicePathAtoZ.getServiceName(),
            RpcStatusEx.Pending,
            RENDERING_DEVICES_A_Z_MSG);
        ListenableFuture<OtnDeviceRenderingResult> atozrenderingFuture =
            this.executor.submit(
                new OtnDeviceRenderingTask(this.otnDeviceRenderer, otnServicePathAtoZ, serviceType));

        LOG.info(RENDERING_DEVICES_Z_A_MSG);
        sendNotifications(
            ServicePathNotificationTypes.ServiceImplementationRequest,
            otnServicePathZtoA.getServiceName(),
            RpcStatusEx.Pending,
            RENDERING_DEVICES_Z_A_MSG);
        ListenableFuture<OtnDeviceRenderingResult> ztoarenderingFuture =
            this.executor.submit(
                new OtnDeviceRenderingTask(this.otnDeviceRenderer, otnServicePathZtoA, serviceType));

        ListenableFuture<List<OtnDeviceRenderingResult>> renderingCombinedFuture =
            Futures.allAsList(atozrenderingFuture, ztoarenderingFuture);
        List<OtnDeviceRenderingResult> otnRenderingResults = new ArrayList<>(2);
        try {
            LOG.info("Waiting for A-Z and Z-A device renderers ...");
            otnRenderingResults = renderingCombinedFuture.get(Timeouts.RENDERING_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(DEVICE_RENDERING_ROLL_BACK_MSG, e);
            sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                otnServicePathAtoZ.getServiceName(),
                RpcStatusEx.Pending,
                DEVICE_RENDERING_ROLL_BACK_MSG);
            //FIXME we can't do rollback here, because we don't have rendering results.
            return otnRenderingResults;
        }
        for (int i = 0; i < otnRenderingResults.size(); i++) {
            rollbackProcessor.addTask(
                new DeviceRenderingRollbackTask(
                    "DeviceTask n° " + i + 1,
                    ! otnRenderingResults.get(i).isSuccess(),
                    otnRenderingResults.get(i).getRenderedNodeInterfaces(),
                    this.deviceRenderer));
        }
        return otnRenderingResults;
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private List<OLMRenderingResult> olmPowerSetup(
            RollbackProcessor rollbackProcessor,
            ServicePowerSetupInput powerSetupInputAtoZ,
            ServicePowerSetupInput powerSetupInputZtoA, boolean isTempService) {

        //TODO olmPowerSetupFutureAtoZ & olmPowerSetupFutureZtoA & olmFutures used only once
        //     Do notifications & LOG.info deserve this ?
        //TODO use constants for LOG.info & notifications common messages
        // if the service create is a temp-service, OLM will be skipped
        if (isTempService) {
            LOG.info("For temp-service create OLM is not computed and skipped");
            return new ArrayList<>();
        }
        LOG.info("Olm power setup A-Z");
        sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                powerSetupInputAtoZ.getServiceName(),
                RpcStatusEx.Pending,
                "Olm power setup A-Z");
        ListenableFuture<OLMRenderingResult> olmPowerSetupFutureAtoZ =
                this.executor.submit(
                    new OlmPowerSetupTask(rpcService.getRpc(ServicePowerSetup.class), powerSetupInputAtoZ));

        LOG.info("OLM power setup Z-A");
        sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                powerSetupInputAtoZ.getServiceName(),
                RpcStatusEx.Pending,
                "Olm power setup Z-A");
        ListenableFuture<OLMRenderingResult> olmPowerSetupFutureZtoA =
                this.executor.submit(
                    new OlmPowerSetupTask(rpcService.getRpc(ServicePowerSetup.class), powerSetupInputZtoA));
        ListenableFuture<List<OLMRenderingResult>> olmFutures =
                Futures.allAsList(olmPowerSetupFutureAtoZ, olmPowerSetupFutureZtoA);

        List<OLMRenderingResult> olmResults = new ArrayList<>();
        try {
            LOG.info("Waiting for A-Z and Z-A OLM power setup ...");
            olmResults = olmFutures.get(Timeouts.OLM_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(OLM_ROLL_BACK_MSG, e);
            sendNotifications(
                    ServicePathNotificationTypes.ServiceImplementationRequest,
                    powerSetupInputAtoZ.getServiceName(),
                    RpcStatusEx.Pending,
                    olmResultMessage(olmResults));
            rollbackProcessor.addTask(
                    new OlmPowerSetupRollbackTask("AtoZOLMTask", true, rpcService.getRpc(ServicePowerTurndown.class),
                        powerSetupInputAtoZ));
            rollbackProcessor.addTask(
                    new OlmPowerSetupRollbackTask("ZtoAOLMTask", true, rpcService.getRpc(ServicePowerTurndown.class),
                        powerSetupInputZtoA));
            return olmResults;
        }
        rollbackProcessor.addTask(new OlmPowerSetupRollbackTask(
                "AtoZOLMTask", !olmResults.get(0).isSuccess(), rpcService.getRpc(ServicePowerTurndown.class),
                powerSetupInputAtoZ));
        rollbackProcessor.addTask(new OlmPowerSetupRollbackTask(
                "ZtoAOLMTask", !olmResults.get(1).isSuccess(), rpcService.getRpc(ServicePowerTurndown.class),
                powerSetupInputZtoA));

        return olmResults;
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private boolean isServiceActivated(String nodeId, String tpId) {
        LOG.info("Starting service activation test on node {} and tp {}", nodeId, tpId);
        if (!NodeTypes.Xpdr.equals(portMapping.getNode(nodeId).getNodeInfo().getNodeType())) {
            LOG.info("Device {} is not xponder, can't verify PreFEC", nodeId);
            return true;
        }
        for (int i = 0; i < 3; i++) {
            List<Measurements> measurements = getMeasurements(nodeId, tpId);
            if (measurements == null) {
                LOG.warn("Device {} is not reporting PreFEC on TP: {}", nodeId, tpId);
                return true;
            }
            if (verifyPreFecBer(measurements)) {
                return true;
            }
            try {
                Thread.sleep(Timeouts.SERVICE_ACTIVATION_TEST_RETRY_TIME);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.error("Service activation test failed on node {} and termination point {}!", nodeId, tpId);
        return false;
    }

    private List<Measurements> getMeasurements(String nodeId, String tp) {
        try {
            GetPmOutput getPmOutput = rpcService.getRpc(GetPm.class).invoke(
                        new GetPmInputBuilder()
                            .setNodeId(nodeId)
                            .setGranularity(PmGranularity._15min)
                            .setResourceIdentifier(new ResourceIdentifierBuilder().setResourceName(tp + "-OTU").build())
                            .setResourceType(ResourceTypeEnum.Interface)
                            .build())
                    .get()
                    .getResult();
            if ((getPmOutput == null) || (getPmOutput.getNodeId() == null)) {
                LOG.warn("OLM's get PM failed for node {} and tp {}", nodeId, tp);
            } else {
                LOG.info("successfully finished calling OLM's get PM");
                return getPmOutput.getMeasurements();
                // may return null
            }

        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Error occurred while getting PM for node {} and tp {}", nodeId, tp, e);
        }
        return null;
    }

    private boolean verifyPreFecBer(List<Measurements> measurements) {
        double preFecCorrectedErrors = Double.MIN_VALUE;
        double fecUncorrectableBlocks = Double.MIN_VALUE;

        for (Measurements measurement : measurements) {
            switch (measurement.getPmparameterName()) {
                case "preFECCorrectedErrors":
                    preFecCorrectedErrors = Double.parseDouble(measurement.getPmparameterValue());
                    break;
                case "FECUncorrectableBlocks":
                    fecUncorrectableBlocks = Double.parseDouble(measurement.getPmparameterValue());
                    break;
                default:
                    break;
            }
        }

        LOG.info("Measurements: preFECCorrectedErrors = {}; FECUncorrectableBlocks = {}",
                preFecCorrectedErrors, fecUncorrectableBlocks);

        if (fecUncorrectableBlocks > Double.MIN_VALUE) {
            LOG.error("Data has uncorrectable errors, BER test failed");
            return false;
        }

        double numOfBitsPerSecond = 112000000000d;
        double threshold = 0.00002d;
        double result = preFecCorrectedErrors / numOfBitsPerSecond;
        LOG.info("PreFEC value is {}", Double.toString(result));
        return result <= threshold;
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageServicePathCreation(ServiceImplementationRequestInput input, String serviceType,
                                              boolean isTempService) {
        ServicePathInputData servicePathInputDataAtoZ =
            ModelMappingUtils
                .rendererCreateServiceInputAToZ(input.getServiceName(), input.getPathDescription(), Action.Create);
        ServicePathInputData servicePathInputDataZtoA =
            ModelMappingUtils
                .rendererCreateServiceInputZToA(input.getServiceName(), input.getPathDescription(), Action.Create);
        // Rollback should be same for all conditions, so creating a new one
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        List<DeviceRenderingResult> renderingResults =
            deviceRendering(rollbackProcessor, servicePathInputDataAtoZ, servicePathInputDataZtoA);
        if (rollbackProcessor.rollbackAllIfNecessary() > 0 || renderingResults.isEmpty()) {
            sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(),
                RpcStatusEx.Failed,
                resultMessage(renderingResults));
            return false;
        }
        olmPowerSetup(
            rollbackProcessor,
            //olmPowerSetupInputAtoZ,
            ModelMappingUtils.createServicePowerSetupInput(renderingResults.get(0).getOlmList(), input),
            //olmPowerSetupInputZtoA
            ModelMappingUtils.createServicePowerSetupInput(renderingResults.get(1).getOlmList(), input), isTempService);
        if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
            sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(),
                RpcStatusEx.Failed,
                resultMessage(renderingResults));
            return false;
        }
        // run service activation test twice - once on source node and once on
        // destination node
        List<Nodes> nodes = servicePathInputDataAtoZ.getServicePathInput().getNodes();
        if ((nodes == null) || (nodes.isEmpty())) {
            return false;
        }

        Nodes sourceNode = nodes.get(0);
        Nodes destNode = nodes.get(nodes.size() - 1);
        String srcNetworkTp =
            sourceNode.getDestTp().contains(StringConstants.NETWORK_TOKEN)
                ? sourceNode.getDestTp()
                : sourceNode.getSrcTp();
        String dstNetowrkTp =
            destNode.getDestTp().contains(StringConstants.NETWORK_TOKEN)
                ? destNode.getDestTp()
                : destNode.getSrcTp();

        if (!isServiceActivated(sourceNode.getNodeId(), srcNetworkTp)
                || !isServiceActivated(destNode.getNodeId(), dstNetowrkTp)) {
            rollbackProcessor.rollbackAll();
            sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(),
                RpcStatusEx.Failed,
                "Service activation test failed.");
            return false;
        }
        sendNotificationsWithPathDescription(
            ServicePathNotificationTypes.ServiceImplementationRequest,
            input.getServiceName(),
            RpcStatusEx.Successful,
            OPERATION_SUCCESSFUL,
            input.getPathDescription(),
            createLinkForNotif(
                renderingResults.stream()
                    .flatMap(rr -> rr.getOtnLinkTps().stream())
                    .collect(Collectors.toList())),
            null,
            serviceType);
        return true;
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageServicePathDeletion(String serviceName, PathDescription pathDescription, String serviceType)
            throws InterruptedException {
        ServicePathInputData servicePathInputDataAtoZ =
            ModelMappingUtils.rendererCreateServiceInputAToZ(serviceName, pathDescription, Action.Delete);
        ServicePathInputData servicePathInputDataZtoA =
            ModelMappingUtils.rendererCreateServiceInputZToA(serviceName, pathDescription, Action.Delete);

        ListenableFuture<OLMRenderingResult> olmPowerTurnDownFutureAtoZ = this.executor.submit(
                new OlmPowerTurnDownTask(serviceName, ATOZPATH, servicePathInputDataAtoZ, notification, rpcService));

        ListenableFuture<OLMRenderingResult> olmPowerTurnDownFutureZtoA = this.executor.submit(
                new OlmPowerTurnDownTask(serviceName, ZTOAPATH, servicePathInputDataZtoA, notification, rpcService));

        ListenableFuture<List<OLMRenderingResult>> olmPowerTurnDownFutures =
            Futures.allAsList(olmPowerTurnDownFutureAtoZ, olmPowerTurnDownFutureZtoA);

        List<OLMRenderingResult> olmRenderingResults;
        // OLM turn down power
        try {
            LOG.info("Waiting for A-Z and Z-A OLM power turn down ...");
            olmRenderingResults = olmPowerTurnDownFutures.get(
                Timeouts.OLM_TIMEOUT, TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error while turning down power!", e);
            return false;
        }
        if (!olmRenderingResults.get(0).isSuccess() || !olmRenderingResults.get(1).isSuccess()) {
            LOG.error("Error while turning down power!");
            return false;
        }
        LOG.info("OLM power successfully turned down!");
        // delete service path with renderer
        LOG.info("Deleting service path via renderer");
        sendNotifications(
            ServicePathNotificationTypes.ServiceDelete,
            serviceName,
            RpcStatusEx.Pending,
            "Deleting service path via renderer");
        sendNotificationsWithPathDescription(
            ServicePathNotificationTypes.ServiceDelete,
            serviceName,
            RpcStatusEx.Successful,
            OPERATION_SUCCESSFUL,
            pathDescription,
            createLinkForNotif(
                deviceRendering(
                        new RollbackProcessor(),
                        servicePathInputDataAtoZ,
                        servicePathInputDataZtoA)
                    .stream()
                    .flatMap(rr -> rr.getOtnLinkTps().stream())
                    .collect(Collectors.toList())),
            null,
            serviceType);
        return true;
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageOtnServicePathCreation(
            ServiceImplementationRequestInput input,
            String serviceType,
            Uint32 serviceRate) {
        // Rollback should be same for all conditions, so creating a new one
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        List<OtnDeviceRenderingResult> renderingResults =
            otnDeviceRendering(
                rollbackProcessor,
                // This is A-Z side
                ModelMappingUtils
                    .rendererCreateOtnServiceInput(
                        input.getServiceName(),
                        Action.Create,
                        input.getServiceAEnd().getServiceFormat().getName(),
                        serviceRate,
                        input.getPathDescription(),
                        true),
                // This is Z-A side
                ModelMappingUtils
                    .rendererCreateOtnServiceInput(
                        input.getServiceName(),
                        Action.Create,
                        input.getServiceZEnd().getServiceFormat().getName(),
                        serviceRate,
                        input.getPathDescription(),
                        false),
                serviceType);
        if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
            rollbackProcessor.rollbackAll();
            sendNotifications(
                ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(),
                RpcStatusEx.Failed,
                DEVICE_RENDERING_ROLL_BACK_MSG);
            return false;
        }
        sendNotificationsWithPathDescription(
            ServicePathNotificationTypes.ServiceImplementationRequest,
            input.getServiceName(),
            RpcStatusEx.Successful, OPERATION_SUCCESSFUL,
            input.getPathDescription(),
            createLinkForNotif(
                renderingResults.stream()
                    .flatMap(rr -> rr.getOtnLinkTps().stream())
                    .collect(Collectors.toList())),
            getSupportedLinks(
                ModelMappingUtils.getLinksFromServicePathDescription(input.getPathDescription()),
                serviceType),
            serviceType);
        return true;
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageOtnServicePathDeletion(
            String serviceName,
            PathDescription pathDescription,
            Services service,
            String serviceType) {
        LOG.info("Deleting otn-service path {} via renderer", serviceName);
        sendNotifications(
                ServicePathNotificationTypes.ServiceDelete,
                serviceName,
                RpcStatusEx.Pending,
                "Deleting otn-service path via renderer");
        List<OtnDeviceRenderingResult> renderingResults =
            otnDeviceRendering(
                new RollbackProcessor(),
                // This is A-Z side
                ModelMappingUtils
                    .rendererCreateOtnServiceInput(
                        serviceName,
                        Action.Delete,
                        service.getServiceAEnd().getServiceFormat().getName(),
                        service.getServiceAEnd().getServiceRate(),
                        pathDescription,
                        true),
                // This is Z-A side
                ModelMappingUtils
                    .rendererCreateOtnServiceInput(
                        serviceName,
                        Action.Delete,
                        service.getServiceZEnd().getServiceFormat().getName(),
                        service.getServiceAEnd().getServiceRate(),
                        pathDescription,
                        false),
                serviceType);
        sendNotificationsWithPathDescription(
            ServicePathNotificationTypes.ServiceDelete,
            serviceName,
            RpcStatusEx.Successful,
            OPERATION_SUCCESSFUL,
            pathDescription,
            createLinkForNotif(
                renderingResults.stream()
                    .flatMap(rr -> rr.getOtnLinkTps().stream())
                    .collect(Collectors.toList())),
            getSupportedLinks(
                ModelMappingUtils.getLinksFromServicePathDescription(pathDescription),
                serviceType),
            serviceType);
        return true;
    }

    /**
     * Send renderer notification.
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     */
    private void sendNotifications(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName,
            RpcStatusEx rpcStatusEx,
            String message) {

        notification.send(
            servicePathNotificationTypes,
            serviceName,
            rpcStatusEx,
            message
        );
    }

    /**
     * Send renderer notification with path description information.
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     * @param pathDescription PathDescription
     */
    private void sendNotificationsWithPathDescription(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName,
            RpcStatusEx rpcStatusEx,
            String message,
            PathDescription pathDescription,
            Link notifLink,
            Set<String> supportedLinks,
            String serviceType) {

        notification.send(
            notification.buildNotification(
                servicePathNotificationTypes,
                serviceName,
                rpcStatusEx,
                message,
                pathDescription,
                notifLink,
                supportedLinks,
                serviceType
            )
        );
    }

    private Link createLinkForNotif(List<LinkTp> otnLinkTerminationPoints) {
        return
            otnLinkTerminationPoints == null || otnLinkTerminationPoints.size() != 2
                ? null
                : new LinkBuilder()
                    .setATermination(
                        new ATerminationBuilder()
                            .setNodeId(otnLinkTerminationPoints.get(0).getNodeId())
                            .setTpId(otnLinkTerminationPoints.get(0).getTpId())
                            .build())
                    .setZTermination(
                        new ZTerminationBuilder()
                            .setNodeId(otnLinkTerminationPoints.get(1).getNodeId())
                            .setTpId(otnLinkTerminationPoints.get(1).getTpId())
                            .build())
                    .build();
    }

    private Set<String> getSupportedLinks(Set<String> allSupportLinks, String serviceType) {
        //TODO a Map might be more indicated here
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                return allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.ODTU4.getName())).collect(Collectors.toSet());
            case StringConstants.SERVICE_TYPE_100GE_M:
                return allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.ODUC4.getName())).collect(Collectors.toSet());
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_100GE_S:
                return allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.OTU4.getName())).collect(Collectors.toSet());
            case StringConstants.SERVICE_TYPE_ODUC4:
                return allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.OTUC4.getName())).collect(Collectors.toSet());
            default:
                return null;
        }
    }

    private String olmResultMessage(List<OLMRenderingResult> renderingResults) {
        Message weightedResultMessage = new WeightedResultMessage();

        return weightedResultMessage.olmRenderingResultMessage(
            renderingResults,
            "OLM power setup failed",
            ""
        );
    }

    private String resultMessage(List<DeviceRenderingResult> renderingResults) {
        Message weightedResultMessage = new WeightedResultMessage();

        return weightedResultMessage.deviceRenderingResultMessage(
            renderingResults,
            "Setup service path failed due to an unknown error",
            ""
        );
    }
}
