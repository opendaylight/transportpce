/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;

public interface Valid extends Observer {

    /**
     * Validate pathComputationRequestInput.
     * Subsequent calls to this method will reset any error message held withing this object.
     *
     * @param pathComputationRequestInput Api input data to be validated.
     * @return true if the validation passed.
     * @see Valid#lastErrorMessage()
     */
    boolean isValid(PathComputationRequestInput pathComputationRequestInput);

    /**
     * Any error during validation will be accessible using this method.
     * Subsequent calls to the valid method will reset the error tracking message.
     *
     * @return An error message.
     */
    String lastErrorMessage();

}
