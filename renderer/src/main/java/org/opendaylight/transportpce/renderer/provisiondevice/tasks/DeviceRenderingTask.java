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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.olm.renderer.input.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRenderingTask implements Callable<DeviceRenderingResult> {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRenderingTask.class);

    private final DeviceRendererService deviceRenderer;
    private final ServicePathInputData servicePathInputData;
    private final ServicePathDirection direction;

    public DeviceRenderingTask(DeviceRendererService deviceRenderer, ServicePathInputData servicePathInputData,
            ServicePathDirection direction) {
        this.deviceRenderer = deviceRenderer;
        this.servicePathInputData = servicePathInputData;
        this.direction = direction;
    }

    @Override
    public DeviceRenderingResult call() throws Exception {
        ServicePathOutput output = this.deviceRenderer.setupServicePath(this.servicePathInputData.getServicePathInput(),
                this.direction);
        if (!output.getSuccess()) {
            LOG.warn("Device rendering not successfully finished.");
            return DeviceRenderingResult.failed("Operation Failed");
        }
        List<Nodes> olmList = this.servicePathInputData.getNodeLists().getOlmList();
        LOG.info("Device rendering finished successfully.");
        return DeviceRenderingResult.ok(olmList, new ArrayList<>(output.nonnullNodeInterface().values()));
    }

}
