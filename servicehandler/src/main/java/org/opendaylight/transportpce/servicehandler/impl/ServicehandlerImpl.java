/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.CatalogInput;
import org.opendaylight.transportpce.servicehandler.DowngradeConstraints;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogMapper;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.CatalogValidation;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRerouteRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.path.computation.reroute.request.input.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalog;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalog;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalActivationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalActivationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalActivationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalDeactivationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalDeactivationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalDeactivationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalPerformanceInfoRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalPerformanceInfoRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalPerformanceInfoRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalPowerControl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalPowerControlInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EndTerminalPowerControlOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EquipmentNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EquipmentNotificationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.EquipmentNotificationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.NetworkReOptimization;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.NetworkReOptimizationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.NetworkReOptimizationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OpticalTunnelCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OpticalTunnelCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OpticalTunnelCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OpticalTunnelRequestCancel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OpticalTunnelRequestCancelInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OpticalTunnelRequestCancelOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateBulk;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateBulkInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateBulkOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateComplexResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateComplexResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateComplexResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteComplexResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteComplexResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteComplexResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheck;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckBulk;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckBulkInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckBulkOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigure;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureBulk;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureBulkInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureBulkOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReroute;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteConfirm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteConfirmInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteConfirmOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteConfirmResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteConfirmResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteConfirmResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestoration;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReversion;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReversionInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReversionOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReversionResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReversionResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReversionResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRoll;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRollInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRollOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRollResultNotificationRequest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRollResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRollResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceSrlgGet;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceSrlgGetInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceSrlgGetOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateBulk;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateBulkInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateBulkOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfo.TailRetention;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Top level service interface providing main OpenROADM controller services.
 */
@Component
public class ServicehandlerImpl implements OrgOpenroadmServiceService {
    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);
    private static final String PUBLISHER = "ServiceHandler";
    private static final String TEMP_SERVICE_CREATE_MSG = "tempServiceCreate: {}";
    private static final String TEMP_SERVICE_DELETE_MSG = "tempServiceDelete: {}";
    private static final String SERVICE_RESTORATION_MSG = "serviceRestoration: {}";
    private static final String SERVICE_RECONFIGURE_MSG = "serviceReconfigure: {}";
    private static final String SERVICE_FEASIBILITY_CHECK_MSG = "serviceFeasibilityCheck: {}";
    private static final String SERVICE_DELETE_MSG = "serviceDelete: {}";
    private static final String SERVICE_CREATE_MSG = "serviceCreate: {}";
    private static final String ADD_OR_TO_CATALOG_MSG = "addORToCatalog: {}";
    private static final String ADD_SPECIFIC_TO_CATALOG_MSG = "addSpecificToCatalog: {}";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private RendererServiceWrapper rendererServiceWrapper;
    private PceListener pceListenerImpl;
    private RendererListener rendererListenerImpl;
    private NetworkListener networkModelListenerImpl;
    private NotificationPublishService notificationPublishService;
    private CatalogDataStoreOperations catalogDataStoreOperations;
    private Registration reg;

    @Activate
    public ServicehandlerImpl(@Reference RpcProviderService rpcProviderService,
            @Reference PathComputationService pathComputationService,
            @Reference RendererServiceOperations rendererServiceOperations,
            @Reference NotificationPublishService notificationPublishService,
            @Reference PceListener pceListenerImpl,
            @Reference RendererListener rendererListenerImpl,
            @Reference NetworkListener networkModelListenerImpl,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference CatalogDataStoreOperations catalogDataStoreOperations) {
        this.catalogDataStoreOperations = catalogDataStoreOperations;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.notificationPublishService =  notificationPublishService;
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.rendererServiceWrapper = new RendererServiceWrapper(rendererServiceOperations, notificationPublishService);
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
        this.networkModelListenerImpl = networkModelListenerImpl;
        this.reg = rpcProviderService.registerRpcImplementations(registerRPCs());
        LOG.info("ServicehandlerImpl Initiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("ServicehandlerImpl Closed");
    }


    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        public static final String PCE_CALLING;
        public static final String ABORT_PCE_FAILED;
        public static final String PCE_FAILED;
        public static final String ABORT_SERVICE_NON_COMPLIANT;
        public static final String SERVICE_NON_COMPLIANT;
        public static final String RENDERER_DELETE_FAILED;
        public static final String ABORT_VALID_FAILED;
        public static final String ABORT_OR_TO_CATALOG_FAILED;
        public static final String ABORT_SPECIFIC_TO_CATALOG_FAILED;

        // Static blocks are generated once and spare memory.
        static {
            PCE_CALLING = "Calling PCE";
            ABORT_PCE_FAILED = "Aborting: PCE calculation failed ";
            PCE_FAILED = "PCE calculation failed";
            ABORT_SERVICE_NON_COMPLIANT = "Aborting: non-compliant service ";
            SERVICE_NON_COMPLIANT = "non-compliant service";
            RENDERER_DELETE_FAILED = "Renderer service delete failed";
            ABORT_VALID_FAILED = "Aborting: validation of service create request failed";
            ABORT_OR_TO_CATALOG_FAILED = "Aborting: validation of add OR to catalog request failed";
            ABORT_SPECIFIC_TO_CATALOG_FAILED = "Aborting: validation of add Specific to catalog request failed";
        }

        public static String serviceInDS(String serviceName) {
            return "Service '" + serviceName + "' already exists in datastore";
        }

        public static String serviceNotInDS(String serviceName) {
            return "Service '" + serviceName + "' does not exist in datastore";
        }

        public static String servicePathNotInDS(String serviceName) {
            return "Service Path from '" + serviceName + "' does not exist in datastore";
        }

        public static String serviceInService(String serviceName) {
            return "Service '" + serviceName + "' is in 'inService' state";
        }

        private LogMessages() {
        }
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreate(ServiceCreateInput input) {
        LOG.info("RPC serviceCreate received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.ServiceCreate);
        if (!validationResult.isSuccess()) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        //Check any presence of services with the same nameequipmentNotification
        String serviceName = input.getServiceName();
        if (this.serviceDataStoreOperations.getService(serviceName).isPresent()) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.serviceInDS(serviceName));
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceInDS(serviceName), ResponseCodes.RESPONSE_FAILED);
        }
        // TODO: Here we also have to check if there is an associated temp-service.
        // TODO: If there is one, delete it from the temp-service-list??
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setTempService(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        // This ensures that the temp-service boolean is false, especially, when
        // service-create is initiated after the temp-service-create
        this.rendererListenerImpl.setTempService(false);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        LOG.debug(SERVICE_CREATE_MSG, LogMessages.PCE_CALLING);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.ABORT_PCE_FAILED);
            sendNbiNotification(new PublishNotificationProcessServiceBuilder()
                    .setServiceName(serviceName)
                    .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                    .setCommonId(input.getCommonId())
                    .setConnectionType(input.getConnectionType())
                    .setResponseFailed(LogMessages.ABORT_PCE_FAILED)
                    .setMessage("ServiceCreate request failed ...")
                    .setOperationalState(State.Degraded)
                    .setPublisherName(PUBLISHER)
                    .build());
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC serviceCreate in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        String serviceName = input.getServiceDeleteReqInfo().getServiceName();
        LOG.info("RPC serviceDelete request received for {}", serviceName);

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliance are verified.
         */
        ComplianceCheckResult serviceHandlerCheckResult =
            ServicehandlerComplianceCheck.check(
                input.getServiceDeleteReqInfo().getServiceName(),
                input.getSdncRequestHeader(), null, RpcActions.ServiceDelete, false, true);
        if (!serviceHandlerCheckResult.hasPassed()) {
            LOG.warn(SERVICE_DELETE_MSG, LogMessages.ABORT_SERVICE_NON_COMPLIANT);
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.SERVICE_NON_COMPLIANT, ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        Optional<Services> serviceOpt = this.serviceDataStoreOperations.getService(serviceName);
        Services service;
        if (serviceOpt.isEmpty()) {
            LOG.warn(SERVICE_DELETE_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(serviceName), ResponseCodes.RESPONSE_FAILED);
        }
        service = serviceOpt.orElseThrow();
        LOG.debug("serviceDelete: Service '{}' found in datastore", serviceName);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setTempService(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setTempService(false);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput
                serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput output =
            this.rendererServiceWrapper.performRenderer(
                serviceDeleteInput, ServiceNotificationTypes.ServiceDeleteResult, service);

        if (output == null) {
            LOG.error(SERVICE_DELETE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            sendNbiNotification(new PublishNotificationProcessServiceBuilder()
                    .setServiceName(service.getServiceName())
                    .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                    .setCommonId(service.getCommonId())
                    .setConnectionType(service.getConnectionType())
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(State.InService)
                    .setResponseFailed(LogMessages.RENDERER_DELETE_FAILED)
                    .setPublisherName(PUBLISHER)
                    .build());
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.RENDERER_DELETE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }

        LOG.debug("RPC serviceDelete in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createDeleteServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceFeasibilityCheck(
            ServiceFeasibilityCheckInput input) {
        LOG.info("RPC serviceFeasibilityCheck received");
        // Validation
        ServiceInput serviceInput = new ServiceInput(input);
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(serviceInput,
                RpcActions.ServiceFeasibilityCheck);
        if (! validationResult.isSuccess()) {
            LOG.warn(SERVICE_FEASIBILITY_CHECK_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setServiceFeasiblity(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        LOG.debug(SERVICE_FEASIBILITY_CHECK_MSG, LogMessages.PCE_CALLING);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(SERVICE_FEASIBILITY_CHECK_MSG, LogMessages.ABORT_PCE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC serviceFeasibilityCheck in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public final ListenableFuture<RpcResult<
            ServiceReconfigureOutput>> serviceReconfigure(ServiceReconfigureInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceReconfigure received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (servicesObject.isEmpty()) {
            LOG.warn(SERVICE_RECONFIGURE_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createCreateServiceReply(
                input,
                LogMessages.serviceNotInDS(serviceName));
        }
        LOG.debug("Service '{}' found in datastore", serviceName);
        OperationResult validationResult = ServiceCreateValidation
                .validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceReconfigure);
        if (!validationResult.isSuccess()) {
            LOG.warn(SERVICE_RECONFIGURE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input,
                    validationResult.getResultMessage());
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                .ServiceDeleteInput serviceDeleteInput =
                        ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                        ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error(SERVICE_RECONFIGURE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input,
                    LogMessages.RENDERER_DELETE_FAILED);
                    //TODO check if RpcStatus.Successful is really expected here
        }
        LOG.info("RPC serviceReconfigure in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input,
                common.getResponseMessage());
    }

    @Override
    public final ListenableFuture<RpcResult<
            ServiceRestorationOutput>> serviceRestoration(ServiceRestorationInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceRestoration received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);

        if (!servicesObject.isPresent()) {
            LOG.warn(SERVICE_RESTORATION_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createRestoreServiceReply(
                    LogMessages.serviceNotInDS(serviceName));
        }

        Services service = servicesObject.orElseThrow();
        State state = service.getOperationalState();

        if (state == State.InService) {
            LOG.error(SERVICE_RESTORATION_MSG, LogMessages.serviceInService(serviceName));
            return ModelMappingUtils.createRestoreServiceReply(
                    LogMessages.serviceInService(serviceName));
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
                .setNotificationUrl(service.getSdncRequestHeader().getNotificationUrl())
                .setRequestId(service.getSdncRequestHeader().getRequestId())
                .setRequestSystemId(service.getSdncRequestHeader().getRequestSystemId())
                .setRpcAction(RpcActions.ServiceDelete);
        ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder()
                .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(serviceName)
                    .setDueDate(datetime)
                    .setTailRetention(TailRetention.No).build())
                .setSdncRequestHeader(sdncBuilder.build());
        ServiceInput serviceInput = new ServiceInput(deleteInputBldr.build());
        serviceInput.setServiceAEnd(service.getServiceAEnd());
        serviceInput.setServiceZEnd(service.getServiceZEnd());
        serviceInput.setConnectionType(service.getConnectionType());
        HardConstraints hardConstraints = service.getHardConstraints();
        if (hardConstraints == null) {
            LOG.warn("service '{}' HardConstraints is not set !", serviceName);
        } else {
            SoftConstraints softConstraints = service.getSoftConstraints();
            if (softConstraints == null) {
                LOG.warn("service '{}' SoftConstraints is not set !", serviceName);
                serviceInput.setSoftConstraints(DowngradeConstraints.convertToSoftConstraints(hardConstraints));
            } else {
                LOG.info("converting hard constraints to soft constraints ...");
                serviceInput.setSoftConstraints(
                        DowngradeConstraints.updateSoftConstraints(hardConstraints, softConstraints));
            }
            serviceInput.setHardConstraints(DowngradeConstraints.downgradeHardConstraints(hardConstraints));
        }
        this.pceListenerImpl.setInput(serviceInput);
        this.pceListenerImpl.setServiceReconfigure(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(serviceInput);
        this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(deleteInputBldr.build()));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error(SERVICE_RESTORATION_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createRestoreServiceReply(LogMessages.RENDERER_DELETE_FAILED);
        }
        LOG.info("RPC serviceRestore in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createRestoreServiceReply(common.getResponseMessage());

    }

    @Override
    public final ListenableFuture<RpcResult<EquipmentNotificationOutput>>
            equipmentNotification(EquipmentNotificationInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<EquipmentNotificationOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceRerouteConfirmOutput>>
            serviceRerouteConfirm(ServiceRerouteConfirmInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceRerouteConfirmOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceRerouteOutput>> serviceReroute(ServiceRerouteInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceReroute received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (servicesObject.isEmpty()) {
            LOG.warn("serviceReroute: {}", LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(serviceName),
                    ResponseCodes.RESPONSE_FAILED);
        }
        Services service = servicesObject.orElseThrow();
        Optional<ServicePaths> servicePathsObject = this.serviceDataStoreOperations.getServicePath(serviceName);
        if (servicePathsObject.isEmpty()) {
            LOG.warn("serviceReroute: {}", LogMessages.servicePathNotInDS(serviceName));
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.servicePathNotInDS(serviceName),
                    ResponseCodes.RESPONSE_FAILED);
        }
        ServicePaths servicePaths = servicePathsObject.orElseThrow();
        // serviceInput for later use maybe...
        ServiceInput serviceInput = new ServiceInput(input);
        serviceInput.setServiceAEnd(service.getServiceAEnd());
        serviceInput.setServiceZEnd(service.getServiceZEnd());
        serviceInput.setConnectionType(service.getConnectionType());
        serviceInput.setCommonId(service.getCommonId());
        serviceInput.setHardConstraints(service.getHardConstraints());
        serviceInput.setSoftConstraints(service.getSoftConstraints());
        serviceInput.setCustomer(service.getCustomer());
        serviceInput.setCustomerContact(service.getCustomerContact());

        // Get the network xpdr termination points
        Map<AToZKey, AToZ> mapaToz = servicePaths.getPathDescription().getAToZDirection().getAToZ();
        String aendtp = ((TerminationPoint) mapaToz.get(new AToZKey(String.valueOf(mapaToz.size() - 3)))
                .getResource()
                .getResource())
                .getTpId();
        String zendtp = ((TerminationPoint) mapaToz.get(new AToZKey("2"))
                .getResource()
                .getResource())
                .getTpId();

        PathComputationRerouteRequestOutput output = this.pceServiceWrapper.performPCEReroute(
                service.getHardConstraints(), service.getSoftConstraints(), input.getSdncRequestHeader(),
                service.getServiceAEnd(), service.getServiceZEnd(),
                new EndpointsBuilder().setAEndTp(aendtp).setZEndTp(zendtp).build());

        if (output == null) {
            LOG.error("serviceReroute: {}", LogMessages.PCE_FAILED);
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC ServiceReroute is done");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createRerouteServiceReply(input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceReversionOutput>> serviceReversion(ServiceReversionInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceReversionOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceRollOutput>> serviceRoll(ServiceRollInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceRollOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<NetworkReOptimizationOutput>>
            networkReOptimization(NetworkReOptimizationInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<NetworkReOptimizationOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<TempServiceDeleteOutput>> tempServiceDelete(TempServiceDeleteInput input) {
        String commonId = input.getCommonId();
        LOG.info("RPC temp serviceDelete request received for {}", commonId);

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliance are verified.
         */
        LOG.debug("checking Service Compliance ...");
        ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerComplianceCheck.check(
                commonId, null, null, RpcActions.ServiceDelete, false, false
            );
        if (!serviceHandlerCheckResult.hasPassed()) {
            LOG.warn(TEMP_SERVICE_DELETE_MSG, LogMessages.ABORT_SERVICE_NON_COMPLIANT);
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.SERVICE_NON_COMPLIANT, ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        LOG.debug("service common-id '{}' is compliant", commonId);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list.Services>
                serviceOpt =
            this.serviceDataStoreOperations.getTempService(commonId);
        if (serviceOpt.isEmpty()) {
            LOG.error(TEMP_SERVICE_DELETE_MSG, LogMessages.serviceNotInDS(commonId));
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(commonId), ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("Service '{}' present in datastore !", commonId);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setTempService(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.rendererListenerImpl.setTempService(true);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .temp.service.list.Services service = serviceOpt.orElseThrow();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput output =
                this.rendererServiceWrapper.performRenderer(input, ServiceNotificationTypes.ServiceDeleteResult,
                        service);
        if (output == null) {
            LOG.error(TEMP_SERVICE_DELETE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.RENDERER_DELETE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC tempServiceDelete in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createDeleteServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public final ListenableFuture<RpcResult<TempServiceCreateOutput>> tempServiceCreate(TempServiceCreateInput input) {
        LOG.info("RPC tempServiceCreate received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.TempServiceCreate);
        if (! validationResult.isSuccess()) {
            LOG.warn(TEMP_SERVICE_CREATE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }

        //Check any presence of temp-service with the same commonId
        String commonId = input.getCommonId();
        if (this.serviceDataStoreOperations.getTempService(commonId).isPresent()) {
            LOG.warn(TEMP_SERVICE_CREATE_MSG, LogMessages.serviceInDS("Temp (" + commonId + ")"));
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceInDS("Temp (" + commonId + ")"), ResponseCodes.RESPONSE_FAILED);
        }

        // Starting service create operation
        LOG.debug(TEMP_SERVICE_CREATE_MSG, LogMessages.PCE_CALLING);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.pceListenerImpl.setTempService(true);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.rendererListenerImpl.setTempService(true);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(TEMP_SERVICE_CREATE_MSG, LogMessages.ABORT_PCE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC tempServiceCreate in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public final ListenableFuture<RpcResult<
        ServiceDeleteComplexResultNotificationRequestOutput>> serviceDeleteComplexResultNotificationRequest(
            ServiceDeleteComplexResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceDeleteComplexResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<
        ServiceCreateResultNotificationRequestOutput>> serviceCreateResultNotificationRequest(
            ServiceCreateResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceCreateResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<
        ServiceDeleteResultNotificationRequestOutput>> serviceDeleteResultNotificationRequest(
            ServiceDeleteResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceDeleteResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<
        ServiceCreateComplexResultNotificationRequestOutput>> serviceCreateComplexResultNotificationRequest(
            ServiceCreateComplexResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceCreateComplexResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceFeasibilityCheckBulkOutput>> serviceFeasibilityCheckBulk(
        ServiceFeasibilityCheckBulkInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceFeasibilityCheckBulkOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceCreateBulkOutput>> serviceCreateBulk(ServiceCreateBulkInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceCreateBulkOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<TempServiceCreateBulkOutput>> tempServiceCreateBulk(
        TempServiceCreateBulkInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<TempServiceCreateBulkOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<
        ServiceRollResultNotificationRequestOutput>> serviceRollResultNotificationRequest(
            ServiceRollResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceRollResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceReconfigureBulkOutput>> serviceReconfigureBulk(
        ServiceReconfigureBulkInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceReconfigureBulkOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceReconfigureResultNotificationRequestOutput>>
            serviceReconfigureResultNotificationRequest(ServiceReconfigureResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceReconfigureResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceRestorationResultNotificationRequestOutput>>
            serviceRestorationResultNotificationRequest(ServiceRestorationResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceRestorationResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceReversionResultNotificationRequestOutput>>
            serviceReversionResultNotificationRequest(ServiceReversionResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceReversionResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceRerouteConfirmResultNotificationRequestOutput>>
            serviceRerouteConfirmResultNotificationRequest(ServiceRerouteConfirmResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceRerouteConfirmResultNotificationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<
            OpticalTunnelCreateOutput>> opticalTunnelCreate(OpticalTunnelCreateInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<OpticalTunnelCreateOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<OpticalTunnelRequestCancelOutput>> opticalTunnelRequestCancel(
            OpticalTunnelRequestCancelInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<OpticalTunnelRequestCancelOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    /**
     * Implementation of the RPC to set OR  operational modes in the catalog of the controller.
     * Semantics of the RPC is such that the information in the input replaces the full content
     * of the OR operational modes catalog in the config data store. Incremental changes to the
     * catalog, if required, must be done via individual PUT/POST/DELETE RESTconf APIs.
     *
     * @param input AddOpenroadmOperationalModesToCatalogInput to be added to Catalog
     * @return Result of the request
     */
    public final ListenableFuture<RpcResult<AddOpenroadmOperationalModesToCatalogOutput>>
        addOpenroadmOperationalModesToCatalog(AddOpenroadmOperationalModesToCatalogInput input) {

        LOG.info("RPC addOpenroadmOperationalModesToCatalog in progress");
        LOG.debug(" Input openRoadm {}", input);
        // Validation
        OperationResult validationResult = CatalogValidation.validateORCatalogRequest(
                new CatalogInput(input), RpcActions.FillCatalogWithOrOperationalModes);
        if (! validationResult.isSuccess()) {
            LOG.warn(ADD_OR_TO_CATALOG_MSG, LogMessages.ABORT_OR_TO_CATALOG_FAILED);
            return ModelMappingUtils.addOpenroadmServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info(" Request System Id {} " ,input.getSdncRequestHeader().getRequestSystemId());
        LOG.info(" Rpc Action {} " ,input.getSdncRequestHeader().getRpcAction());

        OpenroadmOperationalModes objToSave = CatalogMapper.createORModesToSave(input);
        catalogDataStoreOperations.addOpenroadmOperationalModesToCatalog(objToSave);
        LOG.info("RPC addOpenroadmOperationalModesToCatalog Completed");
        return ModelMappingUtils.addOpenroadmServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                validationResult.getResultMessage(), ResponseCodes.RESPONSE_OK);
    }

    @Override
    /**
     * Implementation of the RPC to set specific operational modes in the catalog of the controller.
     * Semantics of the RPC is such that the information in the input replaces the full content
     * of the specific operational modes catalog in the config data store. Incremental changes to the
     * catalog, if required, must be done via individual PUT/POST/DELETE RESTconf APIs.
     *
     * @param input AddSpecificOperationalModesToCatalogInput to be added to Catalog
     * @return Result of the request
     */
    public final ListenableFuture<RpcResult<AddSpecificOperationalModesToCatalogOutput>>
            addSpecificOperationalModesToCatalog(AddSpecificOperationalModesToCatalogInput input) {

        LOG.info("RPC addSpecificOperationalModesToCatalog in progress");
        LOG.debug(" Input openSpecificRoadm {}", input);
        // Validation
        OperationResult validationResult = CatalogValidation.validateSpecificCatalogRequest(
                new CatalogInput(input), RpcActions.FillCatalogWithSpecificOperationalModes);
        if (! validationResult.isSuccess()) {
            LOG.warn(ADD_SPECIFIC_TO_CATALOG_MSG, LogMessages.ABORT_SPECIFIC_TO_CATALOG_FAILED);
            return ModelMappingUtils.addSpecificOpenroadmServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info(" Request System Id {} " ,input.getSdncRequestHeader().getRequestSystemId());
        LOG.info(" Rpc Action {} " ,input.getSdncRequestHeader().getRpcAction());

        SpecificOperationalModes objToSave = CatalogMapper.createSpecificModesToSave(input);
        catalogDataStoreOperations.addSpecificOperationalModesToCatalog(objToSave);
        LOG.info("RPC addSpecificOperationalModesToCatalog Completed");
        return ModelMappingUtils.addSpecificOpenroadmServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                validationResult.getResultMessage(), ResponseCodes.RESPONSE_OK);
    }

    @Override
    public final ListenableFuture<RpcResult<ServiceSrlgGetOutput>> serviceSrlgGet(ServiceSrlgGetInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<ServiceSrlgGetOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<EndTerminalPerformanceInfoRequestOutput>> endTerminalPerformanceInfoRequest(
        EndTerminalPerformanceInfoRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<EndTerminalPerformanceInfoRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<EndTerminalActivationRequestOutput>> endTerminalActivationRequest(
            EndTerminalActivationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<EndTerminalActivationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<EndTerminalDeactivationRequestOutput>> endTerminalDeactivationRequest(
            EndTerminalDeactivationRequestInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<EndTerminalDeactivationRequestOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<EndTerminalPowerControlOutput>> endTerminalPowerControl(
            EndTerminalPowerControlInput input) {
        // TODO Auto-generated method stub
        return RpcResultBuilder.<EndTerminalPowerControlOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    public Registration getRegisteredRpc() {
        return reg;
    }

    /**
     * Send notification to NBI notification in order to publish message.
     * @param service PublishNotificationService
     */
    private void sendNbiNotification(PublishNotificationProcessService service) {
        try {
            notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }

    private ImmutableClassToInstanceMap<Rpc<?, ?>> registerRPCs() {
        return ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(ServiceCreate.class, this::serviceCreate)
            .put(ServiceDelete.class, this::serviceDelete)
            .put(ServiceFeasibilityCheck.class, this::serviceFeasibilityCheck)
            .put(ServiceReconfigure.class, this::serviceReconfigure)
            .put(ServiceRestoration.class, this::serviceRestoration)
            .put(EquipmentNotification.class, this::equipmentNotification)
            .put(ServiceRerouteConfirm.class, this::serviceRerouteConfirm)
            .put(ServiceReroute.class, this::serviceReroute)
            .put(ServiceReversion.class, this::serviceReversion)
            .put(ServiceRoll.class, this::serviceRoll)
            .put(NetworkReOptimization.class, this::networkReOptimization)
            .put(TempServiceDelete.class, this::tempServiceDelete)
            .put(TempServiceCreate.class, this::tempServiceCreate)
            .put(ServiceDeleteComplexResultNotificationRequest.class,
                this::serviceDeleteComplexResultNotificationRequest)
            .put(ServiceCreateResultNotificationRequest.class, this::serviceCreateResultNotificationRequest)
            .put(ServiceDeleteResultNotificationRequest.class, this::serviceDeleteResultNotificationRequest)
            .put(ServiceCreateComplexResultNotificationRequest.class,
                this::serviceCreateComplexResultNotificationRequest)
            .put(ServiceFeasibilityCheckBulk.class, this::serviceFeasibilityCheckBulk)
            .put(ServiceCreateBulk.class, this::serviceCreateBulk)
            .put(TempServiceCreateBulk.class, this::tempServiceCreateBulk)
            .put(ServiceRollResultNotificationRequest.class, this::serviceRollResultNotificationRequest)
            .put(ServiceReconfigureBulk.class, this::serviceReconfigureBulk)
            .put(ServiceReconfigureResultNotificationRequest.class, this::serviceReconfigureResultNotificationRequest)
            .put(ServiceRestorationResultNotificationRequest.class, this::serviceRestorationResultNotificationRequest)
            .put(ServiceReversionResultNotificationRequest.class, this::serviceReversionResultNotificationRequest)
            .put(ServiceRerouteConfirmResultNotificationRequest.class,
                this::serviceRerouteConfirmResultNotificationRequest)
            .put(OpticalTunnelCreate.class, this::opticalTunnelCreate)
            .put(OpticalTunnelRequestCancel.class, this::opticalTunnelRequestCancel)
            .put(AddOpenroadmOperationalModesToCatalog.class, this::addOpenroadmOperationalModesToCatalog)
            .put(AddSpecificOperationalModesToCatalog.class, this::addSpecificOperationalModesToCatalog)
            .put(ServiceSrlgGet.class, this::serviceSrlgGet)
            .put(EndTerminalPerformanceInfoRequest.class, this::endTerminalPerformanceInfoRequest)
            .put(EndTerminalActivationRequest.class, this::endTerminalActivationRequest)
            .put(EndTerminalDeactivationRequest.class, this::endTerminalDeactivationRequest)
            .put(EndTerminalPowerControl.class, this::endTerminalPowerControl)
            .build();
    }
}

