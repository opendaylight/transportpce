/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TempServiceCreateImpl implements TempServiceCreate {
    private static final Logger LOG = LoggerFactory.getLogger(TempServiceCreateImpl.class);
    private static final String TEMP_SERVICE_CREATE_MSG = "tempServiceCreate: {}";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PceListener pceListener;
    private RendererListener rendererListener;
    private NetworkListener networkListener;
    private PCEServiceWrapper pceServiceWrapper;

    public TempServiceCreateImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            final PceListener pceListener, RendererListener rendererListener, NetworkListener networkListener,
            PCEServiceWrapper pceServiceWrapper) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceListener = pceListener;
        this.rendererListener = rendererListener;
        this.networkListener = networkListener;
        this.pceServiceWrapper = pceServiceWrapper;
    }

    @Override
    public ListenableFuture<RpcResult<TempServiceCreateOutput>> invoke(TempServiceCreateInput input) {
        LOG.info("RPC tempServiceCreate received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.TempServiceCreate);
        if (!validationResult.isSuccess()) {
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
        this.pceListener.setInput(new ServiceInput(input));
        this.pceListener.setServiceReconfigure(false);
        this.pceListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.pceListener.setServiceFeasiblity(false);
        this.pceListener.setTempService(true);
        this.rendererListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListener.setServiceInput(new ServiceInput(input));
        this.rendererListener.setTempService(true);
        this.networkListener.setserviceDataStoreOperations(serviceDataStoreOperations);
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

}
