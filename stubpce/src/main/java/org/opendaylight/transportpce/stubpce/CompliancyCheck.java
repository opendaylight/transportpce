/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubpce;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Class to check RPCs Compliancy.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class CompliancyCheck {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(CompliancyCheck.class);
    /* Response message from procedure. */
    private String message;

    private PathComputationRequestInput input;

    public CompliancyCheck(PathComputationRequestInput prcInput) {
        input = prcInput;
    }

    /*
     * Check if a String is not
     * null and not equal to ''.
     *
     * @param value String value
     * @return  true  if String ok
     *          false if not
     */
    public Boolean checkString(String value) {
        Boolean result = false;
        if ((value != null) && (value.compareTo("") != 0)) {
            result = true;
        }
        return result;

    }

    public Boolean check() {
        Boolean result = true;
        if (input != null) {
            if (!checkString(input.getServiceName())) {
                result = false;
                message = "Service Name is not set";
                LOG.debug(message);
            } else {
                if (!checkString(input.getServiceHandlerHeader().getRequestId())) {
                    result = false;
                    message = "ServiceHandlerHeader Request-ID  is not set";
                    LOG.debug(message);
                }
            }
        } else {
            result = false;
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
