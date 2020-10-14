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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.ServicePathInputData;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.DeviceRenderingRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.DeviceRenderingTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerSetupRollbackTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OlmPowerSetupTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.OtnDeviceRenderingTask;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.RollbackProcessor;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200615.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200615.olm.renderer.input.Nodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private ListeningExecutorService executor;
    private NetworkModelWavelengthService networkModelWavelengthService;
    private ServiceRpcResultSp notification = null;

    public RendererServiceOperationsImpl(DeviceRendererService deviceRenderer,
            OtnDeviceRendererService otnDeviceRenderer, TransportpceOlmService olmService,
            DataBroker dataBroker, NetworkModelWavelengthService networkModelWavelengthService,
            NotificationPublishService notificationPublishService) {
        this.deviceRenderer = deviceRenderer;
        this.otnDeviceRenderer = otnDeviceRenderer;
        this.olmService = olmService;
        this.dataBroker = dataBroker;
        this.networkModelWavelengthService = networkModelWavelengthService;
        this.notificationPublishService = notificationPublishService;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
    }

    private void sendNotifications(ServicePathNotificationTypes servicePathNotificationTypes, String serviceName,
            RpcStatusEx rpcStatusEx, String message) {
        this.notification = new ServiceRpcResultSpBuilder()
                .setNotificationType(servicePathNotificationTypes)
                .setServiceName(serviceName)
                .setStatus(rpcStatusEx)
                .setStatusMessage(message)
                .build();
        try {
            notificationPublishService.putNotification(this.notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected: ", e);
        }
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
                // Here is the switch statement that distinguishes on the connection-type
                LOG.info("Connection-type is {} for {}", input.getConnectionType(), input.getServiceName());
                switch (input.getConnectionType()) {
                    case Service: case RoadmLine: // This takes into account of Ethernet 100G, 1G, 10G and ODU4
                        LOG.info("RPC implementation for {}", input.getConnectionType());
                        if (((input.getServiceAEnd().getServiceRate() != null)
                            && (input.getServiceAEnd().getServiceRate().intValue() == 100))
                            && ((input.getServiceAEnd().getServiceFormat().getName().equals("Ethernet"))
                                || (input.getServiceAEnd().getServiceFormat().getName().equals("OC")))) {
                            LOG.info("Service format for {} is {} and rate is {}", input.getServiceName(),
                                input.getServiceAEnd().getServiceFormat(), input.getServiceAEnd().getServiceRate());
                            if (!createServicepathInput(input)) {
                                return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                        } else { // This implies, service-rate is 1 or 10G
                            // This includes the lower-order odu (1G, 10G) and this is A-Z side
                            LOG.info("RPC implementation for LO-ODU");
                            String serviceRate = ""; // Assuming service at A-side and Z-side has same service rate
                            if (input.getServiceAEnd().getServiceRate() != null) {
                                serviceRate = input.getServiceAEnd().getServiceRate().toString() + "G";
                            }
                            LOG.info("Start rendering for {} service with {} rate and {} format",
                                input.getServiceName(), serviceRate,
                                input.getServiceAEnd().getServiceFormat());
                            // This is A-Z side
                            OtnServicePathInput otnServicePathInputAtoZ = ModelMappingUtils
                                .rendererCreateOtnServiceInput(input.getServiceName(),
                                    input.getServiceAEnd().getServiceFormat().getName(),
                                    serviceRate, (PathDescription) input.getPathDescription(), true);
                            // Rollback should be same for all conditions, so creating a new one
                            RollbackProcessor rollbackProcessor = new RollbackProcessor();
                            List<OtnDeviceRenderingResult> otnRenderingResults = otnDeviceRendering(rollbackProcessor,
                                otnServicePathInputAtoZ, null);
                            if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
                                sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                                    input.getServiceName(), RpcStatusEx.Failed, DEVICE_RENDERING_ROLL_BACK_MSG);
                                return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                            LOG.info("OTN rendering result size {}", otnRenderingResults.size());
                            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                                input.getServiceName(), RpcStatusEx.Successful, OPERATION_SUCCESSFUL);
                        }
                        break;
                    case Infrastructure:
                        LOG.info("RPC implementation for {}", input.getConnectionType());
                        if ((input.getServiceAEnd().getOtuServiceRate() != null)
                            && (input.getServiceAEnd().getOtuServiceRate().equals(OTU4.class))) {
                            // For the service of OTU4 infrastructure
                            // First create the OCH and OTU interfaces
                            String serviceRate = "100G"; // For OtnDeviceRendererServiceImpl
                            if (!createServicepathInput(input)) {
                                return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                        }
                        if ((input.getServiceAEnd().getOduServiceRate() != null)
                            && (input.getServiceAEnd().getOduServiceRate().equals(ODU4.class))) {
                            // For the service of OTU4 infrastructure
                            // First create the OCH and OTU interfaces
                            String serviceRate = "100G"; // For OtnDeviceRendererServiceImpl
                            LOG.info("Service format for {} is {} and rate is {}", input.getServiceName(),
                                input.getServiceAEnd().getOduServiceRate(), serviceRate);
                            // Now start rendering ODU4 interface
                            // This is A-Z side
                            OtnServicePathInput otnServicePathInputAtoZ = ModelMappingUtils
                                .rendererCreateOtnServiceInput(input.getServiceName(),
                                    input.getServiceAEnd().getServiceFormat().getName(),
                                    serviceRate,
                                    input.getPathDescription(), true);
                            // This is Z-A side
                            OtnServicePathInput otnServicePathInputZtoA = ModelMappingUtils
                                .rendererCreateOtnServiceInput(input.getServiceName(),
                                    input.getServiceZEnd().getServiceFormat().getName(),
                                    serviceRate,
                                    input.getPathDescription(), false);
                            // Rollback should be same for all conditions, so creating a new one
                            RollbackProcessor rollbackProcessor = new RollbackProcessor();
                            List<OtnDeviceRenderingResult> otnRenderingResults = otnDeviceRendering(rollbackProcessor,
                                otnServicePathInputAtoZ, otnServicePathInputZtoA);
                            if (rollbackProcessor.rollbackAllIfNecessary() > 0) {
                                sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                                    input.getServiceName(), RpcStatusEx.Failed, DEVICE_RENDERING_ROLL_BACK_MSG);
                                return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                            LOG.info("OTN rendering result size {}", otnRenderingResults.size());
                            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                                input.getServiceName(), RpcStatusEx.Successful, OPERATION_SUCCESSFUL);
                        }
                        break;
                    default:
                        LOG.warn("Unsupported connection type {}", input.getConnectionType());
                }
                return ModelMappingUtils.createServiceImplResponse(ResponseCodes.RESPONSE_OK,
                    OPERATION_SUCCESSFUL);
            }
        });
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public OperationResult reserveResource(PathDescription pathDescription) {

        try {
            LOG.info("Reserving resources in network model");
            networkModelWavelengthService.useWavelengths(pathDescription);
        } catch (Exception e) {
            LOG.warn("Reserving resources in network model failed");
            return OperationResult.failed("Resources reserve failed in network model");
        }
        return OperationResult.ok("Resources reserved successfully in network model");
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public OperationResult freeResource(PathDescription pathDescription) {

        try {
            networkModelWavelengthService.freeWavelengths(pathDescription);
        } catch (Exception e) {
            return OperationResult.failed("Resources reserve failed in network model");
        }
        return OperationResult.ok("Resources reserved successfully in network model");
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
                PathDescription pathDescription;
                if (pathDescriptionOpt.isPresent()) {
                    pathDescription = pathDescriptionOpt.get();
                } else {
                    LOG.error("Unable to get path description for service {}!", serviceName);
                    sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName,
                            RpcStatusEx.Failed, "Unable to get path description for service");
                    return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                            OPERATION_FAILED);
                }
                switch (service.getConnectionType()) {
                    case RoadmLine:
                    case Service:
                        if ((ServiceFormat.Ethernet.equals(service.getServiceAEnd().getServiceFormat())
                                || ServiceFormat.OC.equals(service.getServiceAEnd().getServiceFormat()))
                            && Uint32.valueOf("100").equals(service.getServiceAEnd().getServiceRate())) {
                            if (!manageServicePathDeletion(serviceName, pathDescription)) {
                                return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                        }
                        if (ServiceFormat.Ethernet.equals(service.getServiceAEnd().getServiceFormat())
                            && (Uint32.valueOf("10").equals(service.getServiceAEnd().getServiceRate())
                                || Uint32.valueOf("1").equals(service.getServiceAEnd().getServiceRate()))) {
                            if (!manageOtnServicePathDeletion(serviceName, pathDescription, service)) {
                                return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                        }
                        break;
                    case Infrastructure:
                        if (ServiceFormat.OTU.equals(service.getServiceAEnd().getServiceFormat())) {
                            if (!manageServicePathDeletion(serviceName, pathDescription)) {
                                return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                        } else if (ServiceFormat.ODU.equals(service.getServiceAEnd().getServiceFormat())) {
                            if (!manageOtnServicePathDeletion(serviceName, pathDescription, service)) {
                                return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_FAILED,
                                    OPERATION_FAILED);
                            }
                        }
                        break;
                    default:
                        LOG.error("Unmanaged connection-type for deletion of service {}", serviceName);
                        break;
                    }
                return ModelMappingUtils.createServiceDeleteResponse(ResponseCodes.RESPONSE_OK, OPERATION_SUCCESSFUL);
            }
        });
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
        OtnServicePathInput otnServicePathAtoZ, OtnServicePathInput otnServicePathZtoA) {
        LOG.info(RENDERING_DEVICES_A_Z_MSG);
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
            otnServicePathAtoZ.getServiceName(), RpcStatusEx.Pending,
            RENDERING_DEVICES_A_Z_MSG);
        ListenableFuture<OtnDeviceRenderingResult> atozrenderingFuture =
            this.executor.submit(new OtnDeviceRenderingTask(this.otnDeviceRenderer, otnServicePathAtoZ));
        ListenableFuture<List<OtnDeviceRenderingResult>> renderingCombinedFuture;
        if (otnServicePathZtoA != null) {
            LOG.info("Rendering devices Z-A");
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                otnServicePathZtoA.getServiceName(), RpcStatusEx.Pending,
                RENDERING_DEVICES_Z_A_MSG);
            ListenableFuture<OtnDeviceRenderingResult> ztoarenderingFuture =
                this.executor.submit(new OtnDeviceRenderingTask(this.otnDeviceRenderer, otnServicePathZtoA));
            renderingCombinedFuture = Futures.allAsList(atozrenderingFuture, ztoarenderingFuture);
        } else {
            renderingCombinedFuture = Futures.allAsList(atozrenderingFuture);
        }
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
            if ((measurements != null) && verifyPreFecBer(measurements)) {
                return true;
            } else if (measurements == null) {
                LOG.warn("Device {} is not reporting PreFEC on TP: {}", nodeId, tpId);
                return true;
            } else {
                try {
                    Thread.sleep(Timeouts.SERVICE_ACTIVATION_TEST_RETRY_TIME);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        LOG.error("Service activation test failed on node {} and termination point {}!", nodeId, tpId);
        return false;
    }

    private List<Measurements> getMeasurements(String nodeId, String tp) {
        GetPmInputBuilder getPmIpBldr = new GetPmInputBuilder();
        getPmIpBldr.setNodeId(nodeId);
        getPmIpBldr.setGranularity(PmGranularity._15min);
        ResourceIdentifierBuilder rsrcBldr = new ResourceIdentifierBuilder();
        rsrcBldr.setResourceName(tp + "-OTU");
        getPmIpBldr.setResourceIdentifier(rsrcBldr.build());
        getPmIpBldr.setResourceType(ResourceTypeEnum.Interface);

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
            if (measurement.getPmparameterName().equals("preFECCorrectedErrors")) {
                preFecCorrectedErrors = Double.parseDouble(measurement.getPmparameterValue());
            }
            if (measurement.getPmparameterName().equals("FECUncorrectableBlocks")) {
                fecUncorrectableBlocks = Double.parseDouble(measurement.getPmparameterValue());
            }
        }

        LOG.info("Measurements: preFECCorrectedErrors = {}; FECUncorrectableBlocks = {}", preFecCorrectedErrors,
                fecUncorrectableBlocks);

        if (fecUncorrectableBlocks > Double.MIN_VALUE) {
            LOG.error("Data has uncorrectable errors, BER test failed");
            return false;
        } else {
            double numOfBitsPerSecond = 112000000000d;
            double threshold = 0.00002d;
            double result = preFecCorrectedErrors / numOfBitsPerSecond;
            LOG.info("PreFEC value is {}", Double.toString(result));
            return result <= threshold;
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean createServicepathInput(ServiceImplementationRequestInput input) {
        ServicePathInputData servicePathInputDataAtoZ = ModelMappingUtils
            .rendererCreateServiceInputAToZ(input.getServiceName(), input.getPathDescription());
        ServicePathInputData servicePathInputDataZtoA = ModelMappingUtils
            .rendererCreateServiceInputZToA(input.getServiceName(), input.getPathDescription());
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
        String srcNetworkTp;
        String dstNetowrkTp;
        if (sourceNode.getDestTp().contains(StringConstants.NETWORK_TOKEN)) {
            srcNetworkTp = sourceNode.getDestTp();
        } else {
            srcNetworkTp = sourceNode.getSrcTp();
        }
        if (destNode.getDestTp().contains(StringConstants.NETWORK_TOKEN)) {
            dstNetowrkTp = destNode.getDestTp();
        } else {
            dstNetowrkTp = destNode.getSrcTp();
        }
        if (!isServiceActivated(sourceNode.getNodeId(), srcNetworkTp)
            || !isServiceActivated(destNode.getNodeId(), dstNetowrkTp)) {
            rollbackProcessor.rollbackAll();
            sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
                input.getServiceName(), RpcStatusEx.Failed,
                "Service activation test failed.");
            return false;
        }
        // If Service activation is success update Network ModelMappingUtils
        networkModelWavelengthService.useWavelengths(input.getPathDescription());
        sendNotifications(ServicePathNotificationTypes.ServiceImplementationRequest,
            input.getServiceName(), RpcStatusEx.Successful, OPERATION_SUCCESSFUL);
        return true;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageServicePathDeletion(String serviceName, PathDescription pathDescription) {
        ServicePathInputData servicePathInputDataAtoZ =
            ModelMappingUtils.rendererCreateServiceInputAToZ(serviceName, pathDescription);
        ServicePathInputData servicePathInputDataZtoA =
            ModelMappingUtils.rendererCreateServiceInputZToA(serviceName, pathDescription);
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
        deviceRenderer.deleteServicePath(servicePathInputDataAtoZ.getServicePathInput());
        deviceRenderer.deleteServicePath(servicePathInputDataZtoA.getServicePathInput());
        networkModelWavelengthService.freeWavelengths(pathDescription);
        sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Successful,
                OPERATION_SUCCESSFUL);
        return true;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private boolean manageOtnServicePathDeletion(String serviceName, PathDescription pathDescription,
        Services service) {
        OtnServicePathInput ospi = null;
        if (ConnectionType.Infrastructure.equals(service.getConnectionType())) {
            ospi = ModelMappingUtils.rendererCreateOtnServiceInput(
                serviceName, service.getServiceAEnd().getServiceFormat().getName(), "100G", pathDescription, true);
        } else if (ConnectionType.Service.equals(service.getConnectionType())) {
            ospi = ModelMappingUtils.rendererCreateOtnServiceInput(serviceName,
                service.getServiceAEnd().getServiceFormat().getName(),
                service.getServiceAEnd().getServiceRate().toString() + "G", pathDescription, true);
        }
        LOG.info("Deleting otn-service path {} via renderer", serviceName);
        sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Pending,
                "Deleting otn-service path via renderer");
        OtnServicePathOutput result = otnDeviceRenderer.deleteOtnServicePath(ospi);
        if (result.isSuccess()) {
            sendNotifications(ServicePathNotificationTypes.ServiceDelete, serviceName, RpcStatusEx.Successful,
                OPERATION_SUCCESSFUL);
            return true;
        } else {
            return false;
        }
    }

}
