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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.PathDescription;


public interface RendererServiceOperations {

    ListenableFuture<ServiceImplementationRequestOutput> serviceImplementation(ServiceImplementationRequestInput input);

    ListenableFuture<ServiceDeleteOutput> serviceDelete(ServiceDeleteInput input);

    OperationResult reserveResource(PathDescription pathDescription);

    OperationResult freeResource(PathDescription pathDescription);

}
