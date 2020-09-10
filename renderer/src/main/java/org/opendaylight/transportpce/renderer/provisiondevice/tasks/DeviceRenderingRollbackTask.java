/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.renderer.rollback.output.FailedToRollback;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200615.node.interfaces.NodeInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRenderingRollbackTask extends RollbackTask {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRenderingRollbackTask.class);
    private final boolean isRollbackNecessary;
    private final DeviceRendererService rendererService;
    private final List<NodeInterface> renderedInterfaces;

    public DeviceRenderingRollbackTask(String id, boolean isRollbackNecessary, List<NodeInterface> renderedInterfaces,
            DeviceRendererService rendererService) {
        super(id);
        this.isRollbackNecessary = isRollbackNecessary;
        this.rendererService = rendererService;
        this.renderedInterfaces = renderedInterfaces;
    }

    @Override
    public boolean isRollbackNecessary() {
        return isRollbackNecessary;
    }

    @Override
    public Void call() throws Exception {
        RendererRollbackInput rollbackInput = new RendererRollbackInputBuilder()
                .setNodeInterface(this.renderedInterfaces)
                .build();
        RendererRollbackOutput rollbackOutput = this.rendererService.rendererRollback(rollbackInput);
        if (! rollbackOutput.isSuccess()) {
            LOG.warn("Device rendering rollback of {} was not successful! Failed rollback on {}.", this.getId(),
                    createErrorMessage(rollbackOutput.getFailedToRollback()));
        } else {
            LOG.info("Device rollback of {} successful.", this.getId());
        }
        return null;
    }

    private String createErrorMessage(List<FailedToRollback> failedRollbacks) {
        List<String> failedRollbackNodes = new ArrayList<>();
        failedRollbacks.forEach(failedRollback -> {
            String nodeId = failedRollback.getNodeId();
            failedRollbackNodes.add(nodeId + ": " + String.join(", ", failedRollback.getInterface()));
        });
        return String.join(System.lineSeparator(), failedRollbackNodes);
    }

}
