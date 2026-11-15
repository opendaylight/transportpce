/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParseException;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiControllerService;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiDiscoveryService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.SyncTapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.SyncTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.SyncTapiSbiControllerOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.SyncTapiSbiControllerOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC implementation for syncing (re-discovering) a TAPI SBI controller.
 * This RPC removes all old stale data associated with the controller
 * (topology, nodes, links, profiles and service interface points) and
 * re-discovers them fresh from the lower controller. If discovery fails
 * partway, the partial data is cleaned up so the controller is left in
 * an empty state rather than a stale one.
 */
public class SyncTapiSbiControllerImpl implements SyncTapiSbiController {

    private static final Logger LOG = LoggerFactory.getLogger(SyncTapiSbiControllerImpl.class);

    private final TapiSbiControllerService controllerService;
    private final TapiSbiDiscoveryService discoveryService;

    public SyncTapiSbiControllerImpl(TapiSbiControllerService controllerService,
            TapiSbiDiscoveryService discoveryService) {
        this.controllerService = controllerService;
        this.discoveryService = discoveryService;
        LOG.info("SyncTapiSbiControllerImpl initialized");
    }

    @Override
    public ListenableFuture<RpcResult<SyncTapiSbiControllerOutput>> invoke(
            SyncTapiSbiControllerInput input) {

        LOG.info("Received sync-tapi-sbi-controller RPC request for controller-id: {}",
            input.getControllerId());

        if (input.getControllerId() == null || input.getControllerId().trim().isEmpty()) {
            LOG.error("Controller ID is required for sync");
            return RpcResultBuilder.<SyncTapiSbiControllerOutput>failed()
                .withError(ErrorType.PROTOCOL, "Controller ID is required")
                .buildFuture();
        }

        if (!controllerService.controllerExists(input.getControllerId())) {
            LOG.warn("Controller with ID {} does not exist, cannot sync", input.getControllerId());
            return RpcResultBuilder.<SyncTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION,
                    "Controller with ID " + input.getControllerId() + " not found, cannot sync")
                .buildFuture();
        }

        try {
            discoveryService.rediscoverController(input.getControllerId());
            LOG.info("Successfully synced TAPI SBI controller: {}", input.getControllerId());

            var output = new SyncTapiSbiControllerOutputBuilder()
                .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                    .setRequestId(input.getSdncRequestHeader() != null
                        ? input.getSdncRequestHeader().getRequestId() : "")
                    .setResponseCode(ResponseCodes.RESPONSE_OK)
                    .setResponseMessage("TAPI SBI controller synced successfully")
                    .build())
                .build();
            return RpcResultBuilder.success(output).buildFuture();

        } catch (IllegalArgumentException | IllegalStateException | JsonParseException e) {
            LOG.error("Failed to sync TAPI SBI controller: {}", input.getControllerId(), e);
            return RpcResultBuilder.<SyncTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION, "Failed to sync controller: " + e.getMessage())
                .buildFuture();
        }
    }
}
