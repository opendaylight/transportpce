/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.stub;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.mappers.ServiceDeleteInputConverter;
import org.opendaylight.transportpce.servicehandler.mappers.ServiceDeleteOutputConverter;
import org.opendaylight.transportpce.servicehandler.mappers.ServiceImplementationRequestInputConverter;
import org.opendaylight.transportpce.servicehandler.mappers.ServiceImplementationRequestOutputConverter;
import org.opendaylight.transportpce.stubrenderer.impl.StubrendererImpl;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubRendererServiceOperations implements RendererServiceOperations {
    private static final Logger LOG = LoggerFactory.getLogger(StubRendererServiceOperations.class);
    private StubrendererImpl stubrenderer;

    public StubRendererServiceOperations(NotificationPublishService notificationPublishService) {
        this.stubrenderer = new StubrendererImpl(notificationPublishService);
    }

    @Override
    public ServiceImplementationRequestOutput serviceImplementation(ServiceImplementationRequestInput input) {
        Future<RpcResult<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426
            .ServiceImplementationRequestOutput>> rpcResultFuture = this.stubrenderer
                .serviceImplementationRequest(ServiceImplementationRequestInputConverter.getStub(input));
        try {
            return ServiceImplementationRequestOutputConverter.getConcrete(rpcResultFuture.get().getResult());
        } catch (InterruptedException e) {
            LOG.error("RPC serviceImplementation failed !",e);
        } catch (ExecutionException e) {
            LOG.error("RPC serviceImplementation failed !",e);
        }
        return null;
    }

    @Override
    public ServiceDeleteOutput serviceDelete(ServiceDeleteInput input) {
        Future<RpcResult<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426
            .ServiceDeleteOutput>> rpcResultFuture = this.stubrenderer
                .serviceDelete(ServiceDeleteInputConverter.getStub(input));
        try {
            return ServiceDeleteOutputConverter.getConcrete(rpcResultFuture.get().getResult());
        } catch (InterruptedException e) {
            LOG.error("RPC serviceDelete failed !",e);
        } catch (ExecutionException e) {
            LOG.error("RPC serviceDelete failed !",e);
        }
        return null;
    }
}
