/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.pce.input.valid.Factory;
import org.opendaylight.transportpce.pce.input.valid.Valid;
import org.opendaylight.transportpce.pce.input.valid.ValidInputFactory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class to check RPCs Compliancy.
 */
public final class PceComplianceCheck {

    private static final Logger LOG = LoggerFactory.getLogger(PceComplianceCheck.class);

    private PceComplianceCheck() {
    }

    /*
     * Check if a String is not null and not equal to ''.
     *
     * @param value String value
     *
     * @return  true  if String ok
     *          false if not
     */
    public static boolean checkString(String value) {
        return (value != null) && (value.compareTo("") != 0);
    }

    public static PceComplianceCheckResult check(PathComputationRequestInput input) {
        String message = "";
        Boolean result = true;
        if (input != null) {
            LOG.info("New request {} for new service {}",
                    input.getServiceHandlerHeader().getRequestId(), input.getServiceName());
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
        if (result) {

            Factory inputValidationFactory = new ValidInputFactory();
            Valid validInput = inputValidationFactory.instantiate(
                    GridConstant.START_EDGE_FREQUENCY,
                    GridConstant.CENTRAL_FREQUENCY,
                    GridConstant.GRANULARITY,
                    12.5,
                    GridConstant.EFFECTIVE_BITS
            );

            if (!validInput.isValid(input)) {
                result = false;
                message = validInput.lastErrorMessage();
                LOG.debug("PCEInput validation failed: {}", message);
            }

        }
        return new PceComplianceCheckResult(result, message);
    }

    public static PceComplianceCheckResult check(PathComputationRerouteRequestInput input) {
        if (input == null) {
            return new PceComplianceCheckResult(false, "");
        }
        if (input.getEndpoints() == null
                || input.getEndpoints().getAEndTp() == null
                || input.getEndpoints().getZEndTp() == null) {
            String message = "At least one of the termination points is missing";
            LOG.debug(message);
            return new PceComplianceCheckResult(false, message);
        }
        return new PceComplianceCheckResult(true, "");
    }

}
