/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.Collection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.renderer.rollback.output.FailedToRollback;

public interface ResultMessage {

    /**
     * Build an error message for a failed rollback.
     */
    String createErrorMessage(Collection<FailedToRollback> failedRollbacks);

}