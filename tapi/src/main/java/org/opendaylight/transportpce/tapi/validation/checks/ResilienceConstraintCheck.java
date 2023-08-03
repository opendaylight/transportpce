/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.validation.checks;

import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.ResilienceConstraint;

public final class ResilienceConstraintCheck {

    private ResilienceConstraintCheck() {
    }

    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    public static ComplianceCheckResult check(ResilienceConstraint rcl) {
        boolean result = true;
        String message = "";

        if (rcl != null) {
            result = false;
            message = "Resilience constraints are not managet yet";
        }

        return new ComplianceCheckResult(result, message);
    }
}
