/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeader;

/**
 * Class for checking service sdnc-request-header compliancy.
 *
 */
public final class ServicehandlerCompliancyCheck {

    private static final String SERVICENAME_NOT_SET;
    private static final String CONNECTIONTYPE_NOT_SET;
    private static final String REQUESTID_NOT_SET;
    private static final String RPCACTION_NOT_SET;
    private static final String HEADER_NOT_SET;

    static {
        SERVICENAME_NOT_SET = "Service Name (common-id for Temp service) is not set";
        CONNECTIONTYPE_NOT_SET = "Service ConnectionType is not set";
        REQUESTID_NOT_SET = "Service sdncRequestHeader 'request-id' is not set";
        RPCACTION_NOT_SET = "Service sdncRequestHeader 'rpc-action' is not set";
        HEADER_NOT_SET = "Service sdncRequestHeader is not set";
    }

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
     * @return true if Service Request OK and false if not
     */
    public static ComplianceCheckResult check(String serviceName, SdncRequestHeader sdncRequestHeader,
                                       ConnectionType conType, RpcActions action,
                                       Boolean contype, Boolean sdncRequest) {
        if (!checkString(serviceName)) {
            return new ComplianceCheckResult(false, SERVICENAME_NOT_SET);
        }
        if (contype && (conType == null)) {
            return new ComplianceCheckResult(false, CONNECTIONTYPE_NOT_SET);
        }
        if (sdncRequest) {
            if (sdncRequestHeader == null) {
                return new ComplianceCheckResult(false, HEADER_NOT_SET);
            }
            RpcActions serviceAction = sdncRequestHeader.getRpcAction();
            String requestId = sdncRequestHeader.getRequestId();
            if (!checkString(requestId)) {
                return new ComplianceCheckResult(false, REQUESTID_NOT_SET);
            }
            if (serviceAction == null) {
                return new ComplianceCheckResult(false, RPCACTION_NOT_SET);
            }
            if (serviceAction.compareTo(action) != 0) {
                return new ComplianceCheckResult(false,
                    "Service sdncRequestHeader rpc-action '" + serviceAction
                        + "' not equal to '" + action.name() + "'");
            }
        }
        return new ComplianceCheckResult(true, "");
    }

    private ServicehandlerCompliancyCheck() {
    }

}
