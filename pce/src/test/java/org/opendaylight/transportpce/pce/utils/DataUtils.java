/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.utils;

import java.util.Arrays;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.RoutingConstraintsSp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeader;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;

public final class DataUtils {

    public static CancelResourceReserveInput getCancelResourceReserveInput(){
        CancelResourceReserveInput input = new CancelResourceReserveInputBuilder()
            .setServiceName("service 1")
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build())
            .build();

        return input;
    }
}
