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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiControllerOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.DeleteTapiSbiControllerOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.TapiSbiControllers;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;
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
 * RPC implementation for deleting TAPI SBI controller.
 * This RPC removes a controller configuration from MD-SAL datastore based on controller-id.
 */
public class DeleteTapiSbiControllerImpl implements DeleteTapiSbiController {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteTapiSbiControllerImpl.class);

    private final DataBroker dataBroker;

    public DeleteTapiSbiControllerImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        LOG.info("DeleteTapiSbiControllerImpl initialized");
    }

    @Override
    public ListenableFuture<RpcResult<DeleteTapiSbiControllerOutput>> invoke(
            DeleteTapiSbiControllerInput input) {

        LOG.info("Received delete-tapi-sbi-controller RPC request for controller-id: {}",
                input.getControllerId());

        // Validate mandatory fields
        if (input.getControllerId() == null || input.getControllerId().trim().isEmpty()) {
            LOG.error("Controller ID is required for deletion");
            return RpcResultBuilder.<DeleteTapiSbiControllerOutput>failed()
                    .withError(ErrorType.PROTOCOL, "Controller ID is required")
                    .buildFuture();
        }

        // Check if controller exists before deletion
        if (!controllerExists(input.getControllerId())) {
            LOG.warn("Controller with ID {} does not exist", input.getControllerId());
            return RpcResultBuilder.<DeleteTapiSbiControllerOutput>failed()
                    .withError(ErrorType.APPLICATION,
                            "Controller with ID " + input.getControllerId() + " not found")
                    .buildFuture();
        }

        // Delete from MD-SAL datastore
        boolean success = deleteFromDatastore(input.getControllerId());

        if (success) {
            LOG.info("Successfully deleted TAPI SBI controller: {}", input.getControllerId());

            DeleteTapiSbiControllerOutput output = new DeleteTapiSbiControllerOutputBuilder()
                    .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                            .setRequestId(input.getSdncRequestHeader() != null
                                    ? input.getSdncRequestHeader().getRequestId() : "")
                            .setResponseCode(ResponseCodes.RESPONSE_OK)
                            .setResponseMessage("TAPI SBI controller deleted successfully")
                            .build())
                    .build();

            return RpcResultBuilder.success(output).buildFuture();

        } else {
            LOG.error("Failed to delete TAPI SBI controller from datastore");
            return RpcResultBuilder.<DeleteTapiSbiControllerOutput>failed()
                    .withError(ErrorType.APPLICATION, "Failed to delete controller configuration")
                    .buildFuture();
        }
    }

    /**
     * Checks if a controller with the given ID exists in the datastore.
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
     * Deletes the TAPI SBI controller configuration from MD-SAL datastore.
     */
    private boolean deleteFromDatastore(String controllerId) {
        DataObjectIdentifier<TapiSbiController> tapiSbiControllerIID = buildControllerIID(controllerId);

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, tapiSbiControllerIID);

        try {
            CommitInfo commitInfo = writeTransaction.commit().get();
            LOG.debug("TAPI SBI controller deletion committed successfully: {}", commitInfo);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to commit TAPI SBI controller deletion", e);
            return false;
        }
    }

    /**
     * Builds the DataObjectIdentifier for a controller with the given ID.
     */
    private DataObjectIdentifier<TapiSbiController> buildControllerIID(String controllerId) {
        return DataObjectIdentifier.builder(ControllerBehaviourSettings.class)
                .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                        .tapi.sbi.controller.rev260806.ControllerBehaviourSettings1.class)
                .child(TapiSbiControllers.class)
                .child(TapiSbiController.class, new TapiSbiControllerKey(controllerId))
                .build();
    }
}
