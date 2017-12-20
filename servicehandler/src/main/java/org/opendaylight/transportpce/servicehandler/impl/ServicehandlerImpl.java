/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerCompliancyCheck;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.EquipmentNotificationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.EquipmentNotificationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.NetworkReOptimizationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.NetworkReOptimizationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteConfirmInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteConfirmOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReversionInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReversionOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRollInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRollOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfo.TailRetention;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Top level service interface providing main OpenROADM controller services.
 */
public class ServicehandlerImpl implements OrgOpenroadmServiceService {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);

    private DataBroker db;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private RendererServiceOperations rendererServiceOperations;
    private PCEServiceWrapper pceServiceWrapper;

    //TODO: remove private request fields as they are in global scope

    public ServicehandlerImpl(DataBroker databroker, PathComputationService pathComputationService,
                              RendererServiceOperations rendererServiceOperations) {
        this.db = databroker;
        this.rendererServiceOperations = rendererServiceOperations;
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(this.db);
        this.serviceDataStoreOperations.initialize();
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService);
    }

    @Override
    public Future<RpcResult<ServiceCreateOutput>> serviceCreate(ServiceCreateInput input) {
        LOG.info("RPC service creation received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(input);
        if (! validationResult.isSuccess()) {
            LOG.warn("Aborting service create because validation of service create request failed: {}",
                    validationResult.getResultMessage());
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }

        // Starting service create operation
        LOG.info("Commencing PCE");
        //TODO: createService service status into datastore
        PathComputationRequestOutput pceResponse = this.pceServiceWrapper.performPCE(input, true);
        String pceResponseCode = pceResponse.getConfigurationResponseCommon().getResponseCode();
        if (!ResponseCodes.RESPONSE_OK.equals(pceResponseCode)) {
            LOG.info("PCE calculation failed {}", pceResponseCode);
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    pceResponse.getConfigurationResponseCommon().getResponseMessage(), ResponseCodes.RESPONSE_FAILED);
        }

        LOG.info("PCE calculation done OK {}", pceResponseCode);

        OperationResult operationResult = this.serviceDataStoreOperations.createService(input, pceResponse);
        if (!operationResult.isSuccess()) {
            String message = "Service status not updated in datastore !";
            LOG.info(message);
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES, message,
                    ResponseCodes.RESPONSE_FAILED);
        }

        OperationResult operationServicePathSaveResult = this.serviceDataStoreOperations.createServicePath(input,
            pceResponse);
        if (!operationServicePathSaveResult.isSuccess()) {
            String message = "Service Path not updated in datastore !";
            LOG.info(message);
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES, message,
                    ResponseCodes.RESPONSE_FAILED);
        }

        ServiceImplementationRequestInput serviceImplementationRequest =
                ModelMappingUtils.createServiceImplementationRequest(input, pceResponse);
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
            .ServiceImplementationRequestOutput serviceImplementationRequestOutput = this.rendererServiceOperations
            .serviceImplementation(serviceImplementationRequest);
        if (ResponseCodes.RESPONSE_OK
                .equals(serviceImplementationRequestOutput.getConfigurationResponseCommon().getResponseCode())) {
            String message = "Service rendered successfully !";
            LOG.info(message);
            operationResult = this.serviceDataStoreOperations.modifyService(input.getServiceName(), State.InService,
                    State.InService);
            if (!operationResult.isSuccess()) {
                LOG.warn("Service status not updated in datastore !");
            }
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES, message,
                    ResponseCodes.RESPONSE_OK);
        } else {
            String message = "Service rendering has failed !";
            LOG.warn(message);

            OperationResult deleteServicePathOperationResult =
                    this.serviceDataStoreOperations.deleteServicePath(input.getServiceName());
            if (!deleteServicePathOperationResult.isSuccess()) {
                LOG.warn("Service path was not removed from datastore!");
            }

            OperationResult deleteServiceOperationResult =
                    this.serviceDataStoreOperations.deleteService(input.getServiceName());
            if (!deleteServiceOperationResult.isSuccess()) {
                LOG.warn("Service was not removed from datastore!");
            }

            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES, message,
                    ResponseCodes.RESPONSE_FAILED);
        }
    }

    @Override
    public Future<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        LOG.info("RPC serviceDelete request received for {}", input.getServiceDeleteReqInfo().getServiceName());
        String message = "";

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        LOG.info("checking Service Compliancy ...");
        ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerCompliancyCheck.check(
                input.getServiceDeleteReqInfo().getServiceName(),
                input.getSdncRequestHeader(), null, RpcActions.ServiceDelete, false, true);
        if (serviceHandlerCheckResult.hasPassed()) {
            LOG.info("Service compliant !");
        } else {
            LOG.info("Service is not compliant !");
            return ModelMappingUtils
                    .createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                            "Service not compliant !", ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        String serviceName = input.getServiceDeleteReqInfo().getServiceName();
        Optional<Services> service = this.serviceDataStoreOperations.getService(serviceName);
        if (!service.isPresent()) {
            message = "Service '" + serviceName + "' does not exist in datastore";
            LOG.error(message);
            return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    message, ResponseCodes.RESPONSE_FAILED);
        }

        LOG.debug("Service '{}' present in datastore !", serviceName);
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInput
                serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(input);
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
            .ServiceDeleteOutput output = this.rendererServiceOperations.serviceDelete(serviceDeleteInput);

        if (!ResponseCodes.RESPONSE_OK
                .equals(output.getConfigurationResponseCommon().getResponseCode())) {
            message = "Service delete failed!";
            return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES, message,
                    ResponseCodes.RESPONSE_FAILED);
        }

        OperationResult deleteServicePathOperationResult =
                this.serviceDataStoreOperations.deleteServicePath(input.getServiceDeleteReqInfo().getServiceName());
        if (!deleteServicePathOperationResult.isSuccess()) {
            LOG.warn("Service path was not removed from datastore!");
        }

        OperationResult deleteServiceOperationResult =
                this.serviceDataStoreOperations.deleteService(input.getServiceDeleteReqInfo().getServiceName());
        if (!deleteServiceOperationResult.isSuccess()) {
            LOG.warn("Service was not removed from datastore!");
        }

        return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                "Service delete was successful!", ResponseCodes.RESPONSE_OK);
    }

    @Override
    public Future<RpcResult<ServiceFeasibilityCheckOutput>> serviceFeasibilityCheck(
            ServiceFeasibilityCheckInput input) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Future<RpcResult<ServiceReconfigureOutput>> serviceReconfigure(ServiceReconfigureInput input) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Future<RpcResult<ServiceRestorationOutput>> serviceRestoration(ServiceRestorationInput input) {
        throw new UnsupportedOperationException("Not implemented yet");
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
        InstanceIdentifier<Services> servicesIID = InstanceIdentifier.create(ServiceList.class)
                .child(Services.class, new ServicesKey(input.getServiceName()));
        ReadOnlyTransaction rtx = this.db.newReadOnlyTransaction();
        Optional<Services> servicesObject;
        try {
            servicesObject = rtx.read(LogicalDatastoreType.CONFIGURATION, servicesIID).get().toJavaUtil();
            if (servicesObject.isPresent()) {
                ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ssX");
                LocalDateTime now = LocalDateTime.now();
                DateAndTime datetime = new DateAndTime(dtf.format(now));
                deleteInputBldr.setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(input.getServiceName()).setDueDate(datetime)
                    .setTailRetention(TailRetention.No).build());
                SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder();
                sdncBuilder.setNotificationUrl(servicesObject.get().getSdncRequestHeader().getNotificationUrl());
                sdncBuilder.setRequestId(servicesObject.get().getSdncRequestHeader().getRequestId());
                sdncBuilder.setRequestSystemId(servicesObject.get().getSdncRequestHeader().getRequestSystemId());
                sdncBuilder.setRpcAction(RpcActions.ServiceDelete);
                deleteInputBldr.setSdncRequestHeader(sdncBuilder.build());
                // Calling delete service
                serviceDelete(deleteInputBldr.build());
                // Calling create request now
                ServiceCreateInputBuilder serviceCreateBldr = new ServiceCreateInputBuilder();
                serviceCreateBldr.setServiceName(input.getServiceName() + 2);
                serviceCreateBldr.setCommonId(servicesObject.get().getCommonId());
                serviceCreateBldr.setConnectionType(servicesObject.get().getConnectionType());
                serviceCreateBldr.setCustomer(servicesObject.get().getCustomer());
                serviceCreateBldr.setCustomerContact(servicesObject.get().getCustomerContact());
                serviceCreateBldr.setDueDate(servicesObject.get().getDueDate());
                serviceCreateBldr.setEndDate(servicesObject.get().getEndDate());
                serviceCreateBldr.setHardConstraints(servicesObject.get().getHardConstraints());
                serviceCreateBldr.setNcCode(servicesObject.get().getNcCode());
                serviceCreateBldr.setNciCode(servicesObject.get().getNciCode());
                serviceCreateBldr.setOperatorContact(servicesObject.get().getOperatorContact());
                serviceCreateBldr.setSdncRequestHeader(servicesObject.get().getSdncRequestHeader());
                serviceCreateBldr.setSecondaryNciCode(servicesObject.get().getSecondaryNciCode());
                ServiceAEndBuilder serviceAendBuilder = new ServiceAEndBuilder(servicesObject.get().getServiceAEnd());
                serviceCreateBldr.setServiceAEnd(serviceAendBuilder.build());
                ServiceZEndBuilder serviceZendBuilder = new ServiceZEndBuilder(servicesObject.get().getServiceZEnd());
                serviceCreateBldr.setServiceZEnd(serviceZendBuilder.build());
                serviceCreateBldr.setSoftConstraints(servicesObject.get().getSoftConstraints());
                serviceCreate(serviceCreateBldr.build());
                ServiceRerouteOutputBuilder output = new ServiceRerouteOutputBuilder()
                    .setHardConstraints(null).setSoftConstraints(null).setStatus(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RpcStatus.Successful)
                    .setStatusMessage("Success");
                return RpcResultBuilder.success(output).buildFuture();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("Exception caught" , e);
        }
        ServiceRerouteOutputBuilder output = new ServiceRerouteOutputBuilder()
            .setHardConstraints(null).setSoftConstraints(null).setStatus(RpcStatus.Failed).setStatusMessage("Failure");

        return RpcResultBuilder.success(output).buildFuture();
        // return null;
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

}
