/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for checking service sdnc-request-header compliancy.
 *
 */
public final class ServicehandlerCompliancyCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerCompliancyCheck.class);

    /**
     * Check if a String is not null and not equal to void.
     *
     * @param value
     *            String value
     * @return true if String ok false if not
     */
    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    /**
     * Check Compliancy of Service request.
     *
     * @param serviceName
     *            Service Name
     * @param sdncRequestHeader
     *            sdncRequestHeader
     * @param conType
     *            Connection type
     * @param action
     *            RPC Actions
     * @param contype
     *            Boolean to check connection Type
     * @param sdncRequest
     *            Boolean to check sdncRequestHeader
     *
     * @return true if String ok false if not
     */
    public static ComplianceCheckResult check(String serviceName, SdncRequestHeader sdncRequestHeader,
                                       ConnectionType conType, RpcActions action,
                                       Boolean contype, Boolean sdncRequest) {
        boolean result = true;
        String message = "";
        if (!checkString(serviceName)) {
            result = false;
            message = "Service Name is not set";
        } else if (contype && (conType == null)) {
            result = false;
            message = "Service ConnectionType is not set";
        }
        if (sdncRequest) {
            if (sdncRequestHeader != null) {
                RpcActions serviceAction = sdncRequestHeader.getRpcAction();
                String requestId = sdncRequestHeader.getRequestId();
                if (!checkString(requestId)) {
                    result = false;
                    message = "Service sdncRequestHeader 'request-id' is not set";
                    LOG.debug(message);
                } else if (serviceAction != null) {
                    if (serviceAction.compareTo(action) != 0) {
                        result = false;
                        message = "Service sdncRequestHeader rpc-action '" + serviceAction + "' not equal to '"
                                + action.name() + "'";
                    }
                } else {
                    result = false;
                    message = "Service sndc-request-header 'rpc-action' is not set ";
                }

            } else {
                result = false;
                message = "Service sndc-request-header is not set ";
            }
        }
        LOG.debug(message);
        return new ComplianceCheckResult(result, message);
    }

    private ServicehandlerCompliancyCheck() {
    }

}
