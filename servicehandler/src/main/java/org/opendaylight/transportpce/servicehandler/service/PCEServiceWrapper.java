/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.MappingConstraints;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.handler.header.ServiceHandlerHeaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(PCEServiceWrapper.class);

    private final PathComputationService pathComputationService;

    public PCEServiceWrapper(PathComputationService pathComputationService) {
        this.pathComputationService = pathComputationService;
    }

    public PathComputationRequestOutput performPCE(ServiceCreateInput serviceCreateInput, boolean reserveResource) {
        return performPCE(serviceCreateInput.getHardConstraints(), serviceCreateInput.getSoftConstraints(),
                serviceCreateInput.getServiceName(), serviceCreateInput.getSdncRequestHeader(),
                serviceCreateInput.getServiceAEnd(), serviceCreateInput.getServiceZEnd(), reserveResource);
    }

    public PathComputationRequestOutput performPCE(TempServiceCreateInput tempServiceCreateInput,
            boolean reserveResource) {
        return performPCE(tempServiceCreateInput.getHardConstraints(), tempServiceCreateInput.getSoftConstraints(),
                tempServiceCreateInput.getCommonId(), tempServiceCreateInput.getSdncRequestHeader(),
                tempServiceCreateInput.getServiceAEnd(), tempServiceCreateInput.getServiceZEnd(), reserveResource);
    }

    private PathComputationRequestOutput performPCE(org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains
            .rev161014.routing.constraints.HardConstraints hardConstraints, org.opendaylight.yang.gen.v1.http.org
            .openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraints softConstraints,
            String serviceName, SdncRequestHeader sdncRequestHeader, ServiceEndpoint serviceAEnd,
            ServiceEndpoint serviceZEnd, boolean reserveResource) {
        MappingConstraints mappingConstraints = new MappingConstraints(hardConstraints, softConstraints);
        mappingConstraints.serviceToServicePathConstarints();
        PathComputationRequestInput pathComputationRequestInput =
                createPceRequestInput(serviceName, sdncRequestHeader,mappingConstraints.getServicePathHardConstraints(),
                        mappingConstraints.getServicePathSoftConstraints(), reserveResource, serviceAEnd,
                        serviceZEnd);
        LOG.debug("Calling path computation.");
        PathComputationRequestOutput pathComputationRequestOutput
                = this.pathComputationService.pathComputationRequest(pathComputationRequestInput);
        LOG.debug("Path computation done.");
        return pathComputationRequestOutput;
    }

    private PathComputationRequestInput createPceRequestInput(String serviceName,
            SdncRequestHeader serviceHandler, HardConstraints hardConstraints,
            SoftConstraints softConstraints, Boolean reserveResource, ServiceEndpoint serviceAEnd,
            ServiceEndpoint serviceZEnd) {
        LOG.info("Mapping ServiceCreateInput or ServiceFeasibilityCheckInput or serviceReconfigureInput to PCE"
                + "requests");
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (serviceHandler != null) {
            serviceHandlerHeader.setRequestId(serviceHandler.getRequestId());
        }
        return new PathComputationRequestInputBuilder()
            .setServiceName(serviceName)
            .setResourceReserve(reserveResource)
            .setServiceHandlerHeader(serviceHandlerHeader.build())
            .setHardConstraints(hardConstraints)
            .setSoftConstraints(softConstraints)
            .setPceMetric(PceMetric.TEMetric)
            .setServiceAEnd(ModelMappingUtils.createServiceAEnd(serviceAEnd))
            .setServiceZEnd(ModelMappingUtils.createServiceZEnd(serviceZEnd))
            .build();
    }

    private CancelResourceReserveInput mappingCancelResourceReserve(String serviceName,
                                                                    SdncRequestHeader sdncRequestHeader) {
        LOG.debug("Mapping ServiceCreateInput or ServiceFeasibilityCheckInput or serviceReconfigureInput to PCE"
                + "requests");
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (sdncRequestHeader != null) {
            serviceHandlerHeader.setRequestId(sdncRequestHeader.getRequestId());
        }
        CancelResourceReserveInputBuilder cancelResourceReserveInput = new CancelResourceReserveInputBuilder()
                .setServiceName(serviceName)
                .setServiceHandlerHeader(serviceHandlerHeader.build());
        return cancelResourceReserveInput.build();
    }

}
