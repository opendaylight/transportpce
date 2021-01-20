/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;

public interface ServiceHandlerOperations {
    ServiceCreateOutput serviceCreate(ServiceCreateInput input);

    ServiceDeleteOutput serviceDelete(ServiceDeleteInput input);

    ServiceList getORServices();

    ServicePaths getServicePathDescription(String servicename);
}
