/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ServiceEndpoint;

public class GenericServiceEndpoint {
    private ServiceEndpoint value;
    private ServiceEndpointType type;

    public ServiceEndpointType getType() {
        return type;
    }

    public GenericServiceEndpoint(ServiceEndpoint sep, ServiceEndpointType type) {
        this.value = sep;
        this.type = type;
    }

    public ServiceEndpoint getValue() {
        return value;
    }


}
