/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutputBuilder;

public class ServiceDeleteOutputConverter {

    public static ServiceDeleteOutput
        getStub(
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutput
            concrete
    ) {
        ServiceDeleteOutputBuilder stubBuilder = new ServiceDeleteOutputBuilder();
        stubBuilder.setConfigurationResponseCommon(concrete.getConfigurationResponseCommon());
        return stubBuilder.build();
    }


    public static
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutput
        getConcrete(ServiceDeleteOutput stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
            .ServiceDeleteOutputBuilder concreteBuilder =
                new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426
                    .ServiceDeleteOutputBuilder();
        concreteBuilder.setConfigurationResponseCommon(stub.getConfigurationResponseCommon());
        return concreteBuilder.build();
    }

}
