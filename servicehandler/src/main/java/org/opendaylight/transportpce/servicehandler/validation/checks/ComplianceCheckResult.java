/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

public class ComplianceCheckResult {

    private boolean passed;
    private String message;

    public ComplianceCheckResult(boolean result) {
        this.passed = result;
        this.message = "";
    }

    public ComplianceCheckResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasPassed() {
        return passed;
    }

}
