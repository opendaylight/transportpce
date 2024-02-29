/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.renderer.rollback.output.FailedToRollback;

public class RollbackResultMessage implements ResultMessage {

    @Override
    public String createErrorMessage(Collection<FailedToRollback> failedRollbacks) {
        List<String> failedRollbackNodes = new ArrayList<>();

        failedRollbacks.forEach(failedRollback -> {
            var intf = failedRollback.getInterface();

            failedRollbackNodes.add(
                failedRollback.getNodeId()
                    + ": "
                    + intf == null ? "" : String.join(", ", intf)
            );
        });

        return String.join(System.lineSeparator(), failedRollbackNodes);
    }

}
