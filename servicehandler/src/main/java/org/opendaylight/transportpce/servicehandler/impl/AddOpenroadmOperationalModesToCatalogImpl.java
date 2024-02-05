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
import org.opendaylight.transportpce.servicehandler.CatalogInput;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogMapper;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.validation.CatalogValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalog;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AddOpenroadmOperationalModesToCatalogImpl implements AddOpenroadmOperationalModesToCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(AddOpenroadmOperationalModesToCatalogImpl.class);
    private static final String ADD_OR_TO_CATALOG_MSG = "addORToCatalog: {}";

    private CatalogDataStoreOperations catalogDataStoreOperations;

    public AddOpenroadmOperationalModesToCatalogImpl(CatalogDataStoreOperations catalogDataStoreOperations) {
        this.catalogDataStoreOperations = catalogDataStoreOperations;
    }

    /**
     * Implementation of the RPC to set OR  operational modes in the catalog of the controller.
     * Semantics of the RPC is such that the information in the input replaces the full content
     * of the OR operational modes catalog in the config data store. Incremental changes to the
     * catalog, if required, must be done via individual PUT/POST/DELETE RESTconf APIs.
     *
     * @param input AddOpenroadmOperationalModesToCatalogInput to be added to Catalog
     * @return Result of the request
     */
    @Override
    public ListenableFuture<RpcResult<AddOpenroadmOperationalModesToCatalogOutput>> invoke(
            AddOpenroadmOperationalModesToCatalogInput input) {
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

}
