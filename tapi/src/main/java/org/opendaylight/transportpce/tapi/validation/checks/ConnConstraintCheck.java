/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.validation.checks;

import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.ConnectivityConstraint;

public final class ConnConstraintCheck {

    private ConnConstraintCheck() {
    }

    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    public static ComplianceCheckResult check(ConnectivityConstraint cc) {
        boolean result = true;
        String message = "";

        if (!cc.getServiceType().equals(ServiceType.POINTTOPOINTCONNECTIVITY)) {
            result = false;
            message = "Service-Type is not Point-to-Point";
        } else if (!checkString(cc.getServiceLevel())) {
            result = false;
            message = "Service-Level is not set";
        }

        return new ComplianceCheckResult(result, message);
    }
}
