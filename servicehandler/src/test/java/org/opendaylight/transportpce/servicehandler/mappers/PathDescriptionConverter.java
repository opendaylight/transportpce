/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.mappers;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.PathDescriptionBuilder;

public final class PathDescriptionConverter {

    public static PathDescription getStub(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .servicepath.rev170426.service.implementation.request.input.PathDescription concrete) {
        PathDescriptionBuilder stubBuilder = new PathDescriptionBuilder();
        stubBuilder.setAToZDirection(concrete.getAToZDirection());
        stubBuilder.setZToADirection(concrete.getZToADirection());
        return stubBuilder.build();
    }


    public static org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service
        .implementation.request.input.PathDescription getConcrete(PathDescription stub) {
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service
            .implementation.request.input.PathDescriptionBuilder concreteBuilder = new org.opendaylight.yang.gen.v1
                .http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation.request.input
                    .PathDescriptionBuilder();
        concreteBuilder.setAToZDirection(stub.getAToZDirection());
        concreteBuilder.setZToADirection(stub.getZToADirection());
        return concreteBuilder.build();
    }

    private PathDescriptionConverter() {
    }
}
