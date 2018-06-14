/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInputBuilder;

public class ServiceImplementationRequestInputConverter {

    public static ServiceImplementationRequestInput getStub(org.opendaylight.yang.gen.v1.http.org.transportpce
                .b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput concrete) {
        ServiceImplementationRequestInputBuilder stubBuilder = new ServiceImplementationRequestInputBuilder();
        stubBuilder.setServiceHandlerHeader(concrete.getServiceHandlerHeader());
        stubBuilder.setPathDescription(PathDescriptionConverter.getStub(concrete.getPathDescription()));
        stubBuilder.setServiceName(concrete.getServiceName());
        stubBuilder.setServiceAEnd(ServiceAEndConverter.getStub(concrete.getServiceAEnd()));
        stubBuilder.setServiceZEnd(ServiceZEndConverter.getStub(concrete.getServiceZEnd()));
        return stubBuilder.build();
    }


    public static org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
        .ServiceImplementationRequestInput getConcrete(ServiceImplementationRequestInput stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
            .ServiceImplementationRequestInputBuilder concreteBuilder = new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInputBuilder();
        concreteBuilder.setServiceHandlerHeader(stub.getServiceHandlerHeader());
        concreteBuilder.setPathDescription(PathDescriptionConverter.getConcrete(stub.getPathDescription()));
        concreteBuilder.setServiceName(stub.getServiceName());
        concreteBuilder.setServiceAEnd(ServiceAEndConverter.getConcrete(stub.getServiceAEnd()));
        concreteBuilder.setServiceZEnd(ServiceZEndConverter.getConcrete(stub.getServiceZEnd()));
        return concreteBuilder.build();
    }

}
