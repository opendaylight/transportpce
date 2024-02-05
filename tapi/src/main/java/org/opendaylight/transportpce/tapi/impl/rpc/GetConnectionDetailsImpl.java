/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.get.connection.details.output.ConnectionBuilder;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetConnectionDetailsImpl implements GetConnectionDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetConnectionDetailsImpl.class);

    private final TapiContext tapiContext;

    public GetConnectionDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionDetailsOutput>> invoke(GetConnectionDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid connectionUuid = input.getUuid();
        Connection connection = this.tapiContext.getConnection(connectionUuid);
        if (connection == null) {
            LOG.error("Connection {} doesnt exist in tapi context", input.getUuid());
            return RpcResultBuilder.<GetConnectionDetailsOutput>failed()
                .withError(ErrorType.RPC, "Connection doesnt exist in datastore")
                .buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectionDetailsOutputBuilder().setConnection(
                new ConnectionBuilder(connection).build()).build()).buildFuture();
    }

}
