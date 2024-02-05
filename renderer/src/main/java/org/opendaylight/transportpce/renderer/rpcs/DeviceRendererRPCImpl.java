/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOms;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePath;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePath;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class DeviceRendererRPCImpl {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRendererRPCImpl.class);
    private Registration reg;

    @Activate
    public DeviceRendererRPCImpl(@Reference RpcProviderService rpcProviderService,
            @Reference DeviceRendererService deviceRenderer,
            @Reference OtnDeviceRendererService otnDeviceRendererService) {
        this.reg = rpcProviderService.registerRpcImplementations(ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(ServicePath.class, new ServicePathImpl(deviceRenderer))
            .put(OtnServicePath.class, new OtnServicePathImpl(otnDeviceRendererService))
            .put(RendererRollback.class, new RendererRollbackImpl(deviceRenderer))
            .put(CreateOtsOms.class, new CreateOtsOmsImpl(deviceRenderer))
            .build());
        LOG.debug("RPC of DeviceRendererRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("DeviceRendererRPCImpl Closed");
    }

}
