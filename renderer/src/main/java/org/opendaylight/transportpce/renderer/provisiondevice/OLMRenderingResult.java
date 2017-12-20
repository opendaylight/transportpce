/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import org.opendaylight.transportpce.common.OperationResult;

public class OLMRenderingResult extends OperationResult {

    private OLMRenderingResult(boolean success, String message) {
        super(success, message);
    }

    public static OLMRenderingResult failed(String message) {
        return new OLMRenderingResult(false, message);
    }

    public static OLMRenderingResult ok() {
        return new OLMRenderingResult(true, "");
    }

}
