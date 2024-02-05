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
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


/**
 * Rollback created interfaces and cross connects specified by input.
 *
 */
public class RendererRollbackImpl implements RendererRollback {
    private DeviceRendererService deviceRendererService;

    public RendererRollbackImpl(final DeviceRendererService deviceRendererService) {
        this.deviceRendererService = requireNonNull(deviceRendererService);
    }

    @Override
    public ListenableFuture<RpcResult<RendererRollbackOutput>> invoke(RendererRollbackInput input) {
        return RpcResultBuilder.success(this.deviceRendererService.rendererRollback(input)).buildFuture();
    }

}
