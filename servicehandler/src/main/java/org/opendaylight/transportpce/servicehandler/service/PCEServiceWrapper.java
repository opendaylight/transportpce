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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.RoutingConstraintsSp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(PCEServiceWrapper.class);

    private final PathComputationService pathComputationService;

    public PCEServiceWrapper(PathComputationService pathComputationService) {
        this.pathComputationService = pathComputationService;
    }

    public PathComputationRequestOutput performPCE(ServiceCreateInput serviceCreateInput, boolean reserveResource) {
        MappingConstraints mappingConstraints = new MappingConstraints(serviceCreateInput.getHardConstraints(),
                serviceCreateInput.getSoftConstraints());
        mappingConstraints.serviceToServicePathConstarints();
        PathComputationRequestInput pathComputationRequestInput =
                createPceRequestInput(serviceCreateInput, mappingConstraints.getServicePathHardConstraints(),
                mappingConstraints.getServicePathSoftConstraints(), reserveResource);
        LOG.debug("Calling path computation.");
        PathComputationRequestOutput pathComputationRequestOutput
                = this.pathComputationService.pathComputationRequest(pathComputationRequestInput);
        LOG.debug("Path computation done.");
        return pathComputationRequestOutput;
    }

    private PathComputationRequestInput createPceRequestInput(ServiceCreateInput serviceCreateInput,
                                                          HardConstraints hardConstraints,
                                                          SoftConstraints softConstraints,
                                                          Boolean reserveResource) {
        LOG.info("Mapping ServiceCreateInput or ServiceFeasibilityCheckInput or serviceReconfigureInput to PCE"
                + "requests");
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (serviceCreateInput.getSdncRequestHeader() != null) {
            serviceHandlerHeader.setRequestId(serviceCreateInput.getSdncRequestHeader().getRequestId());
        }
        return new PathComputationRequestInputBuilder()
                .setServiceName(serviceCreateInput.getServiceName())
                .setResourceReserve(reserveResource)
                .setServiceHandlerHeader(serviceHandlerHeader.build())
                .setHardConstraints(hardConstraints)
                .setSoftConstraints(softConstraints)
                .setPceMetric(RoutingConstraintsSp.PceMetric.TEMetric)
                .setServiceAEnd(ModelMappingUtils.createServiceAEnd(serviceCreateInput.getServiceAEnd()))
                .setServiceZEnd(ModelMappingUtils.createServiceZEnd(serviceCreateInput.getServiceZEnd()))
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
