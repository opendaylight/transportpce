/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigure;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceReconfigureImpl implements ServiceReconfigure {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceReconfigureImpl.class);
    private static final String SERVICE_RECONFIGURE_MSG = "serviceReconfigure: {}";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PceListener pceListener;
    private RendererListener rendererListener;
    private NetworkListener networkListener;
    private RendererServiceWrapper rendererServiceWrapper;

    public ServiceReconfigureImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            final PceListener pceListener, RendererListener rendererListener, NetworkListener networkListener,
            RendererServiceWrapper rendererServiceWrapper) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceListener = pceListener;
        this.rendererListener = rendererListener;
        this.networkListener = networkListener;
        this.rendererServiceWrapper = rendererServiceWrapper;
    }


    @Override
    public ListenableFuture<RpcResult<ServiceReconfigureOutput>> invoke(ServiceReconfigureInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceReconfigure received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (servicesObject.isEmpty()) {
            LOG.warn(SERVICE_RECONFIGURE_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createCreateServiceReply(
                input,
                LogMessages.serviceNotInDS(serviceName));
        }
        LOG.debug("Service '{}' found in datastore", serviceName);
        OperationResult validationResult = ServiceCreateValidation
                .validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceReconfigure);
        if (!validationResult.isSuccess()) {
            LOG.warn(SERVICE_RECONFIGURE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input,
                    validationResult.getResultMessage());
        }
        this.pceListener.setInput(new ServiceInput(input));
        this.pceListener.setServiceReconfigure(true);
        this.pceListener.setServiceFeasiblity(false);
        this.pceListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListener.setServiceInput(new ServiceInput(input));
        this.networkListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                .ServiceDeleteInput serviceDeleteInput =
                        ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                        ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error(SERVICE_RECONFIGURE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input,
                    LogMessages.RENDERER_DELETE_FAILED);
                    //TODO check if RpcStatus.Successful is really expected here
        }
        LOG.info("RPC serviceReconfigure in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input,
                common.getResponseMessage());
    }

}
