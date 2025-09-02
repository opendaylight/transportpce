/*
 * Copyright © 2023 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation;

import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.CatalogInput;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.RpcActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CatalogValidation {

    private CatalogValidation() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(CatalogValidation.class);

    public static OperationResult validateORCatalogRequest(CatalogInput input, RpcActions rpcActions) {
        /*
         * -OR request header and operational mode info compliance are verified.
         */
        LOG.debug("checking OR Catalog Compliance ...");
        ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerComplianceCheck.checkORCatalog(
                input.getSdncRequestHeader(), input.getOperationalModeInfo(), rpcActions, true);
        if (!serviceHandlerCheckResult.hasPassed()) {
            return OperationResult.failed(serviceHandlerCheckResult.getMessage());
        }
        LOG.debug("Catalog OR request compliant !");
        return OperationResult.ok("Validation successful.");
    }

    public static OperationResult validateSpecificCatalogRequest(CatalogInput input, RpcActions rpcActions) {
        /*
         * -Specific request header and operational mode info compliance are verified.
         */
        LOG.debug("checking specific Catalog Compliance ...");
        ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerComplianceCheck.checkSpecificCatalog(
                input.getSdncRequestHeader(), input.getOperationalModeInfoSpecific(), rpcActions, true);
        if (!serviceHandlerCheckResult.hasPassed()) {
            return OperationResult.failed(serviceHandlerCheckResult.getMessage());
        }
        LOG.debug("Catalog specific request compliant !");
        return OperationResult.ok("Validation successful.");
    }
}
