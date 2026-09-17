/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.tapisbi.utils.BasicEncryptionUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiControllerBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiControllerKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ControllerBehaviourSettings;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC implementation for configuring TAPI SBI controller.
 * This RPC stores controller configuration with encrypted password to MD-SAL datastore.
 */
public class ConfigureTapiSbiControllerImpl implements ConfigureTapiSbiController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigureTapiSbiControllerImpl.class);

    private final DataBroker dataBroker;

    public ConfigureTapiSbiControllerImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        LOG.info("ConfigureTapiSbiControllerImpl initialized");
    }

    @Override
    public ListenableFuture<RpcResult<ConfigureTapiSbiControllerOutput>> invoke(
            ConfigureTapiSbiControllerInput input) {

        LOG.info("Received configure-tapi-sbi-controller RPC request for controller-id: {}",
                input.getControllerId());
        // Validate mandatory fields
        String validationError = validateInput(input);
        if (validationError != null) {
            LOG.error("Input validation failed: {}", validationError);
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                    .withError(ErrorType.PROTOCOL, validationError)
                    .buildFuture();
        }

        // Check if controller already exists - prevent modification
        if (controllerExists(input.getControllerId())) {
            String errorMsg = "Controller with ID '" + input.getControllerId()
                    + "' already exists and cannot be modified. Please delete it first and then re-add.";
            LOG.error("Controller with ID '{}' already exists and cannot be modified", input.getControllerId());
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                    .withError(ErrorType.APPLICATION, errorMsg)
                    .buildFuture();
        }

        // Encrypt password before storing
        String encryptedPassword = encryptPassword(input.getPassword());
        // Build the TAPI SBI controller configuration
        TapiSbiController tapiSbiController = new TapiSbiControllerBuilder()
                .setControllerId(input.getControllerId())
                .setHost(input.getHost())
                .setPort(input.getPort())
                .setUsername(input.getUsername())
                .setPassword(encryptedPassword)
                .setControllerType(input.getControllerType() != null
                    ? input.getControllerType() : "transportpce")
                .setTapiVersion(input.getTapiVersion() != null
                    ? input.getTapiVersion() : "2.4.0")
                .build();
        // Write to MD-SAL datastore
        boolean success = writeToDatastore(tapiSbiController);

        if (success) {
            LOG.info("Successfully configured TAPI SBI controller: {}", input.getControllerId());

            ConfigureTapiSbiControllerOutput output = new ConfigureTapiSbiControllerOutputBuilder()
                    .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                            .setRequestId(input.getSdncRequestHeader() != null
                                    ? input.getSdncRequestHeader().getRequestId() : "")
                            .setResponseCode(ResponseCodes.RESPONSE_OK)
                            .setResponseMessage("TAPI SBI controller configured successfully")
                            .build())
                    .build();

            return RpcResultBuilder.success(output).buildFuture();

        } else {
            LOG.error("Failed to write TAPI SBI controller configuration to datastore");
            return RpcResultBuilder.<ConfigureTapiSbiControllerOutput>failed()
                    .withError(ErrorType.APPLICATION, "Failed to store configuration")
                    .buildFuture();
        }
    }

    /**
     * Validates the RPC input parameters.
     */
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
        return null; // Valid input
    }

    /**
     * Checks if a controller with the given ID already exists in the datastore.
     */
    private boolean controllerExists(String controllerId) {
        DataObjectIdentifier<TapiSbiController> tapiSbiControllerIID = buildControllerIID(controllerId);

        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, tapiSbiControllerIID)
                    .get()
                    .isPresent();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to check if controller exists", e);
            return false;
        }
    }

    /**
     * Encrypts the password before storing to datastore.
     * Uses AES-GCM encryption for secure password storage.
     */
    private String encryptPassword(String plainPassword) {
        return BasicEncryptionUtil.encryptPassword(plainPassword);
    }

    /**
     * Builds the DataObjectIdentifier for a controller with the given ID.
     */
    private DataObjectIdentifier<TapiSbiController> buildControllerIID(String controllerId) {
        return DataObjectIdentifier.builder(ControllerBehaviourSettings.class)
                .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                        .tapi.sbi.controller.rev260806.ControllerBehaviourSettings1.class)
                .child(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                        .tapi.sbi.controller.rev260806.controller.behaviour.settings.TapiSbiControllers.class)
                .child(TapiSbiController.class, new TapiSbiControllerKey(controllerId))
                .build();
    }

    /**
     * Writes the TAPI SBI controller configuration to MD-SAL datastore.
     * The controller is stored in a list, keyed by controller-id.
     */
    private boolean writeToDatastore(TapiSbiController tapiSbiController) {
        // Build the datastore path for list entry with controller-id as key
        DataObjectIdentifier<TapiSbiController> tapiSbiControllerIID =
                buildControllerIID(tapiSbiController.getControllerId());

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, tapiSbiControllerIID, tapiSbiController);

        try {
            CommitInfo commitInfo = writeTransaction.commit().get();
            LOG.debug("TAPI SBI controller configuration committed successfully: {}", commitInfo);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to commit TAPI SBI controller configuration", e);
            return false;
        }
    }
}
