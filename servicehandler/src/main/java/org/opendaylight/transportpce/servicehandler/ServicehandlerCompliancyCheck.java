/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for checking service sdnc-request-header compliancy.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class ServicehandlerCompliancyCheck {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerCompliancyCheck.class);
    /** SdncRequestHeader. */
    private SdncRequestHeader sndcRequestHeader;
    /** Service Name. */
    private String serviceName;
    /** Type of connection : service / infrastructure / roadm-line. */
    private ConnectionType conType;
    /** type of service : service-create / service-delete ... */
    private RpcActions action;
    /** Response message from procedure. */
    private String message;

    /**
     * ServicehandlerCompliancyCheck class constructor.
     *
     * @param sdncRequestHeader
     *            SdncRequestHeader
     * @param serviceName
     *            String Service Name
     * @param conType
     *            Connection Type
     * @param action
     *            RPC type
     */
    public ServicehandlerCompliancyCheck(SdncRequestHeader sdncRequestHeader, String serviceName,
            ConnectionType conType, RpcActions action) {
        this.sndcRequestHeader = sdncRequestHeader;
        this.serviceName = serviceName;
        this.conType = conType;
        this.action = action;
        this.setMessage("");
    }

    /**
     * ServicehandlerCompliancyCheck class constructor.
     *
     * @param sdncRequestHeader
     *            SdncRequestHeader
     * @param serviceName
     *            String Service Name
     * @param action
     *            RPC type
     */
    public ServicehandlerCompliancyCheck(SdncRequestHeader sdncRequestHeader, String serviceName, RpcActions action) {
        this.sndcRequestHeader = sdncRequestHeader;
        this.serviceName = serviceName;
        this.action = action;
        this.setMessage("");
    }

    /**
     * ServicehandlerCompliancyCheck class constructor.
     *
     * @param serviceName
     *            String Service Name
     * @param conType
     *            Connection Type
     * @param action
     *            RPC type
     */
    public ServicehandlerCompliancyCheck(String serviceName, ConnectionType conType, RpcActions action) {
        this.serviceName = serviceName;
        this.conType = conType;
        this.action = action;
        this.setMessage("");
    }

    /**
     * ServicehandlerCompliancyCheck class constructor.
     *
     * @param serviceName
     *            String Service Name
     * @param action
     *            RPC type
     */
    public ServicehandlerCompliancyCheck(String serviceName, RpcActions action) {
        this.serviceName = serviceName;
        this.action = action;
        this.setMessage("");
    }

    /**
     * Check if a String is not null and not equal to void.
     *
     * @param value
     *            String value
     * @return true if String ok false if not
     */
    public Boolean checkString(String value) {
        Boolean result = false;
        if (value != null && value.compareTo("") != 0) {
            result = true;
        }
        return result;

    }

    /**
     * Check Compliancy of Service request.
     *
     * @param contype
     *            Boolean to check connection Type
     * @param sndcRequest
     *            Boolean to check sndcRequestHeader
     *
     * @return true if String ok false if not
     */
    public Boolean check(Boolean contype, Boolean sndcRequest) {
        Boolean result = true;
        if (!checkString(serviceName)) {
            result = false;
            message = "Service Name is not set";
            LOG.debug(message);
        } else if (contype && conType == null) {
            result = false;
            message = "Service ConnectionType is not set";
            LOG.debug(message);
        }
        if (sndcRequest) {
            if (sndcRequestHeader != null) {
                RpcActions serviceAction = sndcRequestHeader.getRpcAction();
                String requestId = sndcRequestHeader.getRequestId();
                if (!checkString(requestId)) {
                    result = false;
                    message = "Service sndcRequestHeader 'request-id' is not set";
                    LOG.debug(message);
                } else if (serviceAction != null) {
                    if (serviceAction.compareTo(action) != 0) {
                        result = false;
                        message = "Service sndcRequestHeader rpc-action '" + serviceAction + "' not equal to '"
                                + action.name() + "'";
                        LOG.debug(message);
                    }
                } else {
                    result = false;
                    message = "Service sndc-request-header 'rpc-action' is not set ";
                    LOG.debug(message);
                }

            } else {
                result = false;
                message = "Service sndc-request-header is not set ";
                LOG.debug(message);
            }
        }
        return result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
