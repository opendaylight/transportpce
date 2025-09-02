/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.OperationalModeInfo;

/**
 * Class for checking service sdnc-request-header compliance.
 *
 */
public final class ServicehandlerComplianceCheck {

    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        public static final String SERVICENAME_NOT_SET;
        public static final String CONNECTIONTYPE_NOT_SET;
        public static final String REQUESTID_NOT_SET;
        public static final String RPCACTION_NOT_SET;
        public static final String HEADER_NOT_SET;

        public static final String CATALOG_REQUESTID_NOT_SET;
        public static final String CATALOG_HEADER_NOT_SET;
        public static final String CATALOG_REQUESTSYSTEMID_NOT_SET;
        public static final String CATALOG_RPCACTION_NOT_SET;
        public static final String CATALOG_OPERATIONAL_MODE_INFO_NOT_SET;

        // Static blocks are generated once and spare memory.
        static {
            SERVICENAME_NOT_SET = "Service Name (common-id for Temp service) is not set";
            CONNECTIONTYPE_NOT_SET = "Service ConnectionType is not set";
            REQUESTID_NOT_SET = "Service sdncRequestHeader 'request-id' is not set";
            RPCACTION_NOT_SET = "Service sdncRequestHeader 'rpc-action' is not set";
            HEADER_NOT_SET = "Service sdncRequestHeader is not set";
            CATALOG_REQUESTID_NOT_SET = "sdnc-request-header 'request-id' is not set";
            CATALOG_REQUESTSYSTEMID_NOT_SET = "sdnc-request-header 'request-system-id' is not set";
            CATALOG_HEADER_NOT_SET = "sdnc-request-header is not set";
            CATALOG_RPCACTION_NOT_SET = "sdnc-request-header 'rpc-action' is not set";
            CATALOG_OPERATIONAL_MODE_INFO_NOT_SET = "operational-mode-info is not set";
        }

        public static String rpcactionsDiffers(RpcActions action1, RpcActions action2) {
            return
                "Service sdncRequestHeader rpc-action '" + action1.name() + "' not equal to '" + action2.name() + "'";
        }

        public static String catalogRpcactionsDiffers(RpcActions action1, RpcActions action2) {
            return
                "Catalog sdnc-request-header rpc-action '" + action1.name() + "' not equal to '" + action2.name() + "'";
        }

        private LogMessages() {
        }
    }

    /**
     * Check if a String is not null and not equal to void.
     *
     * @param value
     *            String value
     * @return true if String ok false if not
     */
    public static boolean checkString(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Check Compliance of Service request.
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
            return new ComplianceCheckResult(false, LogMessages.SERVICENAME_NOT_SET);
        }
        if (contype && (conType == null)) {
            return new ComplianceCheckResult(false, LogMessages.CONNECTIONTYPE_NOT_SET);
        }
        if (sdncRequest) {
            if (sdncRequestHeader == null) {
                return new ComplianceCheckResult(false, LogMessages.HEADER_NOT_SET);
            }
            if (!checkString(sdncRequestHeader.getRequestId())) {
                return new ComplianceCheckResult(false, LogMessages.REQUESTID_NOT_SET);
            }
            RpcActions serviceAction = sdncRequestHeader.getRpcAction();
            if (serviceAction == null) {
                return new ComplianceCheckResult(false, LogMessages.RPCACTION_NOT_SET);
            }
            if (serviceAction.compareTo(action) != 0) {
                return new ComplianceCheckResult(false, LogMessages.rpcactionsDiffers(serviceAction, action));
            }
        }
        return new ComplianceCheckResult(true, "");
    }

    public static ComplianceCheckResult checkORCatalog(SdncRequestHeader sdncRequestHeader,
                                                       OperationalModeInfo operationalModeInfo, RpcActions action,
                                                       Boolean sdncRequest) {
        String resultMsg = sdncRequestHeaderValidate(sdncRequest, sdncRequestHeader, action).getMessage();
        if (resultMsg.contains("sdnc-request-header")) {
            return new ComplianceCheckResult(false, resultMsg);
        }
        if (operationalModeInfo == null) {
            return new ComplianceCheckResult(false, LogMessages.CATALOG_OPERATIONAL_MODE_INFO_NOT_SET);
        }
        return new ComplianceCheckResult(true, "");
    }

    public static ComplianceCheckResult checkSpecificCatalog(SdncRequestHeader sdncRequestHeader,
            org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110
                .add.specific.operational.modes.to.catalog.input.OperationalModeInfo operationalModeInfoSpecific,
            RpcActions action, Boolean sdncRequest) {
        String resultMsg = sdncRequestHeaderValidate(sdncRequest, sdncRequestHeader, action).getMessage();
        if (resultMsg.contains("sdnc-request-header")) {
            return new ComplianceCheckResult(false,resultMsg);
        }
        if (operationalModeInfoSpecific == null) {
            return new ComplianceCheckResult(false, LogMessages.CATALOG_OPERATIONAL_MODE_INFO_NOT_SET);
        }
        return new ComplianceCheckResult(true, "");
    }

    public static ComplianceCheckResult sdncRequestHeaderValidate(Boolean sdncRequest,
            SdncRequestHeader sdncRequestHeader, RpcActions action) {
        if (sdncRequest) {
            if (sdncRequestHeader == null) {
                return new ComplianceCheckResult(false, LogMessages.CATALOG_HEADER_NOT_SET);
            }
            if (!checkString(sdncRequestHeader.getRequestId())) {
                return new ComplianceCheckResult(false, LogMessages.CATALOG_REQUESTID_NOT_SET);
            }
            if (!checkString(sdncRequestHeader.getRequestSystemId())) {
                return new ComplianceCheckResult(false, LogMessages.CATALOG_REQUESTSYSTEMID_NOT_SET);
            }
            RpcActions serviceAction = sdncRequestHeader.getRpcAction();
            if (serviceAction == null) {
                return new ComplianceCheckResult(false, LogMessages.CATALOG_RPCACTION_NOT_SET);
            }
            if (serviceAction.compareTo(action) != 0) {
                return new ComplianceCheckResult(false, LogMessages.catalogRpcactionsDiffers(serviceAction, action));
            }
        }
        return new ComplianceCheckResult(true, "");
    }

    private ServicehandlerComplianceCheck() {
    }

}
