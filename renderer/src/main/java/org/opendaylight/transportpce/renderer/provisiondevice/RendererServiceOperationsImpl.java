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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.ServicePathInputData;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.DeviceRenderingRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.DeviceRenderingTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerSetupRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerSetupTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OtnDeviceRenderingTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.RollbackProcessor;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.link._for.notif.ATerminationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.link._for.notif.ZTerminationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.optical.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RendererServiceOperationsImpl implements RendererServiceOperations {

    private static final String DEVICE_RENDERING_ROLL_BACK_MSG =
            "Device rendering was not successful! Rendering will be rolled back.";
    private static final String OLM_ROLL_BACK_MSG =
            "OLM power setup was not successful! Rendering and OLM will be rolled back.";
    private static final String RENDERING_DEVICES_A_Z_MSG = "Rendering devices A-Z";
    private static final String RENDERING_DEVICES_Z_A_MSG = "Rendering device Z-A";
    private static final String TURNING_DOWN_POWER_ON_A_TO_Z_PATH_MSG = "Turning down power on A-to-Z path";
    private static final Logger LOG = LoggerFactory.getLogger(RendererServiceOperationsImpl.class);
    private static final String FAILED = "Failed";
    private static final String OPERATION_FAILED = "Operation Failed";
    private static final String OPERATION_SUCCESSFUL = "Operation Successful";
    private static final int NUMBER_OF_THREADS = 4;

    private final DeviceRendererService deviceRenderer;
    private final OtnDeviceRendererService otnDeviceRenderer;
    private final TransportpceOlmService olmService;
    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private final PortMapping portMapping;
    private ListeningExecutorService executor;

    public RendererServiceOperationsImpl(DeviceRendererService deviceRenderer,
            OtnDeviceRendererService otnDeviceRenderer, TransportpceOlmService olmService,
            DataBroker dataBroker, NotificationPublishService notificationPublishService, PortMapping portMapping) {
        this.deviceRenderer = deviceRenderer;
        this.otnDeviceRenderer = otnDeviceRenderer;
        this.olmService = olmService;
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
        this.portMapping = portMapping;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
    }

    @Override
    public ListenableFuture<ServiceImplementationRequestOutput>
            serviceImplementation(ServiceImplementationRequestInput input) {
        LOG.info("Calling service impl request {}", input.getServiceName());
        return executor.submit(new Callable<ServiceImplementationRequestOutput>() {

            @Override
            public ServiceImplementationRequestOutput call() throws Exception {
                sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest, input.getServiceName(),
                        RpcStatusEx.Pending, "Service compliant, submitting service implementation Request ...");
                Uint32 serviceRate = getServiceRate(input);
                String serviceType = ServiceTypes.getServiceType(
                    input.getServiceAEnd().getServiceFormat().getName(),
                    serviceRate,
                    (NodeTypes.Xpdr.equals(portMapping.getNode(input.getServiceAEnd().getNodeId())
                        .getNodeInfo().getNodeType())
                            && input.getServiceAEnd().getTxDirection() != null
                            && input.getServiceAEnd().getTxDirection().getPort() != null
                            && input.getServiceAEnd().getTxDirection().getPort().getPortName() != null)
                        ? portMapping.getMapping(input.getServiceAEnd().getNodeId(),
                                input.getServiceAEnd().getTxDirection().getPort().getPortName())
                        : null);

                switch (serviceType) {
                    case StringConstants.SERVICE_TYPE_100GE_T:
                    case StringConstants.SERVICE_TYPE_400GE:
                    case StringConstants.SERVICE_TYPE_OTU4:
                    case StringConstants.SERVICE_TYPE_OTUC4:
                        if (!manageServicePathCreation(input, serviceType)) {
                            return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                                OPERATION_FAILED);
                        }
                        break;
                    case StringConstants.SERVICE_TYPE_1GE:
                    case StringConstants.SERVICE_TYPE_10GE:
                    case StringConstants.SERVICE_TYPE_100GE_M:
                    case StringConstants.SERVICE_TYPE_100GE_S:
                    case StringConstants.SERVICE_TYPE_ODU4:
                    case StringConstants.SERVICE_TYPE_ODUC4:
                        if (!manageOtnServicePathCreation(input, serviceType, serviceRate)) {
                            return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                                OPERATION_FAILED);
                        }
                        break;
                    default:
                        LOG.error("unsupported service-type");
                        return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                            OPERATION_FAILED);
                }
                return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_OK,
                    OPERATION_SUCCESSFUL);
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
                sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName,
                        RpcStatusEx.Pending, "Service compliant, submitting service delete Request ...");
                // Obtain path description
                Optional<
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service
                    .path.PathDescription> pathDescriptionOpt = getPathDescriptionFromDatastore(serviceName);
                if (pathDescriptionOpt.isEmpty()) {
                    LOG.error("Unable to get path description for service {}!", serviceName);
                    sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName,
                            RpcStatusEx.Failed, "Unable to get path description for service");
                    return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                            OPERATION_FAILED);
                }
                PathDescription pathDescription = pathDescriptionOpt.get();
                Mapping mapping = portMapping.getMapping(service.getServiceAEnd().getNodeId().getValue(),
                    service.getServiceAEnd().getTxDirection().getPort().getPortName());
                String serviceType = ServiceTypes.getServiceType(service.getServiceAEnd().getServiceFormat().getName(),
                    service.getServiceAEnd().getServiceRate(), mapping);
                switch (serviceType) {
                    case StringConstants.SERVICE_TYPE_100GE_T:
                    case StringConstants.SERVICE_TYPE_400GE:
                    case StringConstants.SERVICE_TYPE_OTU4:
                    case StringConstants.SERVICE_TYPE_OTUC4:
                        if (!manageServicePathDeletion(serviceName, pathDescription, serviceType)) {
                            return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                                OPERATION_FAILED);
                        }
                        break;
                    case StringConstants.SERVICE_TYPE_1GE:
                    case StringConstants.SERVICE_TYPE_10GE:
                    case StringConstants.SERVICE_TYPE_100GE_M:
                    case StringConstants.SERVICE_TYPE_100GE_S:
                    case StringConstants.SERVICE_TYPE_ODU4:
                    case StringConstants.SERVICE_TYPE_ODUC4:
                        if (!manageOtnServicePathDeletion(serviceName, pathDescription, service, serviceType)) {
                            return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                                OPERATION_FAILED);
                        }
                        break;
                    default:
                        LOG.error("unsupported service-type");
                        return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                            OPERATION_FAILED);
                }
                return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_OK, OPERATION_SUCCESSFUL);
            }
        });
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
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
            ServiceFormat.OTU.equals(input.getServiceAEnd().getServiceFormat())
                ? input.getServiceAEnd().getOtuServiceRate().getSimpleName()
                : input.getServiceAEnd().getOduServiceRate().getSimpleName();
        if (!formatRateMap.get(input.getServiceAEnd().getServiceFormat()).containsKey(serviceName)) {
            LOG.warn("Unable to get service-rate for service {} - unsupported service name {}",
                input.getServiceName(), serviceName);
            return Uint32.ZERO;
        }
        return formatRateMap.get(input.getServiceAEnd().getServiceFormat()).get(serviceName);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private ServicePowerTurndownOutput olmPowerTurndown(ServicePathInputData servicePathInputData)
            throws InterruptedException, ExecutionException, TimeoutException {
        LOG.debug(TURNING_DOWN_POWER_ON_A_TO_Z_PATH_MSG);
        Future<RpcResult<ServicePowerTurndownOutput>> powerTurndownFuture = this.olmService.servicePowerTurndown(
                new ServicePowerTurndownInputBuilder(servicePathInputData.getServicePathInput()).build());
        return powerTurndownFuture.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS).getResult();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private Optional<org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
        .service.path.PathDescription> getPathDescriptionFromDatastore(String serviceName) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
            .service.path.PathDescription> pathDescriptionIID = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName))
                .child(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
                    .service.path.PathDescription.class);
        ReadTransaction pathDescReadTx = this.dataBroker.newReadOnlyTransaction();
        try {
            LOG.debug("Getting path description for service {}", serviceName);
            return pathDescReadTx.read(LogicalDatastoreType.OPERATIONAL, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting path description from datastore {} for service {}!", pathDescriptionIID,
                    serviceName, e);
            return Optional.empty();
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private List<DeviceRenderingResult> deviceRendering(RollbackProcessor rollbackProcessor,
            ServicePathInputData servicePathDataAtoZ, ServicePathInputData servicePathDataZtoA) {
        LOG.info(RENDERING_DEVICES_A_Z_MSG);
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                servicePathDataAtoZ.getServicePathInput().getServiceName(), RpcStatusEx.Pending,
                RENDERING_DEVICES_A_Z_MSG);
        ListenableFuture<DeviceRenderingResult> atozrenderingFuture =
                this.executor.submit(new DeviceRenderingTask(this.deviceRenderer, servicePathDataAtoZ,
                        ServicePathDirection.A_TO_Z));

        LOG.info("Rendering devices Z-A");
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                servicePathDataZtoA.getServicePathInput().getServiceName(), RpcStatusEx.Pending,
                RENDERING_DEVICES_Z_A_MSG);
        ListenableFuture<DeviceRenderingResult> ztoarenderingFuture =
                this.executor.submit(new DeviceRenderingTask(this.deviceRenderer, servicePathDataZtoA,
                        ServicePathDirection.Z_TO_A));
        ListenableFuture<List<DeviceRenderingResult>> renderingCombinedFuture =
                Futures.allAsList(atozrenderingFuture, ztoarenderingFuture);

        List<DeviceRenderingResult> renderingResults = new ArrayList<>(2);
        try {
            LOG.info("Waiting for A-Z and Z-A device renderers ...");
            renderingResults = renderingCombinedFuture.get(Timeouts.RENDERING_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(DEVICE_RENDERING_ROLL_BACK_MSG, e);
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                    servicePathDataAtoZ.getServicePathInput().getServiceName(), RpcStatusEx.Pending,
                    DEVICE_RENDERING_ROLL_BACK_MSG);
            //FIXME we can't do rollback here, because we don't have rendering results.
            return renderingResults;
        }

        rollbackProcessor.addTask(new DeviceRenderingRollbackTask("AtoZDeviceTask",
                ! renderingResults.get(0).isSuccess(), renderingResults.get(0).getRenderedNodeInterfaces(),
                this.deviceRenderer));
        rollbackProcessor.addTask(new DeviceRenderingRollbackTask("ZtoADeviceTask",
                ! renderingResults.get(1).isSuccess(), renderingResults.get(1).getRenderedNodeInterfaces(),
                this.deviceRenderer));
        return renderingResults;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private List<OtnDeviceRenderingResult> otnDeviceRendering(RollbackProcessor rollbackProcessor,
        OtnServicePathInput otnServicePathAtoZ, OtnServicePathInput otnServicePathZtoA, String serviceType) {
        LOG.info(RENDERING_DEVICES_A_Z_MSG);
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
            otnServicePathAtoZ.getServiceName(), RpcStatusEx.Pending,
            RENDERING_DEVICES_A_Z_MSG);
        ListenableFuture<OtnDeviceRenderingResult> atozrenderingFuture =
            this.executor.submit(new OtnDeviceRenderingTask(this.otnDeviceRenderer, otnServicePathAtoZ, serviceType));
        LOG.info(RENDERING_DEVICES_Z_A_MSG);
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
            otnServicePathZtoA.getServiceName(), RpcStatusEx.Pending,
            RENDERING_DEVICES_Z_A_MSG);
        ListenableFuture<OtnDeviceRenderingResult> ztoarenderingFuture =
            this.executor.submit(new OtnDeviceRenderingTask(this.otnDeviceRenderer, otnServicePathZtoA, serviceType));
        ListenableFuture<List<OtnDeviceRenderingResult>> renderingCombinedFuture =
            Futures.allAsList(atozrenderingFuture, ztoarenderingFuture);
        List<OtnDeviceRenderingResult> otnRenderingResults = new ArrayList<>(2);
        try {
            LOG.info("Waiting for A-Z and Z-A device renderers ...");
            otnRenderingResults = renderingCombinedFuture.get(Timeouts.RENDERING_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(DEVICE_RENDERING_ROLL_BACK_MSG, e);
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                otnServicePathAtoZ.getServiceName(), RpcStatusEx.Pending,
                DEVICE_RENDERING_ROLL_BACK_MSG);
            //FIXME we can't do rollback here, because we don't have rendering results.
            return otnRenderingResults;
        }
        for (int i = 0; i < otnRenderingResults.size(); i++) {
            rollbackProcessor.addTask(new DeviceRenderingRollbackTask("DeviceTask n° " + i + 1,
                ! otnRenderingResults.get(i).isSuccess(), otnRenderingResults.get(i).getRenderedNodeInterfaces(),
                this.deviceRenderer));
        }
        return otnRenderingResults;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private void olmPowerSetup(RollbackProcessor rollbackProcessor, ServicePowerSetupInput powerSetupInputAtoZ,
            ServicePowerSetupInput powerSetupInputZtoA) {
        LOG.info("Olm power setup A-Z");
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                powerSetupInputAtoZ.getServiceName(), RpcStatusEx.Pending, "Olm power setup A-Z");
        ListenableFuture<OLMRenderingResult> olmPowerSetupFutureAtoZ
                = this.executor.submit(new OlmPowerSetupTask(this.olmService, powerSetupInputAtoZ));

        LOG.info("OLM power setup Z-A");
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                powerSetupInputAtoZ.getServiceName(), RpcStatusEx.Pending, "Olm power setup Z-A");
        ListenableFuture<OLMRenderingResult> olmPowerSetupFutureZtoA
                = this.executor.submit(new OlmPowerSetupTask(this.olmService, powerSetupInputZtoA));
        ListenableFuture<List<OLMRenderingResult>> olmFutures =
                Futures.allAsList(olmPowerSetupFutureAtoZ, olmPowerSetupFutureZtoA);

        List<OLMRenderingResult> olmResults;
        try {
            LOG.info("Waiting for A-Z and Z-A OLM power setup ...");
            olmResults = olmFutures.get(Timeouts.OLM_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(OLM_ROLL_BACK_MSG, e);
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                    powerSetupInputAtoZ.getServiceName(), RpcStatusEx.Pending,
                    OLM_ROLL_BACK_MSG);
            rollbackProcessor.addTask(new OlmPowerSetupRollbackTask("AtoZOLMTask", true,
                    this.olmService, powerSetupInputAtoZ));
            rollbackProcessor.addTask(new OlmPowerSetupRollbackTask("ZtoAOLMTask", true,
                    this.olmService, powerSetupInputZtoA));
            return;
        }

        rollbackProcessor.addTask(new OlmPowerSetupRollbackTask("AtoZOLMTask", ! olmResults.get(0).isSuccess(),
                this.olmService, powerSetupInputAtoZ));
        rollbackProcessor.addTask(new OlmPowerSetupRollbackTask("ZtoAOLMTask", ! olmResults.get(1).isSuccess(),
                this.olmService, powerSetupInputZtoA));
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "call in call() method")
    private boolean isServiceActivated(String nodeId, String tpId) {
        LOG.info("Starting service activation test on node {} and tp {}", nodeId, tpId);
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
        GetPmInputBuilder getPmIpBldr = new GetPmInputBuilder()
            .setNodeId(nodeId)
            .setGranularity(PmGranularity._15min)
            .setResourceIdentifier(new ResourceIdentifierBuilder().setResourceName(tp + "-OTU").build())
            .setResourceType(ResourceTypeEnum.Interface);

        try {
            Future<RpcResult<GetPmOutput>> getPmFuture = this.olmService.getPm(getPmIpBldr.build());
            RpcResult<GetPmOutput> getPmRpcResult = getPmFuture.get();
            GetPmOutput getPmOutput = getPmRpcResult.getResult();
            if ((getPmOutput != null) && (getPmOutput.getNodeId() != null)) {
                LOG.info("successfully finished calling OLM's get PM");
                return getPmOutput.getMeasurements();
                // may return null
            } else {
                LOG.warn("OLM's get PM failed for node {} and tp {}", nodeId, tp);
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

        LOG.info("Measurements: preFECCorrectedErrors = {}; FECUncorrectableBlocks = {}", preFecCorrectedErrors,
                fecUncorrectableBlocks);

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

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageServicePathCreation(ServiceImplementationRequestInput input, String serviceType) {
        ServicePathInputData servicePathInputDataAtoZ = ModelMappingUtils
            .rendererCreateServiceInputAToZ(input.getServiceName(), input.getPathDescription(), Action.Create);
        ServicePathInputData servicePathInputDataZtoA = ModelMappingUtils
            .rendererCreateServiceInputZToA(input.getServiceName(), input.getPathDescription(), Action.Create);
        // Rollback should be same for all conditions, so creating a new one
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        List<DeviceRenderingResult> renderingResults =
            deviceRendering(rollbackProcessor, servicePathInputDataAtoZ, servicePathInputDataZtoA);
        if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(), RpcStatusEx.Failed, DEVICE_RENDERING_ROLL_BACK_MSG);
            return false;
        }
        ServicePowerSetupInput olmPowerSetupInputAtoZ =
            ModelMappingUtils.createServicePowerSetupInput(renderingResults.get(0).getOlmList(), input);
        ServicePowerSetupInput olmPowerSetupInputZtoA =
            ModelMappingUtils.createServicePowerSetupInput(renderingResults.get(1).getOlmList(), input);
        olmPowerSetup(rollbackProcessor, olmPowerSetupInputAtoZ, olmPowerSetupInputZtoA);
        if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(), RpcStatusEx.Failed, OLM_ROLL_BACK_MSG);
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
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(), RpcStatusEx.Failed,
                "Service activation test failed.");
            return false;
        }
        List<LinkTp> otnLinkTerminationPoints = new ArrayList<>();
        renderingResults.forEach(rr -> otnLinkTerminationPoints.addAll(rr.getOtnLinkTps()));
        Link notifLink = createLinkForNotif(otnLinkTerminationPoints);

        sendNotificationsWithPathDescription(ServicePathNotificationTypes.ServiceImplementationRequest,
            input.getServiceName(), RpcStatusEx.Successful, OPERATION_SUCCESSFUL, input.getPathDescription(),
            notifLink, null, serviceType);
        return true;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageServicePathDeletion(String serviceName, PathDescription pathDescription, String serviceType)
            throws InterruptedException {
        ServicePathInputData servicePathInputDataAtoZ =
            ModelMappingUtils.rendererCreateServiceInputAToZ(serviceName, pathDescription, Action.Delete);
        ServicePathInputData servicePathInputDataZtoA =
            ModelMappingUtils.rendererCreateServiceInputZToA(serviceName, pathDescription, Action.Delete);
        // OLM turn down power
        try {
            LOG.debug(TURNING_DOWN_POWER_ON_A_TO_Z_PATH_MSG);
            sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName,
                RpcStatusEx.Pending, TURNING_DOWN_POWER_ON_A_TO_Z_PATH_MSG);
            ServicePowerTurndownOutput atozPowerTurndownOutput = olmPowerTurndown(servicePathInputDataAtoZ);
            // TODO add some flag rather than string
            if (FAILED.equals(atozPowerTurndownOutput.getResult())) {
                LOG.error("Service power turndown failed on A-to-Z path for service {}!", serviceName);
                sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Failed,
                        "Service power turndown failed on A-to-Z path for service");
                return false;
            }
            LOG.debug("Turning down power on Z-to-A path");
            sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Pending,
                    "Turning down power on Z-to-A path");
            ServicePowerTurndownOutput ztoaPowerTurndownOutput = olmPowerTurndown(servicePathInputDataZtoA);
            // TODO add some flag rather than string
            if (FAILED.equals(ztoaPowerTurndownOutput.getResult())) {
                LOG.error("Service power turndown failed on Z-to-A path for service {}!", serviceName);
                sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Failed,
                        "Service power turndown failed on Z-to-A path for service");
                return false;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error while turning down power!", e);
            return false;
        }
        // delete service path with renderer
        LOG.info("Deleting service path via renderer");
        sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Pending,
                "Deleting service path via renderer");
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        List<DeviceRenderingResult> renderingResults =
            deviceRendering(rollbackProcessor, servicePathInputDataAtoZ, servicePathInputDataZtoA);
        List<LinkTp> otnLinkTerminationPoints = new ArrayList<>();
        renderingResults.forEach(rr -> otnLinkTerminationPoints.addAll(rr.getOtnLinkTps()));
        Link notifLink = createLinkForNotif(otnLinkTerminationPoints);

        sendNotificationsWithPathDescription(ServicePathNotificationTypes.ServiceDelete,
            serviceName, RpcStatusEx.Successful, OPERATION_SUCCESSFUL, pathDescription, notifLink, null, serviceType);
        return true;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageOtnServicePathCreation(ServiceImplementationRequestInput input, String serviceType,
            Uint32 serviceRate) {
        // This is A-Z side
        OtnServicePathInput otnServicePathInputAtoZ = ModelMappingUtils
            .rendererCreateOtnServiceInput(input.getServiceName(), Action.Create,
                input.getServiceAEnd().getServiceFormat().getName(),
                serviceRate,
                input.getPathDescription(), true);
        // This is Z-A side
        OtnServicePathInput otnServicePathInputZtoA = ModelMappingUtils
            .rendererCreateOtnServiceInput(input.getServiceName(), Action.Create,
                input.getServiceZEnd().getServiceFormat().getName(),
                serviceRate,
                input.getPathDescription(), false);
        // Rollback should be same for all conditions, so creating a new one
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        List<OtnDeviceRenderingResult> renderingResults =
            otnDeviceRendering(rollbackProcessor, otnServicePathInputAtoZ, otnServicePathInputZtoA, serviceType);
        if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
            rollbackProcessor.rollbackAll();
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(), RpcStatusEx.Failed, DEVICE_RENDERING_ROLL_BACK_MSG);
            return false;
        }
        List<LinkTp> otnLinkTerminationPoints = new ArrayList<>();
        renderingResults.forEach(rr -> otnLinkTerminationPoints.addAll(rr.getOtnLinkTps()));
        Link notifLink = createLinkForNotif(otnLinkTerminationPoints);
        List<String> allSupportLinks = ModelMappingUtils.getLinksFromServicePathDescription(input.getPathDescription());
        List<String> supportedLinks = null;
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                supportedLinks = allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.ODTU4.getName())).collect(Collectors.toList());
                break;
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_100GE_S:
                supportedLinks = allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.OTU4.getName())).collect(Collectors.toList());
                break;
            case StringConstants.SERVICE_TYPE_ODUC4:
                supportedLinks = allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.OTUC4.getName())).collect(Collectors.toList());
                break;
            default:
                break;
        }

        sendNotificationsWithPathDescription(ServicePathNotificationTypes.ServiceImplementationRequest,
            input.getServiceName(), RpcStatusEx.Successful, OPERATION_SUCCESSFUL, input.getPathDescription(),
            notifLink, supportedLinks, serviceType);
        return true;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageOtnServicePathDeletion(String serviceName, PathDescription pathDescription,
            Services service, String serviceType) {
        // This is A-Z side
        OtnServicePathInput otnServicePathInputAtoZ = ModelMappingUtils
            .rendererCreateOtnServiceInput(serviceName, Action.Delete,
                service.getServiceAEnd().getServiceFormat().getName(),
                service.getServiceAEnd().getServiceRate(),
                pathDescription, true);
        // This is Z-A side
        OtnServicePathInput otnServicePathInputZtoA = ModelMappingUtils
            .rendererCreateOtnServiceInput(serviceName, Action.Delete,
                service.getServiceZEnd().getServiceFormat().getName(),
                service.getServiceAEnd().getServiceRate(),
                pathDescription, false);
        LOG.info("Deleting otn-service path {} via renderer", serviceName);
        sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Pending,
                "Deleting otn-service path via renderer");

        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        List<OtnDeviceRenderingResult> renderingResults =
            otnDeviceRendering(rollbackProcessor, otnServicePathInputAtoZ, otnServicePathInputZtoA, serviceType);

        List<LinkTp> otnLinkTerminationPoints = new ArrayList<>();
        renderingResults.forEach(rr -> otnLinkTerminationPoints.addAll(rr.getOtnLinkTps()));
        Link notifLink = createLinkForNotif(otnLinkTerminationPoints);
        List<String> allSupportLinks = ModelMappingUtils.getLinksFromServicePathDescription(pathDescription);
        List<String> supportedLinks = null;
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                supportedLinks = allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.ODTU4.getName())).collect(Collectors.toList());
                break;
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_100GE_S:
                supportedLinks = allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.OTU4.getName())).collect(Collectors.toList());
                break;
            case StringConstants.SERVICE_TYPE_ODUC4:
                supportedLinks = allSupportLinks.stream()
                    .filter(lk -> lk.startsWith(OtnLinkType.OTUC4.getName())).collect(Collectors.toList());
                break;
            default:
                break;
        }

        sendNotificationsWithPathDescription(ServicePathNotificationTypes.ServiceDelete,
                serviceName, RpcStatusEx.Successful, OPERATION_SUCCESSFUL, pathDescription, notifLink, supportedLinks,
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
    private void sendNotifications(ServicePathNotificationTypes servicePathNotificationTypes, String serviceName,
            RpcStatusEx rpcStatusEx, String message) {
        Notification notification = buildNotification(servicePathNotificationTypes, serviceName, rpcStatusEx, message,
                null, null, null, null);
        send(notification);
    }

    /**
     * Send renderer notification with path description information.
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     * @param pathDescription PathDescription
     */
    private void sendNotificationsWithPathDescription(ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName, RpcStatusEx rpcStatusEx, String message, PathDescription pathDescription,
            Link notifLink, List<String> supportedLinks, String serviceType) {
        Notification notification = buildNotification(servicePathNotificationTypes, serviceName, rpcStatusEx, message,
                pathDescription, notifLink, supportedLinks, serviceType);
        send(notification);
    }

    /**
     * Build notification containing path description information.
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     * @param pathDescription PathDescription
     * @return notification with RendererRpcResultSp type.
     */
    private RendererRpcResultSp buildNotification(ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName, RpcStatusEx rpcStatusEx, String message, PathDescription pathDescription,
            Link notifLink, List<String> supportedLinks, String serviceType) {
        RendererRpcResultSpBuilder builder = new RendererRpcResultSpBuilder()
                .setNotificationType(servicePathNotificationTypes).setServiceName(serviceName).setStatus(rpcStatusEx)
                .setStatusMessage(message)
                .setServiceType(serviceType);
        if (pathDescription != null) {
            builder.setAToZDirection(pathDescription.getAToZDirection())
                .setZToADirection(pathDescription.getZToADirection());
        }
        if (notifLink != null) {
            builder.setLink(notifLink);
        }
        if (supportedLinks != null) {
            builder.setLinkId(supportedLinks);
        }
        return builder.build();
    }

    /**
     * Send renderer notification.
     * @param notification Notification
     */
    private void send(Notification notification) {
        try {
            LOG.info("Sending notification {}", notification);
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected: ", e);
            Thread.currentThread().interrupt();
        }
    }

    private Link createLinkForNotif(List<LinkTp> otnLinkTerminationPoints) {
        if (otnLinkTerminationPoints == null || otnLinkTerminationPoints.size() != 2) {
            return null;
        }
        return new LinkBuilder()
                .setATermination(new ATerminationBuilder()
                    .setNodeId(otnLinkTerminationPoints.get(0).getNodeId())
                    .setTpId(otnLinkTerminationPoints.get(0).getTpId())
                    .build())
                .setZTermination(new ZTerminationBuilder()
                    .setNodeId(otnLinkTerminationPoints.get(1).getNodeId())
                    .setTpId(otnLinkTerminationPoints.get(1).getTpId())
                    .build())
                .build();
    }
}
