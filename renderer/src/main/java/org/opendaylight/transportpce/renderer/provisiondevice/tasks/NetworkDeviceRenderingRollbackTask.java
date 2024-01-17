/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.history.History;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetworkDeviceRenderingRollbackTask extends RollbackTask {

    private final History transactionHistory;

    private final boolean isRollbackNecessary;

    private final DeviceRendererService deviceRendererService;

    private final ResultMessage message;

    private static final Logger LOG = LoggerFactory.getLogger(NetworkDeviceRenderingRollbackTask.class);

    public NetworkDeviceRenderingRollbackTask(String id, History transactionHistory,
                                              boolean isRollbackNecessary,
                                              DeviceRendererService deviceRendererService, ResultMessage message) {
        super(id);
        this.transactionHistory = transactionHistory;
        this.isRollbackNecessary = isRollbackNecessary;
        this.deviceRendererService = deviceRendererService;
        this.message = message;
    }

    @Override
    public boolean isRollbackNecessary() {
        return isRollbackNecessary;
    }

    @Override
    public Void call() throws Exception {

        RendererRollbackOutput rollbackOutput = deviceRendererService.rendererRollback(transactionHistory);

        if (! rollbackOutput.getSuccess()) {
            LOG.warn("Device rendering rollback of {} was not successful! Failed rollback on {}.", this.getId(),
                    message.createErrorMessage(rollbackOutput.nonnullFailedToRollback().values()));
        } else {
            LOG.info("Device rollback of {} successful.", this.getId());
        }

        return null;
    }
}
