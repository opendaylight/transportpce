/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutputBuilder;

public class ServiceImplementationRequestOutputConverter {

    public static ServiceImplementationRequestOutput getStub(org.opendaylight.yang.gen.v1.http.org.transportpce
            .b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutput concrete) {
        ServiceImplementationRequestOutputBuilder stubBuilder = new ServiceImplementationRequestOutputBuilder();
        stubBuilder.setConfigurationResponseCommon(concrete.getConfigurationResponseCommon());
        return stubBuilder.build();
    }


    public static org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
        .ServiceImplementationRequestOutput getConcrete(ServiceImplementationRequestOutput stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
            .ServiceImplementationRequestOutputBuilder concreteBuilder = new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutputBuilder();
        concreteBuilder.setConfigurationResponseCommon(stub.getConfigurationResponseCommon());
        return concreteBuilder.build();
    }

}
