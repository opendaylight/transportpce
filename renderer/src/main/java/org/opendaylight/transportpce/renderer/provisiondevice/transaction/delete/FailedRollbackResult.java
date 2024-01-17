/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.renderer.rollback.output.FailedToRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.renderer.rollback.output.FailedToRollbackBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.renderer.rollback.output.FailedToRollbackKey;

public class FailedRollbackResult implements Result {

    private final Map<String, Set<String>> failedRollback = Collections.synchronizedMap(
            new HashMap<>());

    @Override
    public boolean add(boolean success, String nodeId, String interfaceId) {

        if (success) {
            return false;
        }

        if (!failedRollback.containsKey(nodeId)) {
            failedRollback.put(nodeId, new LinkedHashSet<>());
        }

        return failedRollback.get(nodeId).add(interfaceId);
    }

    @Override
    public RendererRollbackOutput renderRollbackOutput() {

        Map<FailedToRollbackKey, FailedToRollback> failedToRollbackList = new HashMap<>();

        for (Entry<String, Set<String>> entry : failedRollback.entrySet()) {

            FailedToRollback failedToRollack = new FailedToRollbackBuilder()
                    .withKey(new FailedToRollbackKey(entry.getKey()))
                    .setNodeId(entry.getKey())
                    .setInterface(entry.getValue())
                    .build();

            failedToRollbackList.put(failedToRollack.key(), failedToRollack);

        }

        return new RendererRollbackOutputBuilder()
                .setSuccess(failedRollback.isEmpty())
                .setFailedToRollback(failedToRollbackList)
                .build();

    }

}