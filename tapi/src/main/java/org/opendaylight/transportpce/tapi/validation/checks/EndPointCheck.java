/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.validation.checks;

import java.util.List;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EndPointCheck {

    private static final Logger LOG = LoggerFactory.getLogger(EndPointCheck.class);

    private EndPointCheck() {
    }

    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    public static ComplianceCheckResult check(List<EndPoint> epl) {
        boolean result = true;
        String message = "";

        if (epl.isEmpty()) {
            result = false;
            message = "Service End-Point must be set";
        } else {
            for (EndPoint ep : epl) {
                //to do
                LOG.info("ep = {}", ep);
            }
        }

        return new ComplianceCheckResult(result, message);
    }
}
