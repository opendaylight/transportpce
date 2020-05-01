/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.validation.checks;

import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.ResilienceConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RestorationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResilienceConstraintCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ResilienceConstraintCheck.class);

    private ResilienceConstraintCheck() {
    }

    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    public static ComplianceCheckResult check(ResilienceConstraint rcl) {
        boolean result = true;
        String message = "";
        LOG.info("Inside resiliance constraint check");
        if (rcl != null) {
            if (!rcl.getResilienceType().getProtectionType().equals(ProtectionType.NOPROTECTON)) {
                result = false;
                message = "Protection resilience is not yet implemented";
            }
            if (!rcl.getResilienceType().getRestorationPolicy().equals(RestorationPolicy.NA)) {
                result = false;
                message = "Restoration policies are not yet implemented";
            }
        }
        LOG.info("Going to return result {} and message {}", result, message);
        return new ComplianceCheckResult(result, message);
    }
}
