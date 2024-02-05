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
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalog;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AddSpecificOperationalModesToCatalogImpl implements AddSpecificOperationalModesToCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(AddSpecificOperationalModesToCatalogImpl.class);
    private static final String ADD_SPECIFIC_TO_CATALOG_MSG = "addSpecificToCatalog: {}";

    private CatalogDataStoreOperations catalogDataStoreOperations;

    public AddSpecificOperationalModesToCatalogImpl(CatalogDataStoreOperations catalogDataStoreOperations) {
        this.catalogDataStoreOperations = catalogDataStoreOperations;
    }

    /**
     * Implementation of the RPC to set specific operational modes in the catalog of the controller.
     * Semantics of the RPC is such that the information in the input replaces the full content
     * of the specific operational modes catalog in the config data store. Incremental changes to the
     * catalog, if required, must be done via individual PUT/POST/DELETE RESTconf APIs.
     *
     * @param input AddSpecificOperationalModesToCatalogInput to be added to Catalog
     * @return Result of the request
     */
    @Override
    public ListenableFuture<RpcResult<AddSpecificOperationalModesToCatalogOutput>> invoke(
            AddSpecificOperationalModesToCatalogInput input) {
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

}
