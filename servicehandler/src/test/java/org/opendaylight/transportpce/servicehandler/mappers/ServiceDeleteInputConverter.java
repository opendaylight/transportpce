/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInputBuilder;

public final class ServiceDeleteInputConverter {

    public static ServiceDeleteInput getStub(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .servicepath.rev170426.ServiceDeleteInput concrete) {
        ServiceDeleteInputBuilder stubBuilder = new ServiceDeleteInputBuilder();
        stubBuilder.setServiceHandlerHeader(concrete.getServiceHandlerHeader());
        stubBuilder.setServiceName(concrete.getServiceName());
        return stubBuilder.build();
    }


    public static org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
        .ServiceDeleteInput getConcrete(ServiceDeleteInput stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
            .ServiceDeleteInputBuilder concreteBuilder = new org.opendaylight.yang.gen.v1.http.org.transportpce
                .b.c._interface.servicepath.rev170426.ServiceDeleteInputBuilder();
        concreteBuilder.setServiceHandlerHeader(stub.getServiceHandlerHeader());
        concreteBuilder.setServiceName(stub.getServiceName());
        return concreteBuilder.build();
    }

    private ServiceDeleteInputConverter() {
    }
}
