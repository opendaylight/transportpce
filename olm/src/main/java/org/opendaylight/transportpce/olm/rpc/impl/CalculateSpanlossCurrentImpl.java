/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.rpc.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrent;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public class CalculateSpanlossCurrentImpl implements CalculateSpanlossCurrent {
    private final OlmPowerService olmPowerService;

    public CalculateSpanlossCurrentImpl(final OlmPowerService olmPowerService) {
        this.olmPowerService = requireNonNull(olmPowerService);
    }

    @Override
    public ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> invoke(CalculateSpanlossCurrentInput input) {
        return RpcResultBuilder.success(this.olmPowerService.calculateSpanlossCurrent(input)).buildFuture();
    }

}
