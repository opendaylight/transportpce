/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TempServiceDeleteImpl implements TempServiceDelete {
    private static final Logger LOG = LoggerFactory.getLogger(TempServiceDeleteImpl.class);
    private static final String TEMP_SERVICE_DELETE_MSG = "tempServiceDelete: {}";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PceListener pceListener;
    private RendererListener rendererListener;
    private RendererServiceWrapper rendererServiceWrapper;

    public TempServiceDeleteImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            final PceListener pceListener, RendererListener rendererListener,
            RendererServiceWrapper rendererServiceWrapper) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceListener = pceListener;
        this.rendererListener = rendererListener;
        this.rendererServiceWrapper = rendererServiceWrapper;
    }

    @Override
    public ListenableFuture<RpcResult<TempServiceDeleteOutput>> invoke(TempServiceDeleteInput input) {
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
        this.pceListener.setInput(new ServiceInput(input));
        this.pceListener.setServiceReconfigure(false);
        this.pceListener.setTempService(true);
        this.pceListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListener.setServiceInput(new ServiceInput(input));
        this.rendererListener.setTempService(true);
        this.rendererListener.setserviceDataStoreOperations(serviceDataStoreOperations);
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

}
