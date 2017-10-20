/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for checking service sdnc-request-header compliancy.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class StubpceCompliancyCheck {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubpceCompliancyCheck.class);
    /** SdncRequestHeader. */
    private ServiceHandlerHeader serviceHandlerHeader;
    /** Service Name. */
    private String serviceName;
    /** Type of connection : service / infrastructure / roadm-line. */
    private ConnectionType conType;
    /** Response message from procedure. */
    private String message;


    public StubpceCompliancyCheck(String serviceName,ServiceHandlerHeader serviceHandlerHeader) {
        this.serviceName = serviceName;
        this.serviceHandlerHeader = serviceHandlerHeader;
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
     * @param servicehandler
     *            Boolean to check sndcRequestHeader
     *
     * @return true if String ok false if not
     */
    public Boolean check(Boolean contype, Boolean servicehandler) {
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
        if (servicehandler) {
            if (serviceHandlerHeader != null) {
                String requestId = serviceHandlerHeader.getRequestId();
                if (!checkString(requestId)) {
                    result = false;
                    message = "Service serviceHandlerHeader 'request-id' is not set";
                    LOG.debug(message);
                }
            } else {
                result = false;
                message = "Service serviceHandlerHeader is not set ";
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
