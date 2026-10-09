/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.executor.DefaultExecutorPoolManager;
import org.opendaylight.transportpce.common.executor.DefaultRpcRejections;
import org.opendaylight.transportpce.common.executor.ExecutorPoolManager;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.common.executor.config.ExecutorPoolConfig;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class DeviceRendererRPCImpl {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRendererRPCImpl.class);
    private final ExecutorPoolManager pools;
    private Registration reg;

    @Activate
    public DeviceRendererRPCImpl(@Reference RpcProviderService rpcProviderService,
            @Reference DeviceRendererService deviceRenderer,
            @Reference OtnDeviceRendererService otnDeviceRendererService,
            @Reference ExecutorPoolConfig poolConfig) {
        this(rpcProviderService, deviceRenderer, otnDeviceRendererService, new DefaultExecutorPoolManager(poolConfig));
    }

    /**
     * Creates the RPC implementations with executors from the given manager.
     *
     * @param pools the manager of the executors; this object takes ownership of it and closes it in {@link #close()}
     */
    public DeviceRendererRPCImpl(RpcProviderService rpcProviderService, DeviceRendererService deviceRenderer,
            OtnDeviceRendererService otnDeviceRendererService, ExecutorPoolManager pools) {
        this.pools = pools;
        ListeningExecutorService serviceExecutor =
                pools.executorService(DeviceRendererRPCImpl.class, ThreadPoolName.SERVICE_PATH);
        RpcRejections rpcRejections = new DefaultRpcRejections();

        this.reg = rpcProviderService.registerRpcImplementations(
                new ServicePathImpl(deviceRenderer, serviceExecutor, rpcRejections),
                new OtnServicePathImpl(otnDeviceRendererService),
                new RendererRollbackImpl(deviceRenderer),
                new CreateOtsOmsImpl(deviceRenderer));
        LOG.debug("RPC of DeviceRendererRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        // Shuts down the executors, which cancels the requests they are still processing
        this.pools.close();
        LOG.info("DeviceRendererRPCImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return reg;
    }
}
