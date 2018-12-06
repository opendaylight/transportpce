/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.utils;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;

public final class DataUtils {

    public static CancelResourceReserveInput getCancelResourceReserveInput() {
        CancelResourceReserveInput input = new CancelResourceReserveInputBuilder()
            .setServiceName("service 1")
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId("request 1")
                .build())
            .build();

        return input;
    }

    private DataUtils() {
    }

}
