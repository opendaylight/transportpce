/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.tapisbi.renderer.TapiSbiRendererOperation;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TapiSbiServiceDeleteImpl implements TapiSbiServiceDelete {
    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiServiceDeleteImpl.class);
    private final TapiSbiRendererOperation tapiSbiRendererOperation;

    public TapiSbiServiceDeleteImpl(TapiSbiRendererOperation sbiRendererOperation) {
        this.tapiSbiRendererOperation = sbiRendererOperation;

    }

    @Override
    public ListenableFuture<RpcResult<TapiSbiServiceDeleteOutput>> invoke(TapiSbiServiceDeleteInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service delete request {}", serviceName);

        // TODO: Implement this RPC
        LOG.debug("TapiSbiServiceDelete invoked leveraging {}", tapiSbiRendererOperation.getClass());

        return RpcResultBuilder.success(new TapiSbiServiceDeleteOutputBuilder().build()).buildFuture();
    }
}
