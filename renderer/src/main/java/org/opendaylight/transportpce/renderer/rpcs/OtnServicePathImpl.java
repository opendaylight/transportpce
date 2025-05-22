/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OtnServicePath;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OtnServicePathOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OtnServicePathImpl implements OtnServicePath {
    private static final Logger LOG = LoggerFactory.getLogger(OtnServicePathImpl.class);
    private OtnDeviceRendererService otnDeviceRendererService;

    public OtnServicePathImpl(final OtnDeviceRendererService otnDeviceRendererService) {
        this.otnDeviceRendererService = requireNonNull(otnDeviceRendererService);
    }

    @Override
    public ListenableFuture<RpcResult<OtnServicePathOutput>> invoke(OtnServicePathInput input) {
        if (input.getOperation() == null || input.getServiceFormat() == null || input.getServiceRate() == null) {
            LOG.debug("A mandatory input argument is null");
            return RpcResultBuilder
                .success(new OtnServicePathOutputBuilder().setResult("Invalid operation").build())
                .buildFuture();
        }
        String serviceType = ServiceTypes.getOtnServiceType(input.getServiceFormat(), input.getServiceRate());
        return switch (input.getOperation()) {
            case Create -> {
                LOG.info("Create operation request received");
                yield RpcResultBuilder.success(this.otnDeviceRendererService
                    .setupOtnServicePath(input, serviceType)).buildFuture();
            }
            case Delete -> {
                LOG.info("Delete operation request received");
                yield RpcResultBuilder.success(this.otnDeviceRendererService
                    .deleteOtnServicePath(input, serviceType)).buildFuture();
            }
        };
    }
}
