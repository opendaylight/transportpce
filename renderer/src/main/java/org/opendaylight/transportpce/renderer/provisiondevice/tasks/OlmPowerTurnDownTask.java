/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.renderer.ServicePathInputData;
import org.opendaylight.transportpce.renderer.provisiondevice.OLMRenderingResult;
import org.opendaylight.transportpce.renderer.provisiondevice.notification.Notification;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndown;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmPowerTurnDownTask implements Callable<OLMRenderingResult> {

    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerTurnDownTask.class);
    private final String serviceName;
    private final String path;
    private final ServicePathInputData servicePathInputData;
    private final Notification notification;
    private final RpcService rpcService;

    /**
     * Task used to power down OLM.
     *
     * <p>Intended to be used for parallel execution.
     */
    public OlmPowerTurnDownTask(String serviceName, String path, ServicePathInputData servicePathInputData,
            Notification notification, RpcService rpcService) {

        this.serviceName = serviceName;
        this.path = path;
        this.servicePathInputData = servicePathInputData;
        this.notification = notification;
        this.rpcService = rpcService;
    }

    @Override
    public OLMRenderingResult call() throws Exception {

        LOG.debug("Turning down power on {} path for service {}", path, serviceName);

        Future<RpcResult<ServicePowerTurndownOutput>> fr = rpcService.getRpc(ServicePowerTurndown.class).invoke(
            new ServicePowerTurndownInputBuilder(
                servicePathInputData.getServicePathInput()
            ).build());

        notification.send(
            ServicePathNotificationTypes.ServiceDelete,
            serviceName,
            RpcStatusEx.Pending,
            String.format("Turning down power on %s path for service %s", path, serviceName)
        );

        RpcResult<ServicePowerTurndownOutput> result = fr.get();

        if (result == null || !ResponseCodes.SUCCESS_RESULT.equals(result.getResult().getResult())) {
            notification.send(
                ServicePathNotificationTypes.ServiceDelete,
                serviceName,
                RpcStatusEx.Failed,
                String.format("Service power turn down failed on %s path for service %s", path, serviceName)
            );
            return OLMRenderingResult.failed(
                String.format("Service power turn down failed on %s path for service %s", path, serviceName)
            );
        } else {
            LOG.debug("OLM power turn down finished successfully on {} for service {}", path, serviceName);
            return OLMRenderingResult.ok();
        }
    }
}
