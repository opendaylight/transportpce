/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePath;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OtnServicePathImpl implements OtnServicePath {
    private static final Logger LOG = LoggerFactory.getLogger(OtnServicePathImpl.class);
    private final OtnDeviceRendererService otnDeviceRendererService;
    private final ListeningExecutorService executorService;
    private final RpcRejections rpcRejections;

    public OtnServicePathImpl(
            final OtnDeviceRendererService otnDeviceRendererService,
            final ListeningExecutorService executorService,
            final RpcRejections rpcRejections) {
        this.otnDeviceRendererService = requireNonNull(otnDeviceRendererService);
        this.executorService = requireNonNull(executorService);
        this.rpcRejections = requireNonNull(rpcRejections);
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

        try {
            // Cancelling the reply must not interrupt the request: the device work runs on further threads and would
            // continue after its pool thread is freed
            return Futures.nonCancellationPropagating(executorService.submit(() -> switch (input.getOperation()) {
                case Create -> {
                    LOG.info("Create operation request received");
                    yield RpcResultBuilder
                            .success(otnDeviceRendererService.setupOtnServicePath(input, serviceType))
                            .build();
                }
                case Delete -> {
                    LOG.info("Delete operation request received");
                    yield RpcResultBuilder
                            .success(otnDeviceRendererService.deleteOtnServicePath(input, serviceType))
                            .build();
                }
            }));
        } catch (RejectedExecutionException e) {
            return rpcRejections.rejected(executorService, ThreadPoolName.OTN_SERVICE_PATH, e);
        }
    }
}
