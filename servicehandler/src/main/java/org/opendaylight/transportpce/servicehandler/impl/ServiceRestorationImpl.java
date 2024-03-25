/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.opendaylight.transportpce.servicehandler.DowngradeConstraints;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestoration;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfo.TailRetention;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceRestorationImpl implements ServiceRestoration {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRestorationImpl.class);
    private static final String SERVICE_RESTORATION_MSG = "serviceRestoration: {}";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PceListener pceListener;
    private RendererListener rendererListener;
    private NetworkListener networkListener;
    private RendererServiceWrapper rendererServiceWrapper;

    public ServiceRestorationImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            final PceListener pceListener, RendererListener rendererListener, NetworkListener networkListener,
            RendererServiceWrapper rendererServiceWrapper) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceListener = pceListener;
        this.rendererListener = rendererListener;
        this.networkListener = networkListener;
        this.rendererServiceWrapper = rendererServiceWrapper;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRestorationOutput>> invoke(ServiceRestorationInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceRestoration received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);

        if (servicesObject.isEmpty()) {
            LOG.warn(SERVICE_RESTORATION_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createRestoreServiceReply(
                    LogMessages.serviceNotInDS(serviceName));
        }

        Services service = servicesObject.orElseThrow();
        State state = service.getOperationalState();

        if (state == State.InService) {
            LOG.error(SERVICE_RESTORATION_MSG, LogMessages.serviceInService(serviceName));
            return ModelMappingUtils.createRestoreServiceReply(
                    LogMessages.serviceInService(serviceName));
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
                .setNotificationUrl(service.getSdncRequestHeader().getNotificationUrl())
                .setRequestId(service.getSdncRequestHeader().getRequestId())
                .setRequestSystemId(service.getSdncRequestHeader().getRequestSystemId())
                .setRpcAction(RpcActions.ServiceDelete);
        ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder()
                .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(serviceName)
                    .setDueDate(datetime)
                    .setTailRetention(TailRetention.No).build())
                .setSdncRequestHeader(sdncBuilder.build());
        ServiceInput serviceInput = new ServiceInput(deleteInputBldr.build());
        serviceInput.setServiceAEnd(service.getServiceAEnd());
        serviceInput.setServiceZEnd(service.getServiceZEnd());
        serviceInput.setConnectionType(service.getConnectionType());
        HardConstraints hardConstraints = service.getHardConstraints();
        if (hardConstraints == null) {
            LOG.warn("service '{}' HardConstraints is not set !", serviceName);
        } else {
            SoftConstraints softConstraints = service.getSoftConstraints();
            if (softConstraints == null) {
                LOG.warn("service '{}' SoftConstraints is not set !", serviceName);
                serviceInput.setSoftConstraints(DowngradeConstraints.convertToSoftConstraints(hardConstraints));
            } else {
                LOG.info("converting hard constraints to soft constraints ...");
                serviceInput.setSoftConstraints(
                        DowngradeConstraints.updateSoftConstraints(hardConstraints, softConstraints));
            }
            serviceInput.setHardConstraints(DowngradeConstraints.downgradeHardConstraints(hardConstraints));
        }
        this.pceListener.setInput(serviceInput);
        this.pceListener.setServiceReconfigure(true);
        this.pceListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListener.setServiceInput(serviceInput);
        this.rendererListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.networkListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(deleteInputBldr.build()));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error(SERVICE_RESTORATION_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createRestoreServiceReply(LogMessages.RENDERER_DELETE_FAILED);
        }
        LOG.info("RPC serviceRestore in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createRestoreServiceReply(common.getResponseMessage());
    }

}
