/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.CreateOtsOmsInputBuilder;

public final class CreateOtsOmsDataUtils {

    private CreateOtsOmsDataUtils() {

    }

    public static CreateOtsOmsInput buildCreateOtsOms() {
        CreateOtsOmsInputBuilder builder = new CreateOtsOmsInputBuilder()
            .setLogicalConnectionPoint("logical point")
            .setNodeId("node 1");
        return builder.build();
    }
}
