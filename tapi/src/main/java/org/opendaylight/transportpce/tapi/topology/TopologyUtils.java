/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public final class TopologyUtils {

    private TopologyUtils() {
    }

    public static ListenableFuture<RpcResult<GetTopologyDetailsOutput>> createGetTopologyDetailsReply(
        GetTopologyDetailsInput input) {
        GetTopologyDetailsOutputBuilder output = new GetTopologyDetailsOutputBuilder().setTopology(null);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

}
