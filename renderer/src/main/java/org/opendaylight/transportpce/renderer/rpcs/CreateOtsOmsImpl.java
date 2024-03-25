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
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOms;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateOtsOmsImpl implements CreateOtsOms {
    private static final Logger LOG = LoggerFactory.getLogger(CreateOtsOmsImpl.class);
    private DeviceRendererService deviceRendererService;

    public CreateOtsOmsImpl(final DeviceRendererService deviceRendererService) {
        this.deviceRendererService = requireNonNull(deviceRendererService);
    }

    @Override
    public ListenableFuture<RpcResult<CreateOtsOmsOutput>> invoke(CreateOtsOmsInput input) {
        LOG.info("Request received to create oms and ots interfaces on {}: {}",
            input.getNodeId(), input.getLogicalConnectionPoint());
        try {
            return RpcResultBuilder.success(deviceRendererService.createOtsOms(input)).buildFuture();
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("failed to send request to create oms and ots interfaces on {}: {}",
                input.getNodeId(), input.getLogicalConnectionPoint(),e);
        }
        return RpcResultBuilder.<CreateOtsOmsOutput>failed()
            .withError(ErrorType.RPC, "to create oms and ots interfaces")
            .buildFuture();
    }

}
