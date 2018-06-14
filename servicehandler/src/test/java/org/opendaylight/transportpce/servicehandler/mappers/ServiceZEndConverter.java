/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceZEndBuilder;

public class ServiceZEndConverter {
    public static ServiceZEnd getStub(
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
        .request.input.ServiceZEnd concrete) {
        ServiceZEndBuilder stubBuilder = new ServiceZEndBuilder();
        stubBuilder.setClli(concrete.getClli());
        stubBuilder.setNodeId(concrete.getNodeId());
        stubBuilder.setRxDirection(concrete.getRxDirection());
        stubBuilder.setServiceFormat(concrete.getServiceFormat());
        stubBuilder.setServiceRate(concrete.getServiceRate());
        stubBuilder.setTxDirection(concrete.getTxDirection());
        return stubBuilder.build();
    }


    public static
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
            .request.input.ServiceZEnd getConcrete(ServiceZEnd stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
            .request.input.ServiceZEndBuilder concreteBuilder =
            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service
            .implementation.request.input.ServiceZEndBuilder();
        concreteBuilder.setClli(stub.getClli());
        concreteBuilder.setNodeId(stub.getNodeId());
        concreteBuilder.setRxDirection(stub.getRxDirection());
        concreteBuilder.setServiceFormat(stub.getServiceFormat());
        concreteBuilder.setServiceRate(stub.getServiceRate());
        concreteBuilder.setTxDirection(stub.getTxDirection());
        return concreteBuilder.build();
    }
}
