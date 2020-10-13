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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.OtnServicePathOutput;
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
        OtnServicePathOutput output = this.otnDeviceRenderer.setupOtnServicePath(this.otnServicePathInput);
        if (Boolean.TRUE.equals(output.isSuccess())) {
            LOG.info("Device rendering finished successfully.");
            return OtnDeviceRenderingResult.ok(new ArrayList<>(output.nonnullNodeInterface().values()));

        } else {
            //false or null case
            LOG.warn("Device rendering not successfully finished.");
            return OtnDeviceRenderingResult.failed("Operation Failed");
        }
    }

}
