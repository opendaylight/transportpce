/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiControllerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiControllerOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiControllerOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC implementation for deleting a TAPI SBI controller.
 * Validates input, checks existence, then delegates removal to TapiSbiControllerService.
 * In the current phase, only TAPI SBI controller entry from the config datastore is deleted.
 * This is not designed to clear topology or other tapi contex as of now.
 */
public class DeleteTapiSbiControllerImpl implements DeleteTapiSbiController {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteTapiSbiControllerImpl.class);

    private final TapiSbiControllerService controllerService;

    public DeleteTapiSbiControllerImpl(TapiSbiControllerService controllerService) {
        this.controllerService = controllerService;
        LOG.info("DeleteTapiSbiControllerImpl initialized");
    }

    @Override
    public ListenableFuture<RpcResult<DeleteTapiSbiControllerOutput>> invoke(DeleteTapiSbiControllerInput input) {
        LOG.info("Received delete-tapi-sbi-controller RPC request for controller-id: {}", input.getControllerId());

        if (input.getControllerId() == null || input.getControllerId().trim().isEmpty()) {
            LOG.error("Controller ID is required for deletion");
            return RpcResultBuilder.<DeleteTapiSbiControllerOutput>failed()
                .withError(ErrorType.PROTOCOL, "Controller ID is required")
                .buildFuture();
        }

        if (!controllerService.controllerExists(input.getControllerId())) {
            LOG.warn("Controller with ID {} does not exist", input.getControllerId());
            return RpcResultBuilder.<DeleteTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION,
                    "Controller with ID " + input.getControllerId() + " not found")
                .buildFuture();
        }

        if (!controllerService.deleteController(input.getControllerId())) {
            LOG.error("Failed to delete TAPI SBI controller from datastore");
            return RpcResultBuilder.<DeleteTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION, "Failed to delete controller configuration")
                .buildFuture();
        }

        LOG.info("Successfully deleted TAPI SBI controller: {}", input.getControllerId());
        var output = new DeleteTapiSbiControllerOutputBuilder()
            .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                .setRequestId(input.getSdncRequestHeader() != null
                    ? input.getSdncRequestHeader().getRequestId() : "")
                .setResponseCode(ResponseCodes.RESPONSE_OK)
                .setResponseMessage("TAPI SBI controller deleted successfully")
                .build())
            .build();
        return RpcResultBuilder.success(output).buildFuture();
    }
}
