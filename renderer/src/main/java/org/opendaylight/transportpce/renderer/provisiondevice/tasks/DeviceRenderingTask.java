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
import java.util.concurrent.Callable;
import org.opendaylight.transportpce.renderer.ServicePathInputData;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRenderingResult;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.history.History;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRenderingTask implements Callable<DeviceRenderingResult> {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRenderingTask.class);

    private final DeviceRendererService deviceRenderer;
    private final ServicePathInputData servicePathInputData;
    private final ServicePathDirection direction;
    private final History transactionHistory;

    public DeviceRenderingTask(DeviceRendererService deviceRenderer, ServicePathInputData servicePathInputData,
            ServicePathDirection direction, History transactionHistory) {
        this.deviceRenderer = deviceRenderer;
        this.servicePathInputData = servicePathInputData;
        this.direction = direction;
        this.transactionHistory = transactionHistory;
    }

    @Override
    public DeviceRenderingResult call() throws Exception {
        ServicePathOutput output;
        String operation;
        List<Nodes> olmList = null;
        switch (this.servicePathInputData.getServicePathInput().getOperation()) {
            case Create:
                operation = "setup";
                output = this.deviceRenderer.setupServicePath(this.servicePathInputData.getServicePathInput(),
                    this.direction, transactionHistory);
                olmList = this.servicePathInputData.getNodeLists().getOlmNodeList();
                break;
            case Delete:
                operation = "delete";
                output = this.deviceRenderer.deleteServicePath(this.servicePathInputData.getServicePathInput());
                break;
            default:
                return DeviceRenderingResult.failed("Device rendering failed - unknown operation");
        }
        if (!output.getSuccess()) {
            LOG.error("Device rendering {} service path failed.", operation);
            return DeviceRenderingResult.failed(output.getResult());
        }
        LOG.info("Device rendering {} service path finished successfully.", operation);
        return DeviceRenderingResult.ok(olmList, new ArrayList<>(output.nonnullNodeInterface().values()),
            new ArrayList<>(output.nonnullLinkTp()));

    }

}
