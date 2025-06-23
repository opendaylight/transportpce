/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util.rev210618;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;

public final class OlmUtilsTestObjects {

    private OlmUtilsTestObjects() {

    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput
        newGetPmInput210618(String nodeId,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum type,
                        org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmGranularity gran,
                        org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input
                                .ResourceIdentifier identifier) {
        return new GetPmInputBuilder()
                .setNodeId(nodeId)
                .setResourceType(type)
                .setGranularity(gran)
                .setResourceIdentifier(identifier)
                .build();
    }

    //Constructor without the resourceIdentifier, use for pmFetchAll
    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput
        newGetPmInput210618(String nodeId,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum type,
                        org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmGranularity gran) {
        return new GetPmInputBuilder()
                .setNodeId(nodeId)
                .setResourceType(type)
                .setGranularity(gran)
                .build();
    }
}

