/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.path.PathDescription;


public interface RendererServiceOperations {

    ListenableFuture<ServiceImplementationRequestOutput> serviceImplementation(ServiceImplementationRequestInput input);

    ListenableFuture<ServiceDeleteOutput> serviceDelete(ServiceDeleteInput input, Services service);

    OperationResult reserveResource(PathDescription pathDescription);

    OperationResult freeResource(PathDescription pathDescription);

}
