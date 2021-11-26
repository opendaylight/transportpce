/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation;

import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.validation.checks.CheckCoherencyHardSoft;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerTxRxCheck;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.sdnc.request.header.SdncRequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceCreateValidation {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceCreateValidation.class);

    public static OperationResult validateServiceCreateRequest(ServiceInput input, RpcActions rpcActions) {
        /*
         * Upon receipt of service
         * -create RPC, service header and sdnc
         * -request header compliancy are verified.
         */
        LOG.debug("checking Service Compliance ...");
        try {
            String serviceNmame = input.getServiceName();
            SdncRequestHeader sdncRequestHeader = input.getSdncRequestHeader();
            ConnectionType conType = input.getConnectionType();
            ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerComplianceCheck.check(
                    serviceNmame, sdncRequestHeader, conType, rpcActions, true, true);
            if (serviceHandlerCheckResult.hasPassed()) {
                LOG.debug("Service request compliant !");
            } else {
                return OperationResult.failed(serviceHandlerCheckResult.getMessage());
            }
            /*
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */
            LOG.debug("checking Tx/Rx Info for AEnd ...");
            ComplianceCheckResult txrxCheckAEnd = ServicehandlerTxRxCheck.check(input.getServiceAEnd(),
                    ServiceEndpointType.SERVICEAEND);
            if (txrxCheckAEnd.hasPassed()) {
                LOG.debug("Tx/Rx Info for AEnd checked !");
            } else {
                return OperationResult.failed(txrxCheckAEnd.getMessage());
            }

            LOG.debug("checking Tx/Rx Info for ZEnd ...");
            ComplianceCheckResult txrxCheckZEnd = ServicehandlerTxRxCheck.check(input.getServiceZEnd(),
                    ServiceEndpointType.SERVICEZEND);
            if (txrxCheckZEnd.hasPassed()) {
                LOG.debug("Tx/Rx Info for ZEnd checked");
                /*
                 * If OK, common-id is verified in order to see if there is
                 * no routing policy provided. If yes, the routing
                 * constraints of the policy are recovered and coherency
                 * with hard/soft constraints provided in the input of the
                 * RPC.
                 */
            } else {
                return OperationResult.failed(txrxCheckZEnd.getMessage());
            }

            if (input.getCommonId() != null) {
                LOG.debug("Common-id specified");
                // Check coherency with hard/soft constraints
                if (CheckCoherencyHardSoft.check(input.getHardConstraints(), input.getSoftConstraints())) {
                    LOG.debug("hard/soft constraints coherent !");
                } else {
                    return OperationResult.failed("hard/soft constraints are not coherent !");
                }
            } else {
                LOG.warn("Common-id not specified !");
            }
        } catch (NullPointerException e) {
            LOG.error("one of input parameter is null ",e);
            return OperationResult.failed("one of input parameter is null.");
        }
        return OperationResult.ok("Validation successful.");
    }

    private ServiceCreateValidation() {
    }

}
