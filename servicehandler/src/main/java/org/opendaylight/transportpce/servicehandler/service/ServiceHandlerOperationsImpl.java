/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceHandlerOperationsImpl implements ServiceHandlerOperations {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHandlerOperationsImpl.class);

    private ServicehandlerImpl serviceHandler;

    public ServiceHandlerOperationsImpl(DataBroker databroker, PathComputationService pathComputationService,
        RendererServiceOperations rendererServiceOperations, NotificationPublishService notificationPublishService,
        PceListenerImpl pceListenerImpl, RendererListenerImpl rendererListenerImpl,
        NetworkModelListenerImpl networkModelListenerImpl) {
        this.serviceHandler = new ServicehandlerImpl(databroker, pathComputationService, rendererServiceOperations,
            notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl);
    }

    @Override
    public ServiceCreateOutput serviceCreate(ServiceCreateInput input) {
        ListenableFuture<RpcResult<ServiceCreateOutput>> output = this.serviceHandler.serviceCreate(input);
        ServiceCreateOutput outputresult = null;
        try {
            outputresult = output.get().getResult();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Rpc service create failed", e);
        }
        return outputresult;
    }

    @Override
    public ServiceDeleteOutput serviceDelete(ServiceDeleteInput input) {
        return null;
    }

    @Override
    public ServiceList getORServices() {

        return this.serviceHandler.getServices();
    }

    @Override
    public ServicePaths getServicePathDescription(String servicename) {

        return this.serviceHandler.getServicePathDescription(servicename);
    }

    public void init() {
        LOG.info("init ServiceHandlerOperationsImpl...");
    }

    public void close() {
        LOG.info("close ServiceHandlerOperationsImpl...");
    }

}
