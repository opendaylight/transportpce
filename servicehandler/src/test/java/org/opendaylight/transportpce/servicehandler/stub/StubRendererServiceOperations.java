/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.stub;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubRendererServiceOperations implements RendererServiceOperations {
    private static final Logger LOG = LoggerFactory.getLogger(StubRendererServiceOperations.class);

    @Override
    public ServiceImplementationRequestOutput serviceImplementation(ServiceImplementationRequestInput input) {
        ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> rpcResultFuture = StubrendererImpl
                .serviceImplementation(input);
        try {
            return rpcResultFuture.get().getResult();
        } catch (InterruptedException e) {
            LOG.error("RPC serviceImplementation failed !",e);
        } catch (ExecutionException e) {
            LOG.error("RPC serviceImplementation failed !",e);
        }
        return null;
    }

    @Override
    public ServiceDeleteOutput serviceDelete(ServiceDeleteInput input) {
        ListenableFuture<RpcResult<ServiceDeleteOutput>> rpcResultFuture = StubrendererImpl.serviceDelete(input);
        try {
            return rpcResultFuture.get().getResult();
        } catch (InterruptedException e) {
            LOG.error("RPC serviceDelete failed !",e);
        } catch (ExecutionException e) {
            LOG.error("RPC serviceDelete failed !",e);
        }
        return null;
    }
}
