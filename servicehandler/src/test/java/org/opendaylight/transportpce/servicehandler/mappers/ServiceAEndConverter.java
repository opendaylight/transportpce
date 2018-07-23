/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceAEndBuilder;

public final class ServiceAEndConverter {

    public static ServiceAEnd getStub(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath
            .rev170426.service.implementation.request.input.ServiceAEnd concrete) {
        ServiceAEndBuilder stubBuilder = new ServiceAEndBuilder();
        stubBuilder.setClli(concrete.getClli());
        stubBuilder.setNodeId(concrete.getNodeId());
        stubBuilder.setRxDirection(concrete.getRxDirection());
        stubBuilder.setServiceFormat(concrete.getServiceFormat());
        stubBuilder.setServiceRate(concrete.getServiceRate());
        stubBuilder.setTxDirection(concrete.getTxDirection());
        return stubBuilder.build();
    }


    public static org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service
        .implementation.request.input.ServiceAEnd getConcrete(ServiceAEnd stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
            .request.input.ServiceAEndBuilder concreteBuilder = new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.servicepath.rev170426.service.implementation.request.input
                    .ServiceAEndBuilder();
        concreteBuilder.setClli(stub.getClli());
        concreteBuilder.setNodeId(stub.getNodeId());
        concreteBuilder.setRxDirection(stub.getRxDirection());
        concreteBuilder.setServiceFormat(stub.getServiceFormat());
        concreteBuilder.setServiceRate(stub.getServiceRate());
        concreteBuilder.setTxDirection(stub.getTxDirection());
        return concreteBuilder.build();
    }

    private ServiceAEndConverter() {
    }
}
