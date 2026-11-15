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
import org.opendaylight.transportpce.tapisbi.utils.BasicEncryptionUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiControllerBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC implementation for configuring (registering) a TAPI SBI controller.
 * This stores controller configuration with encrypted password to MD-SAL datastore.
 */
public class ConfigureTapiSbiControllerImpl implements ConfigureTapiSbiController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigureTapiSbiControllerImpl.class);

    private final TapiSbiControllerService controllerService;

    public ConfigureTapiSbiControllerImpl(TapiSbiControllerService controllerService) {
        this.controllerService = controllerService;
        LOG.info("ConfigureTapiSbiControllerImpl initialized");
    }

    @Override
    public ListenableFuture<RpcResult<ConfigureTapiSbiControllerOutput>> invoke(
            ConfigureTapiSbiControllerInput input) {
        LOG.info("Received configure-tapi-sbi-controller RPC request for controller-id: {}", input.getControllerId());

        String validationError = validateInput(input);
        if (validationError != null) {
            LOG.error("Input validation failed: {}", validationError);
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                .withError(ErrorType.PROTOCOL, validationError)
                .buildFuture();
        }

        if (controllerService.controllerExists(input.getControllerId())) {
            String errorMessage = "Controller with ID '" + input.getControllerId()
                + "' already exists. Please delete it first and then re-add.";
            LOG.error("Controller with ID '{}' already exists", input.getControllerId());
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION, errorMessage)
                .buildFuture();
        }

        if (controllerService.controllerExistsByHostAndPort(
                input.getHost().stringValue(), input.getPort().getValue().toString())) {
            String errorMessage = "A controller with host '" + input.getHost()
                + "' and port '" + input.getPort() + "' already exists.";
            LOG.error("A controller with host '{}' and port '{}' already exists",
                input.getHost(), input.getPort());
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION, errorMessage)
                .buildFuture();
        }

        var controller = new TapiSbiControllerBuilder()
            .setControllerId(input.getControllerId())
            .setHost(input.getHost())
            .setPort(input.getPort())
            .setUsername(input.getUsername())
            .setPassword(BasicEncryptionUtil.encryptPassword(input.getPassword()))
            .setControllerType(input.getControllerType() != null ? input.getControllerType() : "transportpce")
            .setTapiVersion(input.getTapiVersion() != null ? input.getTapiVersion() : "2.4.0")
            .build();

        if (!controllerService.saveController(controller)) {
            LOG.error("Failed to write TAPI SBI controller configuration to datastore");
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                .withError(ErrorType.APPLICATION, "Failed to store configuration")
                .buildFuture();
        }

        LOG.info("Successfully configured TAPI SBI controller: {}", input.getControllerId());
        var output = new ConfigureTapiSbiControllerOutputBuilder()
            .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                .setRequestId(input.getSdncRequestHeader() != null
                    ? input.getSdncRequestHeader().getRequestId() : "")
                .setResponseCode(ResponseCodes.RESPONSE_OK)
                .setResponseMessage("TAPI SBI controller configured successfully")
                .build())
            .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    private String validateInput(ConfigureTapiSbiControllerInput input) {
        if (input.getHost() == null) {
            return "Host is required";
        }
        if (input.getUsername() == null || input.getUsername().trim().isEmpty()) {
            return "Username is required";
        }
        if (input.getPassword() == null || input.getPassword().trim().isEmpty()) {
            return "Password is required";
        }
        if (input.getControllerId() == null || input.getControllerId().trim().isEmpty()) {
            return "Controller ID is required";
        }
        return null;
    }
}
