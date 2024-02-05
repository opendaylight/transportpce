/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.reroute.request.input.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReroute;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceRerouteImpl implements ServiceReroute {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRerouteImpl.class);

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;

    public ServiceRerouteImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            PCEServiceWrapper pceServiceWrapper) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceServiceWrapper = pceServiceWrapper;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRerouteOutput>> invoke(ServiceRerouteInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceReroute received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (servicesObject.isEmpty()) {
            LOG.warn("serviceReroute: {}", LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(serviceName),
                    ResponseCodes.RESPONSE_FAILED);
        }
        Services service = servicesObject.orElseThrow();
        Optional<ServicePaths> servicePathsObject = this.serviceDataStoreOperations.getServicePath(serviceName);
        if (servicePathsObject.isEmpty()) {
            LOG.warn("serviceReroute: {}", LogMessages.servicePathNotInDS(serviceName));
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.servicePathNotInDS(serviceName),
                    ResponseCodes.RESPONSE_FAILED);
        }
        ServicePaths servicePaths = servicePathsObject.orElseThrow();
        // serviceInput for later use maybe...
        ServiceInput serviceInput = new ServiceInput(input);
        serviceInput.setServiceAEnd(service.getServiceAEnd());
        serviceInput.setServiceZEnd(service.getServiceZEnd());
        serviceInput.setConnectionType(service.getConnectionType());
        serviceInput.setCommonId(service.getCommonId());
        serviceInput.setHardConstraints(service.getHardConstraints());
        serviceInput.setSoftConstraints(service.getSoftConstraints());
        serviceInput.setCustomer(service.getCustomer());
        serviceInput.setCustomerContact(service.getCustomerContact());

        // Get the network xpdr termination points
        Map<AToZKey, AToZ> mapaToz = servicePaths.getPathDescription().getAToZDirection().getAToZ();
        String aendtp = ((TerminationPoint) mapaToz.get(new AToZKey(String.valueOf(mapaToz.size() - 3)))
                .getResource()
                .getResource())
                .getTpId();
        String zendtp = ((TerminationPoint) mapaToz.get(new AToZKey("2"))
                .getResource()
                .getResource())
                .getTpId();

        PathComputationRerouteRequestOutput output = this.pceServiceWrapper.performPCEReroute(
                service.getHardConstraints(), service.getSoftConstraints(), input.getSdncRequestHeader(),
                service.getServiceAEnd(), service.getServiceZEnd(),
                new EndpointsBuilder().setAEndTp(aendtp).setZEndTp(zendtp).build());

        if (output == null) {
            LOG.error("serviceReroute: {}", LogMessages.PCE_FAILED);
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC ServiceReroute is done");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createRerouteServiceReply(input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

}
