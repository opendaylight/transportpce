/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.ServicePathInput;

public class ServicePathInputData {

    private ServicePathInput servicePathInput;
    private NodeLists nodeLists;

    public ServicePathInputData(ServicePathInput servicePathInput, NodeLists nodeLists) {
        this.servicePathInput = servicePathInput;
        this.nodeLists = nodeLists;
    }

    public ServicePathInput getServicePathInput() {
        return servicePathInput;
    }

    public NodeLists getNodeLists() {
        return nodeLists;
    }

}
