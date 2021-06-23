/*
 * Copyright © 2020 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRenderingResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtnDeviceRenderingTask implements Callable<OtnDeviceRenderingResult> {
    private static final Logger LOG = LoggerFactory.getLogger(OtnDeviceRenderingTask.class);

    private final OtnDeviceRendererService otnDeviceRenderer;
    private final OtnServicePathInput otnServicePathInput;

    public OtnDeviceRenderingTask(OtnDeviceRendererService otnDeviceRendererService,
            OtnServicePathInput otnServicePathInput) {
        this.otnDeviceRenderer = otnDeviceRendererService;
        this.otnServicePathInput = otnServicePathInput;
    }

    @Override
    public OtnDeviceRenderingResult call() throws Exception {
        OtnServicePathOutput output;
        switch (this.otnServicePathInput.getOperation()) {
            case Create:
                output = this.otnDeviceRenderer.setupOtnServicePath(this.otnServicePathInput);
                if (!output.getSuccess()) {
                    LOG.error("Device rendering setup otn service path failed.");
                    return OtnDeviceRenderingResult.failed("Operation Failed");
                }
                LOG.info("Device rendering setup otn service path finished successfully.");
                return OtnDeviceRenderingResult.ok(new ArrayList<>(output.nonnullNodeInterface().values()),
                    new ArrayList<>(output.nonnullLinkTp()));
            case Delete:
                output = this.otnDeviceRenderer.deleteOtnServicePath(this.otnServicePathInput);
                if (!output.getSuccess()) {
                    LOG.error("Device rendering delete otn service path failed.");
                    return OtnDeviceRenderingResult.failed("Operation Failed");
                }
                LOG.info("Device rendering delete otn service path finished successfully.");
                return OtnDeviceRenderingResult.ok(new ArrayList<>(output.nonnullNodeInterface().values()),
                    new ArrayList<>(output.nonnullLinkTp()));
            default:
                return OtnDeviceRenderingResult.failed("Device rendering failed - unknwon operation");
        }
    }
}
