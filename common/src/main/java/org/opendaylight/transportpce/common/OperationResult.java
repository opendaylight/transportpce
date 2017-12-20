/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common;

public class OperationResult {

    private final boolean success;
    private final String resultMessage;

    protected OperationResult(boolean success, String resultMessage) {
        this.success = success;
        this.resultMessage = resultMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public static OperationResult failed(String message) {
        return new OperationResult(false, message);
    }

    public static OperationResult ok(String message) {
        return new OperationResult(true, message);
    }

}
