/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.renderer.provisiondevice.OLMRenderingResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmPowerSetupTask implements Callable<OLMRenderingResult> {

    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerSetupTask.class);

    private final TransportpceOlmService olmService;
    private final ServicePowerSetupInput input;

    public OlmPowerSetupTask(TransportpceOlmService olmService, ServicePowerSetupInput input) {
        this.olmService = olmService;
        this.input = input;
    }

    @Override
    public OLMRenderingResult call() throws Exception {
        Future<RpcResult<ServicePowerSetupOutput>> fr = this.olmService.servicePowerSetup(this.input);
        RpcResult<ServicePowerSetupOutput> result = fr.get();
        if (result == null) {
            LOG.warn("Result is NULL");
            return OLMRenderingResult.failed("Operation Failed");
        }

        LOG.debug("Result: {}", result.getResult());
        if (ResponseCodes.SUCCESS_RESULT.equals(result.getResult().getResult())) {
            LOG.info("OLM power setup finished successfully");
            return OLMRenderingResult.ok();
        } else {
            LOG.warn("OLM power setup not successfully finished");
            return OLMRenderingResult.failed("Operation Failed");
        }
    }

}
