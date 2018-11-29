/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.stub;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
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
    private StubrendererImpl stubrendererImpl;
    private final ListeningExecutorService executor;

    public StubRendererServiceOperations(NetworkModelWavelengthService networkModelWavelengthService,
            DataBroker dataBroker, NotificationPublishService notificationPublishService) {
        this.stubrendererImpl =
                new StubrendererImpl(networkModelWavelengthService, dataBroker, notificationPublishService);
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    }

    @Override
    public ListenableFuture<ServiceImplementationRequestOutput>
            serviceImplementation(ServiceImplementationRequestInput input) {
        return executor.submit(new Callable<ServiceImplementationRequestOutput>() {

            @Override
            public ServiceImplementationRequestOutput call() {
                ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> rpcResultFuture =
                        stubrendererImpl.serviceImplementation(input);
                try {
                    return rpcResultFuture.get().getResult();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("RPC serviceImplementation failed !", e);
                }
                return null;
            }
        });
    }

    @Override
    public ListenableFuture<ServiceDeleteOutput> serviceDelete(ServiceDeleteInput input) {
        return executor.submit(new Callable<ServiceDeleteOutput>() {

            @Override
            public ServiceDeleteOutput call() {
                ListenableFuture<RpcResult<ServiceDeleteOutput>> rpcResultFuture =
                        stubrendererImpl.serviceDelete(input);
                try {
                    return rpcResultFuture.get().getResult();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("RPC serviceDelete failed !", e);
                }
                return null;
            }
        });
    }
}
